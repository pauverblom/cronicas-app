package io.github.pauverblom.cronicas.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.pauverblom.cronicas.data.ColoresTema
import io.github.pauverblom.cronicas.ui.hud.Campo
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos

/** Los tres colores de un tema, cada uno con su muestra; con [onCambiar], tocar una abre el selector. */
@Composable
fun MuestrasColores(colores: ColoresTema, onCambiar: ((ColoresTema) -> Unit)?, modifier: Modifier = Modifier) {
    var editando by remember { mutableStateOf<Int?>(null) }
    val partes = listOf(
        Triple("Acento", colores.acento) { v: Int -> colores.copy(acento = v) },
        Triple("Marcos", colores.marco) { v: Int -> colores.copy(marco = v) },
        Triple("Fondo", colores.fondo) { v: Int -> colores.copy(fondo = v) }
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        partes.forEachIndexed { i, (nombre, color, _) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Muestra(
                    Color(color), 44, nombre,
                    onClick = onCambiar?.let { { editando = i } }
                )
                Text(nombre, style = Tipos.caption, color = Marca.colores.tintaSuave, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    val i = editando
    if (i != null && onCambiar != null) {
        val (nombre, color, cambiar) = partes[i]
        SelectorColorDialog(nombre, color, onElegir = { onCambiar(cambiar(it)) }, onDismiss = { editando = null })
    }
}

/** Un cuadrado de color con el corte de las placas. */
@Composable
private fun Muestra(color: Color, lado: Int, descripcion: String, elegida: Boolean = false, onClick: (() -> Unit)? = null) {
    val c = Marca.colores
    val forma = CutCornerShape(6.dp)
    Box(
        Modifier
            .size(lado.dp)
            .clip(forma)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics { contentDescription = descripcion }
            .border(if (elegida) 3.dp else 1.5.dp, if (elegida) c.foco else c.metalMedio, forma)
            .padding(if (elegida) 5.dp else 3.dp)
            .clip(forma)
    ) {
        Canvas(Modifier.matchParentSize()) { drawRect(color) }
    }
}

/** Colores de partida: el oro y el latón del pergamino y una gama de tonos de fantasía. */
private val SUGERIDOS = listOf(
    0xFFDDAC4C, 0xFFD4A553, 0xFFC0C6D0, 0xFFB87333, 0xFF9E2A1F, 0xFFEA6253,
    0xFFE07A2E, 0xFF7FCB7A, 0xFF2E7D5B, 0xFF2BB3A6, 0xFF7EA9E6, 0xFF2848A8,
    0xFF6A4FC4, 0xFFB49CF0, 0xFFC2448C, 0xFF5C4630
).map { it.toInt() }

/**
 * Selector de color: tono en una barra, saturación y brillo en un recuadro, sugerencias y el código hexadecimal.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorColorDialog(titulo: String, inicial: Int, onElegir: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = Marca.colores
    val hsv = remember { aHsv(inicial) }
    var tono by remember { mutableFloatStateOf(hsv[0]) }
    var saturacion by remember { mutableFloatStateOf(hsv[1]) }
    var brillo by remember { mutableFloatStateOf(hsv[2]) }
    val actual = Color.hsv(tono, saturacion, brillo)
    var hex by remember { mutableStateOf(aHex(inicial)) }
    fun poner(argb: Int) {
        val n = aHsv(argb)
        tono = n[0]; saturacion = n[1]; brillo = n[2]
        hex = aHex(argb)
    }
    fun mover() { hex = aHex(Color.hsv(tono, saturacion, brillo).toArgb()) }

    FormularioDialog(titulo, onDismiss = onDismiss, onGuardar = { onElegir(actual.toArgb()); onDismiss() }, guardar = "Usar") {
        // Saturación (horizontal) y brillo (vertical) del tono elegido
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(CutCornerShape(6.dp))
                .semantics { contentDescription = "Saturación y brillo" }
                .pointerInput(Unit) {
                    fun en(p: Offset) {
                        saturacion = (p.x / size.width).coerceIn(0f, 1f)
                        brillo = 1f - (p.y / size.height).coerceIn(0f, 1f)
                        mover()
                    }
                    awaitEachGesture {
                        val abajo = awaitFirstDown()
                        en(abajo.position)
                        drag(abajo.id) { cambio -> en(cambio.position); cambio.consume() }
                    }
                }
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(tono, 1f, 1f))))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val punto = Offset(saturacion * size.width, (1f - brillo) * size.height)
            drawCircle(Color.Black, 9.dp.toPx(), punto, style = Stroke(3.dp.toPx()))
            drawCircle(Color.White, 9.dp.toPx(), punto, style = Stroke(1.5.dp.toPx()))
        }
        // Tono
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(CutCornerShape(6.dp))
                .semantics { contentDescription = "Tono" }
                .pointerInput(Unit) {
                    fun en(p: Offset) {
                        tono = (p.x / size.width).coerceIn(0f, 1f) * 359.9f
                        mover()
                    }
                    awaitEachGesture {
                        val abajo = awaitFirstDown()
                        en(abajo.position)
                        drag(abajo.id) { cambio -> en(cambio.position); cambio.consume() }
                    }
                }
        ) {
            drawRect(Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) }))
            val x = tono / 360f * size.width
            drawRect(Color.Black, Offset(x - 3.dp.toPx(), 0f), androidx.compose.ui.geometry.Size(6.dp.toPx(), size.height), style = Stroke(2.dp.toPx()))
            drawRect(Color.White, Offset(x - 2.dp.toPx(), 0f), androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height), style = Stroke(1.dp.toPx()))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SUGERIDOS.forEach { s -> Muestra(Color(s), 32, aHex(s), elegida = s == actual.toArgb(), onClick = { poner(s) }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Muestra(actual, 48, "Color elegido")
            Campo(
                hex,
                { v ->
                    hex = v.uppercase().filter { it == '#' || it.isDigit() || it in 'A'..'F' }.take(7)
                    deHex(hex)?.let { argb -> aHsv(argb).let { n -> tono = n[0]; saturacion = n[1]; brillo = n[2] } }
                },
                "Código",
                Modifier.weight(1f),
                marcador = "#RRGGBB"
            )
        }
        Text(
            "Toca el recuadro y la barra, o elige un color sugerido.",
            style = Tipos.caption, color = c.tintaSuave, textAlign = TextAlign.Start
        )
    }
}

/** Tono (0..360), saturación y valor (0..1) de un color ARGB. */
internal fun aHsv(argb: Int): FloatArray {
    val r = (argb shr 16 and 0xFF) / 255f
    val g = (argb shr 8 and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return floatArrayOf(h, if (max == 0f) 0f else d / max, max)
}

internal fun aHex(argb: Int): String = "#" + (argb and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

internal fun deHex(texto: String): Int? {
    val t = texto.removePrefix("#")
    if (t.length != 6) return null
    return t.toIntOrNull(16)?.let { it or (0xFF shl 24) }
}
