package io.github.pauverblom.cronicas.ui.common

import android.content.res.AssetManager
import io.github.pauverblom.cronicas.rules.data.ArticuloDef
import io.github.pauverblom.cronicas.rules.data.Categoria
import io.github.pauverblom.cronicas.rules.data.TipoArma

/**
 * Ilustraciones del reglamento en assets/ilustraciones (generadas con scripts/extract_ilustraciones.py).
 * Devuelve URIs que Coil carga directamente.
 */
object Ilustraciones {
    private const val BASE = "file:///android_asset/ilustraciones/"

    /** La ilustración de la clase, o null si los libros no la dibujan. */
    fun clase(id: String): String? = "clase_$id.webp".takeIf { it in disponibles }?.let { BASE + it }
    fun especie(id: String): String = "${BASE}especie_$id.webp"

    /** Ids de catálogo con dibujo propio (objeto_<id>.webp); se rellena al arrancar con [cargar]. */
    @Volatile
    private var conDibujo: Set<String> = emptySet()

    @Volatile
    private var disponibles: Set<String> = emptySet()

    fun cargar(assets: AssetManager) {
        disponibles = assets.list("ilustraciones").orEmpty().toSet()
        conDibujo = disponibles
            .filter { it.startsWith("objeto_") }
            .map { it.removePrefix("objeto_").removeSuffix(".webp") }
            .toSet()
    }

    private val armasDeAsta = setOf(
        "alabarda", "lanza", "lanza_caballeria", "pica", "tridente", "jabalina", "guadana", "latigo", "baston"
    )

    /** El dibujo propio del artículo si los libros lo tienen; si no, la lámina de su categoría. */
    fun articulo(a: ArticuloDef): String {
        if (a.id in conDibujo) return "${BASE}objeto_${a.id}.webp"
        val lamina = when (a.categoria) {
            Categoria.ARMA -> when {
                a.tipo == TipoArma.DISTANCIA -> "armas_distancia"
                a.id in armasDeAsta -> "armas_astas"
                else -> "armas_cuerpo"
            }
            Categoria.MUNICION -> "armas_distancia"
            Categoria.ARMADURA -> "armaduras"
            Categoria.ESCUDO -> "escudos"
            Categoria.CASCO -> "cascos"
            Categoria.CONTENEDOR, Categoria.EQUIPO -> "equipo"
        }
        return "${BASE}lamina_$lamina.webp"
    }
}
