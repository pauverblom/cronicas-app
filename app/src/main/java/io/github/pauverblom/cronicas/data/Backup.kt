package io.github.pauverblom.cronicas.data

import android.util.Base64
import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Exporta e importa un personaje completo (con su retrato) como un único JSON.
 * Los conjuros (también los de los pergaminos) se exportan enteros y al importar se enlazan por nombre con el catálogo.
 */
object Backup {

    const val FORMATO = 1

    @Serializable
    data class ConjuroExportado(val conjuro: Conjuro, val nivel: Int, val preparadoVeces: Int, val usadoVeces: Int)

    @Serializable
    data class PersonajeExportado(
        val formato: Int = FORMATO,
        val personaje: Personaje,
        val caracteristicas: Caracteristicas?,
        val estado: Estado?,
        val tesoro: Tesoro?,
        val objetos: List<Objeto>,
        val habilidades: List<Habilidad>,
        val condiciones: List<Condicion>,
        val companias: List<Compania>,
        val conjuros: List<ConjuroExportado>,
        val espacios: List<EspacioOverride>,
        val notas: List<Nota>,
        val retratoBase64: String? = null,
        /** Conjuros guardados en pergaminos, con el id que referencia [Objeto.conjuroId]. */
        val conjurosPergaminos: List<Conjuro> = emptyList()
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun exportar(repo: Repository, pid: Long): String {
        val db = repo.database
        val p = db.personajeDao().getById(pid) ?: error("Personaje $pid no existe")
        val conjuros = db.conjuroDao().getDePersonaje(pid)
        val porId = db.conjuroDao().getAllOnce().associateBy { it.id }
        val objetos = db.objetoDao().getAll(pid)
        val retrato = p.fotoPath?.let { File(it) }?.takeIf { it.exists() }
            ?.let { Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }
        val export = PersonajeExportado(
            personaje = p.copy(fotoPath = null),
            caracteristicas = db.caracteristicasDao().get(pid),
            estado = db.estadoDao().get(pid),
            tesoro = db.tesoroDao().get(pid),
            objetos = objetos,
            habilidades = db.habilidadDao().getAll(pid),
            condiciones = db.condicionDao().getAll(pid),
            companias = db.companiaDao().getAll(pid),
            conjuros = conjuros.mapNotNull { cp ->
                porId[cp.conjuroId]?.let { ConjuroExportado(it, cp.nivel, cp.preparadoVeces, cp.usadoVeces) }
            },
            espacios = db.conjuroDao().getEspacios(pid),
            notas = db.notaDao().getAll(pid),
            retratoBase64 = retrato,
            conjurosPergaminos = objetos.mapNotNull { it.conjuroId }.distinct().mapNotNull(porId::get)
        )
        return json.encodeToString(PersonajeExportado.serializer(), export)
    }

    /** Importa siempre como personaje nuevo. Devuelve su id. */
    suspend fun importar(repo: Repository, texto: String, dirRetratos: File): Long {
        val e = json.decodeFromString(PersonajeExportado.serializer(), texto)
        val db = repo.database
        return db.withTransaction {
            val pid = db.personajeDao().insert(e.personaje.copy(id = 0, creado = System.currentTimeMillis()))
            e.retratoBase64?.let { b64 ->
                dirRetratos.mkdirs()
                val f = File(dirRetratos, "retrato_${pid}_${System.currentTimeMillis()}.jpg")
                f.writeBytes(Base64.decode(b64, Base64.DEFAULT))
                db.personajeDao().update(db.personajeDao().getById(pid)!!.copy(fotoPath = f.absolutePath))
            }
            db.caracteristicasDao().upsert((e.caracteristicas ?: Caracteristicas(0)).copy(personajeId = pid))
            db.estadoDao().upsert((e.estado ?: Estado(0)).copy(personajeId = pid))
            db.tesoroDao().upsert((e.tesoro ?: Tesoro(0)).copy(personajeId = pid))

            suspend fun idConjuro(c: Conjuro): Long = db.conjuroDao().byName(c.nombre)?.id ?: db.conjuroDao().insert(c.copy(id = 0, esPropio = true))
            val pergaminos = e.conjurosPergaminos.associate { it.id to idConjuro(it) }
            fun Objeto.importado(): Objeto = copy(id = 0, personajeId = pid, conjuroId = conjuroId?.let(pergaminos::get))

            // Monturas primero, para reenlazar la carga; luego contenedores y su contenido
            val companiaIds = e.companias.associate { it.id to db.companiaDao().insert(it.copy(id = 0, personajeId = pid)) }
            val objetoIds = mutableMapOf<Long, Long>()
            val (contenidos, resto) = e.objetos.partition { it.contenedorId != null }
            for (o in resto) {
                objetoIds[o.id] = db.objetoDao().insert(o.importado().copy(companiaId = o.companiaId?.let(companiaIds::get)))
            }
            for (o in contenidos) {
                db.objetoDao().insert(
                    o.importado().copy(
                        contenedorId = o.contenedorId?.let(objetoIds::get),
                        companiaId = o.companiaId?.let(companiaIds::get)
                    )
                )
            }
            e.habilidades.forEach { db.habilidadDao().insert(it.copy(id = 0, personajeId = pid)) }
            e.condiciones.forEach { db.condicionDao().insert(it.copy(id = 0, personajeId = pid)) }
            e.notas.forEach { db.notaDao().insert(it.copy(id = 0, personajeId = pid)) }
            e.espacios.forEach { db.conjuroDao().upsertEspacio(it.copy(personajeId = pid)) }
            for (c in e.conjuros) {
                val cid = idConjuro(c.conjuro)
                db.conjuroDao().insertDePersonaje(ConjuroPersonaje(pid, cid, c.nivel, c.preparadoVeces, c.usadoVeces))
            }
            pid
        }
    }
}
