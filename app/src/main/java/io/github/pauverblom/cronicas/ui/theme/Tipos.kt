package io.github.pauverblom.cronicas.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.pauverblom.cronicas.R

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, peso: Int, estilo: FontStyle = FontStyle.Normal) =
    Font(res, FontWeight(peso), estilo, variationSettings = FontVariation.Settings(FontVariation.weight(peso)))

/** Cinzel: capitales romanas para todo lo tallado (títulos, cifras, etiquetas, botones). */
val Cinzel = FontFamily(
    variable(R.font.cinzel, 500),
    variable(R.font.cinzel, 600),
    variable(R.font.cinzel, 700)
)

/** Alegreya: la tipografía del libro, para lo que se cuenta (descripciones, cuerpo, lore). */
val Alegreya = FontFamily(
    variable(R.font.alegreya, 400),
    variable(R.font.alegreya, 500),
    variable(R.font.alegreya, 700),
    variable(R.font.alegreya_italic, 400, FontStyle.Italic)
)

/** Alegreya Sans: la interfaz (listas, metadatos, campos, ayudas). */
val AlegreyaSans = FontFamily(
    Font(R.font.alegreya_sans_regular, FontWeight.Normal),
    Font(R.font.alegreya_sans_medium, FontWeight.Medium),
    Font(R.font.alegreya_sans_bold, FontWeight.Bold)
)

/** Cinzel Decorative: iniciales del retrato y del sello de lacre. */
val CinzelDecorativa = FontFamily(Font(R.font.cinzel_decorative_bold, FontWeight.Bold))

private val SinRelleno = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun estilo(familia: FontFamily, tamano: Int, linea: Int, peso: Int, espaciado: Double = 0.0, cursiva: Boolean = false) =
    TextStyle(
        fontFamily = familia,
        fontSize = tamano.sp,
        lineHeight = linea.sp,
        fontWeight = FontWeight(peso),
        fontStyle = if (cursiva) FontStyle.Italic else FontStyle.Normal,
        letterSpacing = espaciado.em,
        lineHeightStyle = SinRelleno
    )

/** Estilos de texto del sistema (type.groups de tokens.json). */
object Tipos {
    /** Nombre del personaje en la ficha y la forja; títulos de estado vacío. */
    val tituloXl = estilo(Cinzel, 34, 40, 700, 0.02)
    /** Títulos de pantalla, de paso, de diálogo y de tarjeta. */
    val tituloLg = estilo(Cinzel, 24, 30, 600, 0.02)
    /** Encabezados de ventana: siempre en mayúsculas y en oro. */
    val encabezado = estilo(Cinzel, 15, 20, 700, 0.14)
    /** Etiquetas de estadística, pestañas, insignias como «EN MANO». */
    val etiquetaCaps = estilo(Cinzel, 12, 16, 700, 0.16)
    /** Texto de botón (en mayúsculas desde el componente). */
    val boton = estilo(Cinzel, 14, 20, 700, 0.08)

    /** PG actuales en la ficha. */
    val cifraXl = estilo(Cinzel, 60, 60, 700)
    /** CA, modificadores, PX. */
    val cifraLg = estilo(Cinzel, 34, 38, 700)
    /** Puntuaciones, monedas, BA y daño. */
    val cifraMd = estilo(Cinzel, 22, 26, 700)

    /** Descripciones de especie y clase, citas de reglas. */
    val lore = estilo(Alegreya, 17, 27, 400, cursiva = true)
    val cuerpo = estilo(Alegreya, 16, 24, 400)
    val cuerpoSm = estilo(Alegreya, 14, 20, 400)

    /** Títulos de elementos de lista, valores de campo, nombres de inventario. */
    val ui = estilo(AlegreyaSans, 16, 22, 500)
    /** Líneas de metadatos, etiquetas de campo. */
    val uiSm = estilo(AlegreyaSans, 14, 18, 500)
    /** Detalle de objetos, ayudas y errores, contadores. */
    val caption = estilo(AlegreyaSans, 12, 16, 500)

    /** Inicial del retrato y del sello. */
    val inicial = estilo(CinzelDecorativa, 56, 56, 700)
}
