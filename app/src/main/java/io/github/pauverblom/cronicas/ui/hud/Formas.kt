package io.github.pauverblom.cronicas.ui.hud

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.pauverblom.cronicas.ui.theme.Paleta
import io.github.pauverblom.cronicas.ui.theme.Textura
import kotlin.random.Random

// ── Formas de los interiores: cada marco recorta su cristal por sus cortes ──

/** Contorno dado por una lista de puntos en px, calculados a partir del tamaño. */
private class Poligono(private val puntos: Density.(Size) -> List<Offset>) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val p = density.puntos(size)
        val path = Path().apply {
            moveTo(p[0].x, p[0].y)
            for (i in 1 until p.size) lineTo(p[i].x, p[i].y)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Octógono con las esquinas cortadas [corte], separado [margen] del borde (ventanas, placas, engastes). */
fun formaCortada(corte: Dp, margen: Dp = 0.dp): Shape = Poligono { s ->
    val c = corte.toPx()
    val m = margen.toPx()
    val w = s.width - m
    val h = s.height - m
    listOf(
        Offset(m, m + c), Offset(m + c, m), Offset(w - c, m), Offset(w, m + c),
        Offset(w, h - c), Offset(w - c, h), Offset(m + c, h), Offset(m, h - c)
    )
}

/** Interior de las ventanas: corte de 24 px, a 2 px del marco. */
val FormaVentana = formaCortada(24.dp, 2.dp)
/** Interior de las placas: corte de 11 px. */
val FormaPlaca = formaCortada(11.dp, 2.dp)
/** Interior de los engastes pequeños: corte de 6 px. */
val FormaEngaste = formaCortada(6.dp, 1.dp)

/**
 * Interior de una placa con puntas (botón, título, etiqueta). [s] es la escala del marco (1 = 48 px de alto);
 * [margenV], [base] y [punta] son las medidas del bundle a escala 1.
 */
fun formaPlaca(s: Float, margenV: Float = 3f, base: Float = 18f, punta: Float = 7f): Shape = Poligono { z ->
    val k = density * s
    val y0 = margenV * k
    val y1 = z.height - margenV * k
    val ym = z.height / 2f
    listOf(
        Offset(base * k, y0), Offset(z.width - base * k, y0), Offset(z.width - punta * k, ym),
        Offset(z.width - base * k, y1), Offset(base * k, y1), Offset(punta * k, ym)
    )
}

/** Insignia de corte de gema (StatTile), en proporciones del marco. */
val FormaInsignia: Shape = Poligono { s ->
    fun p(x: Float, y: Float) = Offset(s.width * x, s.height * y)
    listOf(
        p(.167f, .065f), p(.5f, .009f), p(.833f, .065f), p(.988f, .167f), p(.988f, .833f),
        p(.833f, .935f), p(.5f, .991f), p(.167f, .935f), p(.012f, .833f), p(.012f, .167f)
    )
}

/** Escudo heráldico (AbilityScore). */
val FormaEscudo: Shape = Poligono { s ->
    val m = 2.dp.toPx()
    val c = 8.dp.toPx()
    val w = s.width - m
    val h = s.height - m
    listOf(Offset(m + c, m), Offset(w - c, m), Offset(w, m + c), Offset(w, m + (h - m) * .77f), Offset(s.width / 2f, h), Offset(m, m + (h - m) * .77f), Offset(m, m + c))
}

/** Pestaña con las esquinas de arriba cortadas y la base abierta. */
val FormaPestana: Shape = Poligono { s ->
    val m = 2.dp.toPx()
    val c = 16.dp.toPx()
    listOf(Offset(m, m + c), Offset(m + c, m), Offset(s.width - m - c, m), Offset(s.width - m, m + c), Offset(s.width - m, s.height), Offset(m, s.height))
}

/** Barra de una fila de salvación: del engaste a la punta derecha. */
val FormaFila: Shape = Poligono { s ->
    val y0 = 9.dp.toPx()
    val y1 = s.height - 9.dp.toPx()
    listOf(Offset(30.dp.toPx(), y0), Offset(s.width - 14.dp.toPx(), y0), Offset(s.width - 1.dp.toPx(), s.height / 2f), Offset(s.width - 14.dp.toPx(), y1), Offset(30.dp.toPx(), y1))
}

internal fun Shape.contorno(scope: DrawScope): Path {
    val o = createOutline(scope.size, scope.layoutDirection, scope)
    return when (o) {
        is Outline.Generic -> o.path
        is Outline.Rectangle -> Path().apply { addRect(o.rect) }
        is Outline.Rounded -> Path().apply { addRoundRect(o.roundRect) }
    }
}

// ── Caché de pintado ──

/** Verdadero dentro de algo que ya tiene su propia capa: lo de dentro no necesita otra. */
internal val LocalEnCapa = staticCompositionLocalOf { false }

/**
 * Pinta el componente en una capa propia fuera de pantalla: la GPU rasteriza marcos, texturas y sombras una vez y,
 * al desplazar, solo mueve la imagen. Dentro de otra capa no hace nada, para no duplicar memoria.
 *
 * Una capa recorta a sus límites, y los ornamentos sobresalen a propósito (picos, clave, placa del título), así
 * que la capa se agranda [lado], [arriba] y [abajo] sin cambiar lo que ocupa el componente en la disposición.
 * Con [siempre], la capa se crea también dentro de otra: para lo que se anima dentro de algo quieto.
 */
@Composable
internal fun Modifier.capa(lado: Dp = 0.dp, arriba: Dp = 0.dp, abajo: Dp = 0.dp, siempre: Boolean = false): Modifier {
    if (LocalEnCapa.current && !siempre) return this
    return this
        .layout { medible, limites ->
            val x = lado.roundToPx()
            val y0 = arriba.roundToPx()
            val y1 = abajo.roundToPx()
            val p = medible.measure(limites.offset(2 * x, y0 + y1))
            layout(p.width - 2 * x, p.height - y0 - y1) { p.place(-x, -y0) }
        }
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .padding(start = lado, end = lado, top = arriba, bottom = abajo)
}

/** Contenido que vive dentro de una capa (ver [capa]). */
@Composable
internal fun EnCapa(contenido: @Composable () -> Unit) = CompositionLocalProvider(LocalEnCapa provides true, content = contenido)

// ── Profundidad y brillos ──

/**
 * Sombra interior aproximada (shadow-inset): cuatro degradados, uno desde cada borde hacia dentro, que rellenan la
 * misma forma. Sin recortes ni trazos gruesos, que son lo que más cuesta pintar en cada fotograma al desplazarse.
 */
internal fun DrawScope.sombraInterior(contorno: Path, color: Color, radio: Float) {
    val b = contorno.getBounds()
    val r = radio.coerceAtMost(minOf(b.width, b.height) / 2f)
    val nada = color.copy(alpha = 0f)
    drawPath(contorno, Brush.verticalGradient(listOf(color, nada), b.top, b.top + r))
    drawPath(contorno, Brush.verticalGradient(listOf(nada, color), b.bottom - r, b.bottom))
    drawPath(contorno, Brush.horizontalGradient(listOf(color, nada), b.left, b.left + r))
    drawPath(contorno, Brush.horizontalGradient(listOf(nada, color), b.right - r, b.right))
}

/** Halo exterior (drop-shadow de brillo) alrededor de un contorno: tres trazos cada vez más anchos y tenues. */
internal fun DrawScope.halo(contorno: Path, color: Color, radio: Float, pasos: Int = 3) {
    for (i in pasos downTo 1) {
        drawPath(contorno, color.copy(alpha = color.alpha * 0.9f / pasos), style = Stroke(2f * radio * i / pasos))
    }
}

/** Degradado del cristal: vitela 200 arriba, 100 en medio, 300 abajo. */
internal fun cristal(c: Paleta) = Brush.verticalGradient(0f to c.vitela200, 0.45f to c.vitela100, 1f to c.vitela300)

/** Relleno dorado de los botones principales y casillas marcadas. */
internal fun rellenoOro(c: Paleta) = Brush.verticalGradient(0f to c.metalAlto, 0.55f to c.oroRelleno, 1f to c.oroRelleno)

// ── Textura grabada: celosía de runas + grano + brillo superior ──

enum class TipoTextura(internal val escala: Float) {
    /** Ventanas, diálogos y tarjetas: celosía grande y brillo arriba. */
    VENTANA(1f),
    /** Placas: celosía al 62 %, sin brillo. */
    PLACA(0.62f)
}

private class Teselas(val runas: Map<TipoTextura, ShaderBrush>, val grano: ShaderBrush)

private val cacheTeselas = HashMap<Triple<Color, Color, Float>, Teselas>()

private fun bitmap(lado: Int, densidad: Float, dibujar: DrawScope.() -> Unit): ImageBitmap {
    val img = ImageBitmap(lado, lado)
    CanvasDrawScope().draw(Density(densidad), LayoutDirection.Ltr, Canvas(img), Size(lado.toFloat(), lado.toFloat()), dibujar)
    return img
}

private fun teselas(c: Paleta, densidad: Float): Teselas = synchronized(cacheTeselas) {
    cacheTeselas.getOrPut(Triple(c.grabadoAlto, c.grabadoBajo, densidad)) {
        val runas = TipoTextura.entries.associateWith { t ->
            val k = t.escala * densidad
            val lado = (120f * k).toInt().coerceAtLeast(8)
            val img = bitmap(lado, densidad) {
                withTransform({ scale(k, k, Offset.Zero) }) {
                    // Cada línea dos veces: la sombra del corte desplazada debajo y el filo iluminado encima
                    translate(0.8f, 0.8f) { TESELA_RUNAS.forEach { drawPath(it.path, c.grabadoBajo, style = it.estilo) } }
                    TESELA_RUNAS.forEach { drawPath(it.path, c.grabadoAlto, style = it.estilo) }
                }
            }
            ShaderBrush(ImageShader(img, TileMode.Repeated, TileMode.Repeated))
        }
        val azar = Random(7)
        // Grano oscuro con alfa al azar: se mezcla normal, sin el modo Overlay que obliga a releer la pantalla
        val grano = bitmap(96, 1f) {
            for (y in 0 until 96) for (x in 0 until 96) {
                drawRect(Color(0f, 0f, 0f, azar.nextFloat()), Offset(x.toFloat(), y.toFloat()), Size(1f, 1f))
            }
        }
        Teselas(runas, ShaderBrush(ImageShader(grano, TileMode.Repeated, TileMode.Repeated)))
    }
}

/** Textura grabada dentro de [contorno]: se dibuja detrás del contenido y nunca baja el contraste comprobado. */
internal fun DrawScope.texturaGrabada(c: Paleta, tipo: TipoTextura, contorno: Path) {
    val t = teselas(c, density)
    clipPath(contorno) {
        drawRect(t.runas.getValue(tipo), alpha = Textura.RUNAS)
        drawRect(t.grano, alpha = Textura.GRANO * 2f)
        if (tipo == TipoTextura.VENTANA) {
            withTransform({ scale(size.width, size.height, Offset.Zero) }) {
                drawRect(
                    Brush.radialGradient(listOf(c.metalMedio, c.metalMedio.copy(alpha = 0f)), Offset(0.5f, 0f), 0.85f),
                    size = Size(1f, 1f),
                    alpha = Textura.BRILLO
                )
            }
        }
    }
}

/**
 * El interior completo de un marco: relleno recortado, textura y sombra interior.
 * Se usa desde drawBehind, antes del marco y del contenido.
 */
internal fun DrawScope.interior(
    c: Paleta,
    forma: Shape,
    relleno: Brush?,
    textura: TipoTextura? = null,
    sombra: Boolean = false
): Path {
    val contorno = forma.contorno(this)
    relleno?.let { drawPath(contorno, it) }
    textura?.let { texturaGrabada(c, it, contorno) }
    if (sombra) sombraInterior(contorno, Color.Black.copy(alpha = if (c.oscuro) 0.45f else 0.08f), 18.dp.toPx())
    return contorno
}

