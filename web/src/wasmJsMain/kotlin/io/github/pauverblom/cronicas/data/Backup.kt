package io.github.pauverblom.cronicas.data

import io.github.pauverblom.cronicas.ui.foto.AlmacenRetratos
import io.github.pauverblom.cronicas.web.aBase64
import io.github.pauverblom.cronicas.web.deBase64
import kotlinx.serialization.json.Json

/** Versión web de Backup: el mismo JSON que la app de Android, así que los archivos sirven en las dos. */
object Backup {

    const val FORMATO = FORMATO_FICHA

    internal val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun exportar(repo: Repository, pid: Long): String {
        val almacen = repo.almacen
        val e = almacen.exportable(pid) ?: error("Personaje $pid no existe")
        val retrato = almacen.ficha(pid)?.personaje?.fotoPath?.let(AlmacenRetratos::leer)?.let(::aBase64)
        return json.encodeToString(PersonajeExportado.serializer(), e.copy(retratoBase64 = retrato))
    }

    /** Importa siempre como personaje nuevo. Devuelve su id. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun importar(repo: Repository, texto: String, dirRetratos: String): Long {
        val e = json.decodeFromString(PersonajeExportado.serializer(), texto)
        val almacen = repo.almacen
        val pid = almacen.nuevoId()
        val foto = e.retratoBase64?.let { b64 ->
            AlmacenRetratos.nuevaRuta(pid).also { AlmacenRetratos.guardar(it, deBase64(b64)) }
        }
        almacen.poner(pid, almacen.adoptar(e, pid, foto, io.github.pauverblom.cronicas.ui.plataforma.ahora()))
        return pid
    }
}
