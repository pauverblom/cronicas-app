package io.github.pauverblom.cronicas.ui.common

import io.github.pauverblom.cronicas.rules.data.ArticuloDef
import io.github.pauverblom.cronicas.rules.data.Categoria
import io.github.pauverblom.cronicas.rules.data.TipoArma

/**
 * Ilustraciones del reglamento en assets/ilustraciones (generadas con scripts/extract_ilustraciones.py).
 * Devuelve URIs que Coil carga directamente.
 */
object Ilustraciones {
    private const val BASE = "file:///android_asset/ilustraciones/"

    fun clase(id: String): String = "${BASE}clase_$id.webp"
    fun especie(id: String): String = "${BASE}especie_$id.webp"

    /** Piezas recortadas de las láminas de armaduras, escudos y cascos. */
    private val piezas = setOf(
        "cuero_acolchado", "cuero_endurecido", "cuero_tachonado", "cota_escamas", "anillas", "cota_mallas",
        "laminada", "peto_coraza", "cota_bandas", "placas_mallas", "armadura_completa",
        "cofia_cuero", "bacinete", "celada", "cofia_mallas", "yelmo", "yelmo_completo", "paves"
    )

    private val armasDeAsta = setOf(
        "alabarda", "lanza", "lanza_caballeria", "pica", "tridente", "jabalina", "guadana", "latigo", "baston"
    )

    /** La pieza recortada si el libro la dibuja suelta; si no, la lámina de su categoría. */
    fun articulo(a: ArticuloDef): String {
        val pieza = when {
            a.id in piezas -> a.id
            a.id == "broquel" || a.id.startsWith("escudo_pequeno") -> "broquel"
            a.id.startsWith("escudo_medio") -> "escudo_medio"
            a.id.startsWith("escudo_pesado") -> "escudo_pesado"
            else -> null
        }
        if (pieza != null) return "${BASE}objeto_$pieza.webp"
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
