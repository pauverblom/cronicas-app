package io.github.pauverblom.cronicas.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import io.github.pauverblom.cronicas.ui.hud.VentanaArte
import io.github.pauverblom.cronicas.ui.hud.foco
import io.github.pauverblom.cronicas.ui.hud.marcoEleccion
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos

/** Verdadero mientras el carrusel mide sus tarjetas fuera de pantalla: las ilustraciones no cargan la imagen. */
private val LocalMidiendo = staticCompositionLocalOf { false }

/** Ancho de las tarjetas del carrusel. */
private val AnchoTarjeta = 280.dp

/**
 * Ilustración del reglamento sobre papel. Si no hay imagen y se indica [archivoFaltante], muestra el aviso
 * «MISSING PICTURE» con el archivo que falta; si no, una «?» (opciones personalizadas).
 */
@Composable
fun Ilustracion(uri: String?, nombre: String, alto: Dp, modifier: Modifier = Modifier, archivoFaltante: String? = null) {
    VentanaArte(uri, nombre, alto, modifier, archivoFaltante, medir = LocalMidiendo.current)
}

/**
 * Carrusel de tarjetas de elección de una en una; las vecinas asoman por los lados.
 * [seleccionada] marca la tarjeta elegida; tocar una tarjeta la elige.
 * Con [llama], la tarjeta elegida arde: el fuego recorre su marco, respira, se eleva, suelta chispas y vibra.
 * Todas las tarjetas miden lo que la más alta, así el carrusel no cambia de alto al deslizar.
 */
@Composable
fun <T> Carrusel(
    estado: PagerState,
    elementos: List<T>,
    seleccionada: (T) -> Boolean,
    onElegir: (T) -> Unit,
    modifier: Modifier = Modifier,
    clave: ((T) -> Any)? = null,
    llama: ((T) -> Llama)? = null,
    contenido: @Composable ColumnScope.(T) -> Unit
) {
    val c = Marca.colores
    Column(modifier) {
        // El pager solo compone las páginas visibles y toma el alto de la mayor de ellas; medimos antes todas
        // las tarjetas al ancho de página para fijar un alto común.
        val medicion = @Composable {
            CompositionLocalProvider(LocalMidiendo provides true) {
                elementos.forEach { Tarjeta(it, elegida = false, onElegir = {}, Modifier, contenido) }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val margen = max((maxWidth - AnchoTarjeta) / 2, 24.dp)
            val ancho = maxWidth - margen * 2
            val pager = @Composable { alto: Dp ->
                HorizontalPager(
                    state = estado,
                    modifier = Modifier.height(alto),
                    pageSize = PageSize.Fixed(ancho),
                    contentPadding = PaddingValues(horizontal = margen),
                    pageSpacing = 12.dp,
                    key = clave?.let { k -> { i: Int -> k(elementos[i]) } }
                ) { i ->
                    val e = elementos[i]
                    Tarjeta(e, seleccionada(e), onElegir, Modifier.fillMaxHeight(), contenido, llama?.invoke(e))
                }
            }
            SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
                val px = ancho.roundToPx()
                val alto = subcompose("medicion", medicion)
                    .maxOfOrNull { it.measure(Constraints(minWidth = px, maxWidth = px)).height } ?: 0
                val colocable = subcompose("pager") { pager(alto.toDp()) }.first().measure(constraints.copy(minHeight = 0))
                layout(colocable.width, colocable.height) { colocable.place(0, 0) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
            Text("${estado.currentPage + 1} / ${elementos.size}", style = Tipos.etiquetaCaps, color = c.tintaSuave)
        }
    }
}

@Composable
private fun <T> Tarjeta(
    elemento: T,
    elegida: Boolean,
    onElegir: (T) -> Unit,
    modifier: Modifier,
    contenido: @Composable ColumnScope.(T) -> Unit,
    llama: Llama? = null
) {
    if (llama != null) {
        val haptica = LocalHapticFeedback.current
        val previa = remember { booleanArrayOf(elegida) }
        LaunchedEffect(elegida) {
            val recienElegida = elegida && !previa[0]
            previa[0] = elegida
            if (recienElegida) haptica.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val interaccion = remember { MutableInteractionSource() }
    // Margen arriba para la cresta y la elevación, abajo para la clave que cuelga
    Box(modifier.fillMaxWidth().padding(top = 14.dp, bottom = 20.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .marcoEleccion(elegida, llama)
                .foco(interaccion)
                .clickable(interaccion, null, role = Role.Button) { onElegir(elemento) }
                .semantics { selected = elegida }
                // A los lados, sitio para el triple marco y los picos: el texto no toca los ornamentos
                .padding(start = 26.dp, end = 26.dp, top = 18.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) { contenido(elemento) }
        if (llama != null) Estallido(elegida, llama, Modifier.matchParentSize())
    }
}
