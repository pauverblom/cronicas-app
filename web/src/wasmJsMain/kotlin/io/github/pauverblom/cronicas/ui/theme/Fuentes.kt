package io.github.pauverblom.cronicas.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import io.github.pauverblom.cronicas.web.recursos.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Las tipografías de la app, cargadas de los recursos web antes de pintar nada ([cargar]). */
object FuentesWeb {
    lateinit var cinzel: FontFamily
    lateinit var alegreya: FontFamily
    lateinit var alegreyaSans: FontFamily
    lateinit var cinzelDecorativa: FontFamily

    @OptIn(ExperimentalResourceApi::class)
    suspend fun cargar() {
        suspend fun bytes(archivo: String) = Res.readBytes("files/fuentes/$archivo")
        val cinzelTtf = bytes("cinzel.ttf")
        val alegreyaTtf = bytes("alegreya.ttf")
        cinzel = FontFamily(
            Font("cinzel-500", cinzelTtf, FontWeight(500)),
            Font("cinzel-600", cinzelTtf, FontWeight(600)),
            Font("cinzel-700", cinzelTtf, FontWeight(700))
        )
        alegreya = FontFamily(
            Font("alegreya-400", alegreyaTtf, FontWeight(400)),
            Font("alegreya-500", alegreyaTtf, FontWeight(500)),
            Font("alegreya-700", alegreyaTtf, FontWeight(700)),
            Font("alegreya-italic", bytes("alegreya_italic.ttf"), FontWeight(400), FontStyle.Italic)
        )
        alegreyaSans = FontFamily(
            Font("alegreya-sans-400", bytes("alegreya_sans_regular.ttf"), FontWeight.Normal),
            Font("alegreya-sans-500", bytes("alegreya_sans_medium.ttf"), FontWeight.Medium),
            Font("alegreya-sans-700", bytes("alegreya_sans_bold.ttf"), FontWeight.Bold)
        )
        cinzelDecorativa = FontFamily(Font("cinzel-decorative", bytes("cinzel_decorative_bold.ttf"), FontWeight.Bold))
    }
}

/** Cinzel: capitales romanas para todo lo tallado (títulos, cifras, etiquetas, botones). */
val Cinzel: FontFamily get() = FuentesWeb.cinzel

/** Alegreya: la tipografía del libro, para lo que se cuenta (descripciones, cuerpo, lore). */
val Alegreya: FontFamily get() = FuentesWeb.alegreya

/** Alegreya Sans: la interfaz (listas, metadatos, campos, ayudas). */
val AlegreyaSans: FontFamily get() = FuentesWeb.alegreyaSans

/** Cinzel Decorative: iniciales del retrato y del sello de lacre. */
val CinzelDecorativa: FontFamily get() = FuentesWeb.cinzelDecorativa
