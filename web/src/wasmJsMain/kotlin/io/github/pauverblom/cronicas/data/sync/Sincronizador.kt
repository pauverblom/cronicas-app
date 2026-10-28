package io.github.pauverblom.cronicas.data.sync

import io.github.pauverblom.cronicas.data.Almacen
import io.github.pauverblom.cronicas.data.Backup
import io.github.pauverblom.cronicas.data.PersonajeExportado
import io.github.pauverblom.cronicas.ui.foto.AlmacenRetratos
import io.github.pauverblom.cronicas.ui.plataforma.ahora
import io.github.pauverblom.cronicas.web.Nube
import io.github.pauverblom.cronicas.web.aBase64
import io.github.pauverblom.cronicas.web.deBase64
import io.github.pauverblom.cronicas.web.jsAzar
import io.github.pauverblom.cronicas.web.jsGuardarLocal
import io.github.pauverblom.cronicas.web.jsLeerLocal
import io.github.pauverblom.cronicas.web.sha256Hex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Versión web del Sincronizador: los mismos documentos de Firestore que la app Android (a través de nube.js),
 * así que un personaje se puede sincronizar entre un móvil Android y un navegador. Gana el último cambio.
 */
class Sincronizador(private val almacen: Almacen, val disponible: Boolean, private val scope: CoroutineScope) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serVinculos = MapSerializer(Long.serializer(), String.serializer())

    private val _vinculos = MutableStateFlow(
        jsLeerLocal(CLAVE)?.let { runCatching { json.decodeFromString(serVinculos, it) }.getOrNull() } ?: emptyMap()
    )

    /** Personajes sincronizados: id local → código. */
    val vinculos: StateFlow<Map<Long, String>> = _vinculos.asStateFlow()

    private val mutex = Mutex()

    /** Última ficha (normalizada) que este dispositivo sabe que está en la nube, por personaje. */
    private val ultimaFicha = mutableMapOf<Long, String>()
    /** Hash del retrato local la última vez que estaba al día: si cambia, es que se ha cambiado aquí y hay que subirlo. */
    private val retratoVisto = mutableMapOf<Long, String>()
    /** Hash del retrato que hay en la nube. */
    private val retratoNube = mutableMapOf<Long, String>()
    private val escuchas = mutableMapOf<Long, Int>()

    @Serializable
    private class FichaNube(val formato: Int = 0, val datos: String = "", val retratoHash: String = "")

    @Serializable
    private class RetratoNube(val hash: String = "", val imagen: String? = null)

    /** Arranca la sincronización de los personajes ya vinculados. Se llama una vez, al iniciar la app. */
    @OptIn(FlowPreview::class)
    fun iniciar() {
        if (!disponible) return
        scope.launch {
            // Sin conexión la primera vez no hay sesión: se reintenta hasta conseguirla
            var espera = 5_000L
            while (runCatching { Nube.sesion() }.isFailure) {
                delay(espera)
                espera = (espera * 2).coerceAtMost(5 * 60_000L)
            }
            _vinculos.value.forEach { (pid, codigo) ->
                retratoVisto[pid] = retratoLocal(pid).second
                escuchar(pid, codigo)
            }
            almacen.fichas.debounce(400).collect {
                mutex.withLock { _vinculos.value.forEach { (pid, codigo) -> subir(pid, codigo) } }
            }
        }
    }

    /** Empieza a sincronizar [pid] con un código nuevo. Devuelve el código. */
    suspend fun activar(pid: Long): String {
        _vinculos.value[pid]?.let { return it }
        check(disponible) { "La sincronización no está configurada" }
        Nube.sesion()
        val codigo = Codigos.nuevo(::jsAzar)
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
        val c = Codigos.normalizar(codigo)
        require(c.length == Codigos.LARGO) { "El código tiene ${Codigos.LARGO} letras y números" }
        _vinculos.value.entries.firstOrNull { it.value == c }?.let { return it.key }
        check(disponible) { "La sincronización no está configurada" }
        Nube.sesion()
        val remota = withTimeout(20_000) { Nube.leer(c) }
        requireNotNull(remota) { "No hay ningún personaje con ese código" }
        val nube = json.decodeFromString(FichaNube.serializer(), remota)
        val e = leerFicha(nube) ?: error("Ese personaje es de una versión más nueva de la app")
        val pid = mutex.withLock {
            val pid = almacen.nuevoId()
            almacen.poner(pid, almacen.adoptar(e, pid, fotoPath = null, creado = ahora()))
            ultimaFicha[pid] = Canonica.de(e)
            guardarVinculo(pid, c)
            retratoVisto[pid] = ""
            retratoNube[pid] = nube.retratoHash
            aplicarRetrato(pid, c, nube.retratoHash)
            pid
        }
        escuchar(pid, c)
        return pid
    }

    /** Deja de sincronizar [pid] en este dispositivo. La copia en la nube y en los demás dispositivos sigue. */
    fun desvincular(pid: Long) {
        escuchas.remove(pid)?.let(Nube::dejar)
        ultimaFicha.remove(pid)
        retratoVisto.remove(pid)
        retratoNube.remove(pid)
        _vinculos.value = _vinculos.value - pid
        jsGuardarLocal(CLAVE, json.encodeToString(serVinculos, _vinculos.value))
    }

    // ── Subida ──

    /** Sube la ficha y el retrato de [pid] si han cambiado desde lo último que hay en la nube. Con el mutex cogido. */
    private fun subir(pid: Long, codigo: String) {
        val e = almacen.exportable(pid)
        if (e == null) {
            // Borrado en este dispositivo: deja de sincronizarse (la copia en la nube no se toca)
            desvincular(pid)
            return
        }
        val ficha = Canonica.de(e)
        val (retrato, hash) = retratoLocal(pid)
        val retratoCambiado = hash != retratoVisto[pid]
        if (ficha == ultimaFicha[pid] && !retratoCambiado) return

        // El retrato va antes que la ficha, para que quien reciba la ficha ya pueda descargarlo
        if (retratoCambiado) {
            Nube.escribirRetrato(codigo, hash, retrato?.let(::aBase64))
            retratoVisto[pid] = hash
            retratoNube[pid] = hash
        }
        Nube.escribir(
            codigo,
            Backup.FORMATO,
            Backup.json.encodeToString(PersonajeExportado.serializer(), e),
            // Si hay un retrato remoto que aún no se ha podido descargar, se respeta el de la nube
            retratoNube[pid] ?: hash
        )
        ultimaFicha[pid] = ficha
    }

    // ── Bajada ──

    private fun escuchar(pid: Long, codigo: String) {
        escuchas.remove(pid)?.let(Nube::dejar)
        escuchas[pid] = Nube.escuchar(codigo) { texto ->
            scope.launch { mutex.withLock { aplicar(pid, codigo, texto) } }
        }
    }

    /** Aplica la ficha remota sobre la local si es distinta. Con el mutex cogido. */
    private suspend fun aplicar(pid: Long, codigo: String, texto: String) {
        if (_vinculos.value[pid] != codigo) return
        val nube = runCatching { json.decodeFromString(FichaNube.serializer(), texto) }.getOrNull() ?: return
        val e = leerFicha(nube) ?: return
        val ficha = Canonica.de(e)
        if (ficha != ultimaFicha[pid]) {
            val actual = almacen.ficha(pid) ?: return
            // Antes de escribir, para que el cambio local que provoca no se vuelva a subir
            ultimaFicha[pid] = ficha
            almacen.poner(pid, almacen.adoptar(e, pid, actual.personaje.fotoPath, actual.personaje.creado))
        }
        retratoNube[pid] = nube.retratoHash
        if (nube.retratoHash != retratoLocal(pid).second) aplicarRetrato(pid, codigo, nube.retratoHash)
    }

    /**
     * Pone como retrato local el de la nube ([hash]). Si aún no se puede descargar, no hace nada:
     * se reintenta con el siguiente cambio remoto, y mientras tanto el retrato local no se sube.
     */
    private suspend fun aplicarRetrato(pid: Long, codigo: String, hash: String) {
        val anterior = almacen.ficha(pid)?.personaje?.fotoPath
        if (hash.isEmpty()) {
            almacen.editar(pid) { it.copy(personaje = it.personaje.copy(fotoPath = null)) }
            anterior?.let(AlmacenRetratos::borrar)
            retratoVisto[pid] = hash
            return
        }
        val texto = runCatching { withTimeoutOrNull(20_000) { Nube.leerRetrato(codigo) } }.getOrNull() ?: return
        val r = runCatching { json.decodeFromString(RetratoNube.serializer(), texto) }.getOrNull() ?: return
        val bytes = r.imagen?.let { runCatching { deBase64(it) }.getOrNull() } ?: return
        if (sha256Hex(bytes) != hash) return
        val path = AlmacenRetratos.nuevaRuta(pid)
        AlmacenRetratos.guardar(path, bytes)
        retratoVisto[pid] = hash
        almacen.editar(pid) { it.copy(personaje = it.personaje.copy(fotoPath = path)) }
        anterior?.takeIf { it != path }?.let(AlmacenRetratos::borrar)
    }

    // ── Utilidades ──

    private fun leerFicha(nube: FichaNube): PersonajeExportado? {
        if (nube.formato == 0 || nube.formato > Backup.FORMATO || nube.datos.isEmpty()) return null
        return runCatching { Backup.json.decodeFromString(PersonajeExportado.serializer(), nube.datos) }
            .onFailure { println("Ficha remota ilegible: $it") }
            .getOrNull()
    }

    /** Bytes y hash del retrato local de [pid]; hash vacío si no tiene. */
    private fun retratoLocal(pid: Long): Pair<ByteArray?, String> {
        val bytes = almacen.ficha(pid)?.personaje?.fotoPath?.let(AlmacenRetratos::leer)
        return bytes to (bytes?.let(::sha256Hex) ?: "")
    }

    private fun guardarVinculo(pid: Long, codigo: String) {
        _vinculos.value = _vinculos.value + (pid to codigo)
        jsGuardarLocal(CLAVE, json.encodeToString(serVinculos, _vinculos.value))
    }

    private companion object {
        const val CLAVE = "cronicas.vinculos"
    }
}
