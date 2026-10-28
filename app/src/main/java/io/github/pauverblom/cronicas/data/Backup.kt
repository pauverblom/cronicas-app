package io.github.pauverblom.cronicas.data

import android.util.Base64
import androidx.room.withTransaction
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Exporta e importa un personaje completo (con su retrato) como un único JSON.
 * Los conjuros (también los de los pergaminos) se exportan enteros y al importar se enlazan por nombre con el catálogo.
 * La sincronización usa la misma instantánea, sin el retrato, que viaja aparte.
 */
object Backup {

    const val FORMATO = FORMATO_FICHA

    internal val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** La ficha entera de [pid]; con [conRetrato], también la foto en base64. */
    suspend fun instantanea(repo: Repository, pid: Long, conRetrato: Boolean = true): PersonajeExportado {
        val db = repo.database
        val p = db.personajeDao().getById(pid) ?: error("Personaje $pid no existe")
        val conjuros = db.conjuroDao().getDePersonaje(pid)
        val porId = db.conjuroDao().getAllOnce().associateBy { it.id }
        val objetos = db.objetoDao().getAll(pid)
        val retrato = if (!conRetrato) null else p.fotoPath?.let { File(it) }?.takeIf { it.exists() }
            ?.let { Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }
        return PersonajeExportado(
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
    }

    suspend fun exportar(repo: Repository, pid: Long): String =
        json.encodeToString(PersonajeExportado.serializer(), instantanea(repo, pid))

    /** Importa siempre como personaje nuevo. Devuelve su id. */
    suspend fun importar(repo: Repository, texto: String, dirRetratos: File): Long =
        escribir(repo.database, json.decodeFromString(PersonajeExportado.serializer(), texto), sobre = null, dirRetratos)

    /**
     * Escribe la ficha [e]. Sin [sobre], como personaje nuevo; con [sobre], sustituye entera la ficha de ese
     * personaje conservando su id, su fecha de creación y su retrato (el retrato de la sincronización va aparte).
     * Los objetos se insertan en el orden de [e], para que la ficha resultante se exporte en el mismo orden.
     */
    suspend fun escribir(db: AppDatabase, e: PersonajeExportado, sobre: Long?, dirRetratos: File): Long = db.withTransaction {
        val pid = if (sobre == null) {
            db.personajeDao().insert(e.personaje.copy(id = 0, fotoPath = null, creado = System.currentTimeMillis()))
        } else {
            val previo = db.personajeDao().getById(sobre)
            // Borrar el personaje borra en cascada todas sus filas; se vuelve a crear con el mismo id
            db.personajeDao().delete(sobre)
            db.personajeDao().insert(
                e.personaje.copy(id = sobre, fotoPath = previo?.fotoPath, creado = previo?.creado ?: e.personaje.creado)
            )
        }
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

        // Monturas primero, para reenlazar la carga; luego los objetos en su orden y por último su contenedor
        val companiaIds = e.companias.associate { it.id to db.companiaDao().insert(it.copy(id = 0, personajeId = pid)) }
        val objetoIds = e.objetos.associate { o ->
            o.id to db.objetoDao().insert(
                o.copy(
                    id = 0,
                    personajeId = pid,
                    contenedorId = null,
                    companiaId = o.companiaId?.let(companiaIds::get),
                    conjuroId = o.conjuroId?.let(pergaminos::get)
                )
            )
        }
        for (o in e.objetos) {
            val contenedor = o.contenedorId?.let(objetoIds::get) ?: continue
            val id = objetoIds.getValue(o.id)
            db.objetoDao().update(
                o.copy(
                    id = id,
                    personajeId = pid,
                    contenedorId = contenedor,
                    companiaId = o.companiaId?.let(companiaIds::get),
                    conjuroId = o.conjuroId?.let(pergaminos::get)
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
