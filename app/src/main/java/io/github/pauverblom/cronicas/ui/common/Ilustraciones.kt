package io.github.pauverblom.cronicas.ui.common

import android.content.res.AssetManager
import io.github.pauverblom.cronicas.rules.data.ArticuloDef

/**
 * Ilustraciones del reglamento en assets/ilustraciones (generadas con scripts/extract_ilustraciones.py).
 * Devuelve URIs que Coil carga directamente.
 */
object Ilustraciones {
    private const val BASE = "file:///android_asset/ilustraciones/"

    /** Nombre de archivo esperado en assets/ilustraciones para cada cosa. */
    fun archivoClase(id: String) = "clase_$id.webp"
    fun archivoEspecie(id: String) = "especie_$id.webp"
    fun archivoArticulo(a: ArticuloDef) = "objeto_${a.id}.webp"

    /** Archivos presentes en assets/ilustraciones; se rellena al arrancar con [cargar]. */
    @Volatile
    private var disponibles: Set<String> = emptySet()

    fun cargar(assets: AssetManager) {
        disponibles = assets.list("ilustraciones").orEmpty().toSet()
    }

    private fun uri(archivo: String): String? = archivo.takeIf { it in disponibles }?.let { BASE + it }

    /** La ilustración, o null si ningún libro la dibuja (la app muestra «MISSING PICTURE»). */
    fun clase(id: String): String? = uri(archivoClase(id))
    fun especie(id: String): String? = uri(archivoEspecie(id))
    fun articulo(a: ArticuloDef): String? = uri(archivoArticulo(a))
}
