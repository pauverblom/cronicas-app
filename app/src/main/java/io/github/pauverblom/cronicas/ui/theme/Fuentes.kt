package io.github.pauverblom.cronicas.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.pauverblom.cronicas.R

// Las tipografías salen de los recursos de Android; la versión web las carga de sus propios archivos.

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
