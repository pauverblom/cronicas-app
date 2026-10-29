package io.github.pauverblom.cronicas.ui.foto

import android.app.Application
import androidx.compose.runtime.Composable
import io.github.pauverblom.cronicas.data.ColoresTema
import io.github.pauverblom.cronicas.ui.plataforma.Archivo
import io.github.pauverblom.cronicas.ui.plataforma.ahora
import io.github.pauverblom.cronicas.ui.plataforma.rememberAbrirArchivo
import io.github.pauverblom.cronicas.web.aBase64
import io.github.pauverblom.cronicas.web.deBase64
import io.github.pauverblom.cronicas.web.jsBorrarLocal
import io.github.pauverblom.cronicas.web.jsGuardarLocal
import io.github.pauverblom.cronicas.web.jsLeerLocal
import io.github.pauverblom.cronicas.web.procesarRetrato
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Retratos en la web: el JPEG se guarda en localStorage con una clave que hace de ruta ([io.github.pauverblom.cronicas
 * .data.Personaje.fotoPath]), y se mantiene en memoria para pintarlo.
 */
object AlmacenRetratos {
    private const val PREFIJO = "cronicas.retrato."
    private val cache = mutableMapOf<String, ByteArray>()

    fun leer(path: String): ByteArray? = cache[path] ?: jsLeerLocal(PREFIJO + path)?.let(::deBase64)?.also { cache[path] = it }

    fun guardar(path: String, bytes: ByteArray) {
        cache[path] = bytes
        jsGuardarLocal(PREFIJO + path, aBase64(bytes))
    }

    fun borrar(path: String) {
        cache.remove(path)
        jsBorrarLocal(PREFIJO + path)
    }

    fun nuevaRuta(pid: Long) = "web-$pid-${ahora()}"
}

@Serializable
private class RetratoJs(val base64: String, val colores: ColoresTema? = null)

private val json = Json { ignoreUnknownKeys = true }

object Retratos {
    @Suppress("UNUSED_PARAMETER")
    fun directorio(context: Application): String = "retratos"

    /** Recorta y reduce la imagen a un retrato de 512 px; devuelve su «ruta» y los colores del tema «Desde retrato». */
    @Suppress("UNUSED_PARAMETER")
    suspend fun guardar(context: Application, pid: Long, uri: Archivo, anterior: String?): Pair<String, ColoresTema?> {
        val r = procesarRetrato(aBase64(uri.bytes))?.let { json.decodeFromString(RetratoJs.serializer(), it) }
            ?: error("No se pudo leer la imagen")
        val path = AlmacenRetratos.nuevaRuta(pid)
        AlmacenRetratos.guardar(path, deBase64(r.base64))
        anterior?.takeIf { it != path }?.let(AlmacenRetratos::borrar)
        return path to r.colores
    }

    fun borrar(path: String) = AlmacenRetratos.borrar(path)
}

/** En la web la cámara y la galería son el mismo selector de imágenes del navegador. */
@Composable
fun rememberSelectorRetrato(onRecortado: (Archivo) -> Unit): (camara: Boolean) -> Unit {
    val abrir = rememberAbrirArchivo(arrayOf("image/*"), onRecortado)
    return { _ -> abrir() }
}
