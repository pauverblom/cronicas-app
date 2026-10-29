package io.github.pauverblom.cronicas.data

import kotlinx.serialization.Serializable

enum class Tema(val etiqueta: String, val descripcion: String) {
    PERGAMINO_OSCURO("Pergamino oscuro", "Marrón oscuro y oro"),
    PERGAMINO_CLARO("Pergamino claro", "Papel crema, tinta oscura y rojo"),
    MATERIAL_YOU("Material You", "Los colores del fondo de pantalla en el acento, los marcos y el fondo (Android 12+)"),
    PERSONALIZADO("Personalizado", "Elige tú el acento, los marcos y el fondo"),
    /** Solo para una ficha: los colores sacados de su retrato. */
    RETRATO("Desde retrato", "Los colores del retrato del personaje");

    companion object {
        /** Los temas que se pueden elegir para toda la app (el del retrato es de cada ficha). */
        val DE_LA_APP = entries - RETRATO
    }
}

enum class Modo(val etiqueta: String) { SISTEMA("Sistema"), CLARO("Claro"), OSCURO("Oscuro") }

/**
 * Los tres colores (ARGB) de un tema generado: el acento (títulos, pestaña activa, botón principal), los marcos
 * (el metal de ventanas y placas) y el fondo (la noche y el cristal de las ventanas).
 */
@Serializable
data class ColoresTema(val acento: Int, val marco: Int, val fondo: Int) {
    companion object {
        /** Oro, latón y azul noche: el pergamino oscuro, como punto de partida para personalizar. */
        val PREDETERMINADOS = ColoresTema(0xFFDDAC4C.toInt(), 0xFFD4A553.toInt(), 0xFF2848A8.toInt())
    }
}

data class AjustesApp(
    val tema: Tema = Tema.PERGAMINO_OSCURO,
    val modo: Modo = Modo.SISTEMA,
    /** Los colores del tema personalizado. */
    val colores: ColoresTema = ColoresTema.PREDETERMINADOS
) {
    /**
     * Los ajustes con los que se pinta la ficha de [p]: su propio tema si tiene uno, si no el de la app.
     * Con «Desde retrato» y sin retrato se usa el de la app.
     */
    fun paraPersonaje(p: Personaje?): AjustesApp {
        val propio = p?.tema?.let { n -> Tema.entries.firstOrNull { it.name == n } } ?: return this
        return when (propio) {
            Tema.PERSONALIZADO -> copy(tema = propio, colores = p.coloresPropios() ?: colores)
            Tema.RETRATO -> p.coloresRetrato()?.let { copy(tema = propio, colores = it) } ?: this
            else -> copy(tema = propio)
        }
    }
}

/** Los colores personalizados de la ficha, si los tiene. */
fun Personaje.coloresPropios(): ColoresTema? =
    if (colorAcento != null && colorMarco != null && colorFondo != null) ColoresTema(colorAcento, colorMarco, colorFondo) else null

/** Los colores sacados del retrato. Las fichas de antes solo guardaban uno: se usa para los tres. */
fun Personaje.coloresRetrato(): ColoresTema? =
    colorSemilla?.let { ColoresTema(it, colorRetratoMarco ?: it, colorRetratoFondo ?: it) }

/** El personaje con un retrato nuevo y sus colores (o sin colores, si no se pudieron sacar). */
fun Personaje.conRetrato(path: String?, colores: ColoresTema?): Personaje =
    copy(fotoPath = path, colorSemilla = colores?.acento, colorRetratoMarco = colores?.marco, colorRetratoFondo = colores?.fondo)
