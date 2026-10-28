package io.github.pauverblom.cronicas.ui.common

import io.github.pauverblom.cronicas.rules.data.ArticuloDef
import io.github.pauverblom.cronicas.web.jsBaseUrl

/** Ilustraciones del reglamento, servidas junto a la página en ilustraciones/ (la lista se genera al compilar). */
object Ilustraciones {
    private val base by lazy { jsBaseUrl() + "ilustraciones/" }

    fun archivoClase(id: String) = "clase_$id.webp"
    fun archivoEspecie(id: String) = "especie_$id.webp"
    fun archivoArticulo(a: ArticuloDef) = "objeto_${a.id}.webp"

    private fun uri(archivo: String): String? = archivo.takeIf { it in ILUSTRACIONES_DISPONIBLES }?.let { base + it }

    fun clase(id: String): String? = uri(archivoClase(id))
    fun especie(id: String): String? = uri(archivoEspecie(id))
    fun articulo(a: ArticuloDef): String? = uri(archivoArticulo(a))
}
