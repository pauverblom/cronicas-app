package io.github.pauverblom.cronicas.data.sync

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.room.InvalidationTracker
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import io.github.pauverblom.cronicas.data.Backup
import io.github.pauverblom.cronicas.data.PersonajeExportado
import io.github.pauverblom.cronicas.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Sincroniza fichas entre dispositivos con Firestore. Cada personaje sincronizado es un documento
 * `personajes/{codigo}` con su ficha ([PersonajeExportado] sin retrato) en «datos"; el retrato va en
 * `personajes/{codigo}/retrato/actual` y solo se sube cuando cambia. Gana el último cambio.
 *
 * Sin `google-services.json` en la compilación, Firebase no se inicia y [disponible] es false: la app funciona igual
 * y la opción de sincronizar no aparece.
 */
class Sincronizador(context: Context, private val repo: Repository, private val dirRetratos: File) {

    private val app = context.applicationContext

    val disponible: Boolean = FirebaseApp.getApps(app).isNotEmpty()

    private val prefs = app.getSharedPreferences("sincronizacion", Context.MODE_PRIVATE)

    private val _vinculos = MutableStateFlow(leerVinculos())

    /** Personajes sincronizados: id local → código. */
    val vinculos: StateFlow<Map<Long, String>> = _vinculos.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    /** Última ficha (normalizada) que este dispositivo sabe que está en la nube, por personaje. */
    private val ultimaFicha = ConcurrentHashMap<Long, String>()
    /** Hash del retrato local la última vez que estaba al día: si cambia, es que se ha cambiado aquí y hay que subirlo. */
    private val retratoVisto = ConcurrentHashMap<Long, String>()
    /** Hash del retrato que hay en la nube. */
    private val retratoNube = ConcurrentHashMap<Long, String>()
    private val escuchas = ConcurrentHashMap<Long, ListenerRegistration>()

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    /** Arranca la sincronización de los personajes ya vinculados. Se llama una vez, al iniciar la app. */
    @OptIn(FlowPreview::class)
    fun iniciar() {
        if (!disponible) return
        scope.launch {
            // Sin conexión la primera vez no hay sesión: se reintenta hasta conseguirla
            var espera = 5_000L
            while (runCatching { asegurarSesion() }.isFailure) {
                delay(espera)
                espera = (espera * 2).coerceAtMost(5 * 60_000L)
            }
            _vinculos.value.forEach { (pid, codigo) ->
                retratoVisto[pid] = retratoLocal(pid).second
                escuchar(pid, codigo)
            }
            cambiosLocales().debounce(400).collect {
                mutex.withLock { _vinculos.value.forEach { (pid, codigo) -> subir(pid, codigo) } }
            }
        }
    }

    /** Empieza a sincronizar [pid] con un código nuevo. Devuelve el código. */
    suspend fun activar(pid: Long): String {
        _vinculos.value[pid]?.let { return it }
        asegurarSesion()
        val codigo = nuevoCodigo()
        mutex.withLock {
            guardarVinculo(pid, codigo)
            ultimaFicha.remove(pid)
            retratoVisto.remove(pid)
            retratoNube.remove(pid)
            subir(pid, codigo)
        }
        escuchar(pid, codigo)
        return codigo
    }

    /** Descarga el personaje de [codigo] como personaje nuevo y lo deja sincronizado. Devuelve su id. */
    suspend fun vincular(codigo: String): Long {
        val c = normalizar(codigo)
        require(c.length == Codigos.LARGO) { "El código tiene ${Codigos.LARGO} letras y números" }
        _vinculos.value.entries.firstOrNull { it.value == c }?.let { return it.key }
        asegurarSesion()
        val snap = withTimeout(20_000) { doc(c).get(Source.SERVER).await() }
        require(snap.exists()) { "No hay ningún personaje con ese código" }
        val e = leerFicha(snap) ?: error("Ese personaje es de una versión más nueva de la app")
        val pid = mutex.withLock {
            val pid = Backup.escribir(repo.database, e, sobre = null, dirRetratos)
            ultimaFicha[pid] = Canonica.de(e)
            guardarVinculo(pid, c)
            retratoVisto[pid] = ""
            val hash = snap.getString(CAMPO_RETRATO).orEmpty()
            retratoNube[pid] = hash
            aplicarRetrato(pid, c, hash)
            pid
        }
        escuchar(pid, c)
        return pid
    }

