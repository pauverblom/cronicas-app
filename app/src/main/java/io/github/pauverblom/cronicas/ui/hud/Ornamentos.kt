package io.github.pauverblom.cronicas.ui.hud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import io.github.pauverblom.cronicas.ui.common.Llama
import io.github.pauverblom.cronicas.ui.common.animacionesActivas
import io.github.pauverblom.cronicas.ui.theme.Paleta

/** Una figura de un ornamento: su trazado en unidades del ornamento y cómo se pinta. */
internal class Prim(
    val d: String,
    val relleno: Boolean,
    val trazo: Boolean,
    val grosor: Float,
    val guiones: FloatArray?
) {
    val path: Path by lazy { PathParser().parsePathString(d).toPath() }
    val estilo: Stroke by lazy {
        Stroke(grosor, miter = 4f, join = StrokeJoin.Miter, pathEffect = guiones?.let { PathEffect.dashPathEffect(it) })
    }
}

/** [cortes] son los bordes fijos del 9-slice (arriba, derecha, abajo, izquierda), o null si no se estira. */
internal class DatosOrnamento(val ancho: Float, val alto: Float, val cortes: IntArray?, val prims: List<Prim>)

/** Los ornamentos del grupo «Ornaments» del sistema de diseño. */
enum class Orn(internal val clave: String) {
    /** Ventanas: Panel, Dialog, ChoiceCard. */
    PANEL("frame-panel"),
    /** Placas escalonadas, pozos y engastes. */
    CHAFLAN("frame-chamfer"),
    /** Placas con remates: botones, títulos de ventana, etiquetas. */
    PLACA("frame-plaque"),
    PESTANA("frame-tab"),
    /** Filas de salvación con engaste de gema. */
    FILA("frame-row"),
    BARRA("frame-gauge"),
    PUNTA("gauge-cap"),
    INSIGNIA("frame-badge"),
    ESCUDO("frame-shield"),
    ORBE("frame-orb"),
    ANILLO("frame-ring"),
    ENGASTE("socket"),
    PICO("edge-spike"),
    CLAVE("keystone"),
    CRESTA("crest"),
    MEDALLON("medallion"),
    SIGILO("sigil");

    internal val datos: DatosOrnamento get() = DATOS_ORNAMENTOS.getValue(clave)
}

/** Con qué se pinta un ornamento: el oro, el hierro, el fuego de una clase o un color plano. */
sealed interface Pintura {
    data object Metal : Pintura
    data object Hierro : Pintura
    /** [fase] desplaza el degradado para que el fuego corra por el marco. */
    data class Fuego(val llama: Llama, val fase: Float = 0f) : Pintura
    data class Plana(val color: Color) : Pintura
}

/** Tramo del degradado en unidades del ornamento (como en el bundle: de 0,0 a 60,60, reflejado). */
private const val TRAMO = 60f

internal fun Pintura.brocha(c: Paleta): Brush = when (this) {
    Pintura.Metal -> Brush.linearGradient(
        0f to c.metalAlto, 0.28f to c.metalMedio, 0.5f to c.metalBajo, 0.72f to c.metalMedio, 1f to c.metalAlto,
        start = Offset.Zero, end = Offset(TRAMO, TRAMO), tileMode = TileMode.Mirror
    )
    Pintura.Hierro -> Brush.linearGradient(
        0f to c.hierroAlto, 0.35f to c.hierro, 0.55f to c.hierroBajo, 0.75f to c.hierro, 1f to c.hierroAlto,
        start = Offset.Zero, end = Offset(TRAMO, TRAMO), tileMode = TileMode.Mirror
    )
    is Pintura.Fuego -> Brush.linearGradient(
        0f to llama.nucleo, 0.3f to llama.brillo, 0.55f to llama.chispa, 0.8f to llama.brillo, 1f to llama.nucleo,
        start = Offset(fase, 0f), end = Offset(TRAMO + fase, TRAMO), tileMode = TileMode.Mirror
    )
    is Pintura.Plana -> SolidColor(color)
}

/** Fase del fuego que corre por un marco: un ciclo cada 2,4 s (quieto sin animaciones). */
@Composable
fun rememberFaseFuego(): State<Float> {
    if (!animacionesActivas()) return remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val t = rememberInfiniteTransition(label = "fuego")
    return t.animateFloat(0f, TRAMO * 2f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "fase")
}

private fun DrawScope.figuras(prims: List<Prim>, brocha: Brush, alfa: Float) {
    for (p in prims) {
        if (p.relleno) drawPath(p.path, brocha, alpha = alfa)
        if (p.trazo) drawPath(p.path, brocha, alpha = alfa, style = p.estilo)
    }
}

/** Dibuja un ornamento entero estirado a [destino] (sin recortar: los remates pueden salirse). */
internal fun DrawScope.ornamento(o: Orn, brocha: Brush, destino: Rect = Rect(Offset.Zero, size), alfa: Float = 1f) {
    val d = o.datos
    withTransform({
        translate(destino.left, destino.top)
        scale(destino.width / d.ancho, destino.height / d.alto, Offset.Zero)
    }) { figuras(d.prims, brocha, alfa) }
}

/**
 * Marco 9-slice que llena [destino]: esquinas fijas y bordes estirados a lo largo, como el Frame del bundle.
 * [k] son píxeles por unidad del ornamento.
 */
internal fun DrawScope.marco(o: Orn, brocha: Brush, k: Float, destino: Rect = Rect(Offset.Zero, size), alfa: Float = 1f) {
    val d = o.datos
    val c = d.cortes ?: return ornamento(o, brocha, destino, alfa)
    val (t, r, b, l) = c.map { it.toFloat() }
    val anchoCentro = d.ancho - l - r
    val altoCentro = d.alto - t - b
    val x0 = destino.left
    val y0 = destino.top
    val x1 = x0 + l * k
    val y1 = y0 + t * k
    val x2 = (destino.right - r * k).coerceAtLeast(x1)
    val y2 = (destino.bottom - b * k).coerceAtLeast(y1)
    val x3 = destino.right
    val y3 = destino.bottom

    fun trozo(sx: Float, sy: Float, sw: Float, sh: Float, dx: Float, dy: Float, dw: Float, dh: Float) {
        if (sw <= 0f || sh <= 0f || dw <= 0f || dh <= 0f) return
        clipRect(dx, dy, dx + dw, dy + dh) {
            withTransform({
                translate(dx - sx * dw / sw, dy - sy * dh / sh)
                scale(dw / sw, dh / sh, Offset.Zero)
            }) { figuras(d.prims, brocha, alfa) }
        }
    }
    if (t > 0f) {
        trozo(0f, 0f, l, t, x0, y0, x1 - x0, y1 - y0)
        trozo(l, 0f, anchoCentro, t, x1, y0, x2 - x1, y1 - y0)
        trozo(d.ancho - r, 0f, r, t, x2, y0, x3 - x2, y1 - y0)
    }
    trozo(0f, t, l, altoCentro, x0, y1, x1 - x0, y2 - y1)
    trozo(d.ancho - r, t, r, altoCentro, x2, y1, x3 - x2, y2 - y1)
    if (b > 0f) {
        trozo(0f, d.alto - b, l, b, x0, y2, x1 - x0, y3 - y2)
        trozo(l, d.alto - b, anchoCentro, b, x1, y2, x2 - x1, y3 - y2)
        trozo(d.ancho - r, d.alto - b, r, b, x2, y2, x3 - x2, y3 - y2)
    }
}