    /** Deja de sincronizar [pid] en este dispositivo. La copia en la nube y en los demás dispositivos sigue. */
    fun desvincular(pid: Long) {
        escuchas.remove(pid)?.remove()
        ultimaFicha.remove(pid)
        retratoVisto.remove(pid)
        retratoNube.remove(pid)
        prefs.edit().remove(clave(pid)).apply()
        _vinculos.value = _vinculos.value - pid
    }

    // ── Subida ──

    /** Sube la ficha y el retrato de [pid] si han cambiado desde lo último que hay en la nube. Con el mutex cogido. */
    private suspend fun subir(pid: Long, codigo: String) {
        if (repo.getPersonaje(pid) == null) {
            // Borrado en este dispositivo: deja de sincronizarse (la copia en la nube no se toca)
            desvincular(pid)
            return
        }
        val e = Backup.instantanea(repo, pid, conRetrato = false)
        val ficha = Canonica.de(e)
        val (retrato, hash) = retratoLocal(pid)
        val retratoCambiado = hash != retratoVisto[pid]
        if (ficha == ultimaFicha[pid] && !retratoCambiado) return

        val ref = doc(codigo)
        // Las escrituras no se esperan: sin conexión, Firestore las guarda y las envía al volver la red.
        // El retrato va antes que la ficha, para que quien reciba la ficha ya pueda descargarlo.
        if (retratoCambiado) {
            ref.collection(COLECCION_RETRATO).document(DOC_RETRATO)
                .set(mapOf(CAMPO_HASH to hash, CAMPO_IMAGEN to retrato?.let { Base64.encodeToString(it, Base64.NO_WRAP) }))
                .addOnFailureListener { Log.w(TAG, "No se pudo subir el retrato de $pid", it) }
            retratoVisto[pid] = hash
            retratoNube[pid] = hash
        }
        ref.set(
            mapOf(
                CAMPO_FORMATO to Backup.FORMATO,
                CAMPO_DATOS to Backup.json.encodeToString(PersonajeExportado.serializer(), e),
                // Si hay un retrato remoto que aún no se ha podido descargar, se respeta el de la nube
                CAMPO_RETRATO to (retratoNube[pid] ?: hash),
                CAMPO_ACTUALIZADO to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener { Log.w(TAG, "No se pudo subir la ficha de $pid", it) }
        ultimaFicha[pid] = ficha
    }

    // ── Bajada ──

    private fun escuchar(pid: Long, codigo: String) {
        escuchas.remove(pid)?.remove()
        escuchas[pid] = doc(codigo).addSnapshotListener(MetadataChanges.EXCLUDE) { snap, error ->
            if (error != null) {
                Log.w(TAG, "Error escuchando $codigo", error)
                return@addSnapshotListener
            }
            // Los cambios propios aún sin confirmar no se aplican: ya están en la base de datos local
            if (snap == null || !snap.exists() || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            scope.launch { mutex.withLock { aplicar(pid, codigo, snap) } }
        }
    }

    /** Aplica la ficha remota sobre la local si es distinta. Con el mutex cogido. */
    private suspend fun aplicar(pid: Long, codigo: String, snap: DocumentSnapshot) {
        if (_vinculos.value[pid] != codigo) return
        val e = leerFicha(snap) ?: return
        val ficha = Canonica.de(e)
        if (ficha != ultimaFicha[pid]) {
            // Antes de escribir, para que el cambio local que provoca no se vuelva a subir
            ultimaFicha[pid] = ficha
            Backup.escribir(repo.database, e, sobre = pid, dirRetratos)
        }
        val hash = snap.getString(CAMPO_RETRATO).orEmpty()
        retratoNube[pid] = hash
        if (hash != retratoLocal(pid).second) aplicarRetrato(pid, codigo, hash)
    }

    /**
     * Pone como retrato local el de la nube ([hash]). Si aún no se puede descargar, no hace nada:
     * se reintenta con el siguiente cambio remoto, y mientras tanto el retrato local no se sube.
     */
    private suspend fun aplicarRetrato(pid: Long, codigo: String, hash: String) {
        val p = repo.database.personajeDao().getById(pid) ?: return
        if (hash.isEmpty()) {
            repo.database.personajeDao().update(p.copy(fotoPath = null))
            p.fotoPath?.let { File(it).delete() }
            retratoVisto[pid] = hash
            return
        }
        val snap = runCatching {
            withTimeout(20_000) { doc(codigo).collection(COLECCION_RETRATO).document(DOC_RETRATO).get().await() }
        }.getOrNull() ?: return
        val bytes = snap.getString(CAMPO_IMAGEN)?.let { Base64.decode(it, Base64.DEFAULT) } ?: return
        if (sha256(bytes) != hash) return
        val f = File(dirRetratos.apply { mkdirs() }, "retrato_${pid}_${System.currentTimeMillis()}.jpg")
        f.writeBytes(bytes)
        retratoVisto[pid] = hash
        repo.database.personajeDao().update(p.copy(fotoPath = f.absolutePath))
        p.fotoPath?.let { File(it).takeIf { v -> v.absolutePath != f.absolutePath }?.delete() }
    }

    // ── Utilidades ──

    private suspend fun asegurarSesion() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    private fun doc(codigo: String): DocumentReference = firestore.collection(COLECCION).document(codigo)

    private fun leerFicha(snap: DocumentSnapshot): PersonajeExportado? {
        val formato = snap.getLong(CAMPO_FORMATO)?.toInt() ?: return null
        if (formato > Backup.FORMATO) return null
        val datos = snap.getString(CAMPO_DATOS) ?: return null
        return runCatching { Backup.json.decodeFromString(PersonajeExportado.serializer(), datos) }
            .onFailure { Log.w(TAG, "Ficha remota ilegible", it) }
            .getOrNull()
    }

    /** Bytes y hash del retrato local de [pid]; hash vacío si no tiene. */
    private suspend fun retratoLocal(pid: Long): Pair<ByteArray?, String> {
        val bytes = repo.getPersonaje(pid)?.fotoPath?.let(::File)?.takeIf { it.exists() }?.readBytes()
        return bytes to (bytes?.let(::sha256) ?: "")
    }

    /** Avisa (con un Unit) de cada cambio en las tablas de las fichas. */
    private fun cambiosLocales() = callbackFlow {
        val observador = object : InvalidationTracker.Observer(TABLAS) {
            override fun onInvalidated(tables: Set<String>) {
                trySend(Unit)
            }
        }
        repo.database.invalidationTracker.addObserver(observador)
        awaitClose { repo.database.invalidationTracker.removeObserver(observador) }
    }

    private fun leerVinculos(): Map<Long, String> = prefs.all.mapNotNull { (k, v) ->
        val pid = k.removePrefix(PREFIJO).toLongOrNull() ?: return@mapNotNull null
        (v as? String)?.let { pid to it }
    }.toMap()

    private fun guardarVinculo(pid: Long, codigo: String) {
        prefs.edit().putString(clave(pid), codigo).apply()
        _vinculos.value = _vinculos.value + (pid to codigo)
    }

    private fun clave(pid: Long) = "$PREFIJO$pid"

    companion object {
        private const val TAG = "Sincronizador"
        private const val PREFIJO = "pj_"

        const val COLECCION = "personajes"
        private const val COLECCION_RETRATO = "retrato"
        private const val DOC_RETRATO = "actual"
        private const val CAMPO_FORMATO = "formato"
        private const val CAMPO_DATOS = "datos"
        private const val CAMPO_RETRATO = "retratoHash"
        private const val CAMPO_ACTUALIZADO = "actualizado"
        private const val CAMPO_HASH = "hash"
        private const val CAMPO_IMAGEN = "imagen"

        private val TABLAS = arrayOf(
            "personaje", "caracteristicas", "estado", "objeto", "tesoro", "habilidad",
            "condicion", "compania", "conjuro_personaje", "espacio_override", "nota"
        )

        private fun nuevoCodigo(): String {
            val r = SecureRandom()
            return Codigos.nuevo { r.nextInt(it) }
        }

        fun normalizar(codigo: String): String = Codigos.normalizar(codigo)

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
