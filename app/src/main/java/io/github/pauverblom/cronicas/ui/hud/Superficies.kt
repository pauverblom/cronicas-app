package io.github.pauverblom.cronicas.ui.hud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.pauverblom.cronicas.ui.common.Llama
import io.github.pauverblom.cronicas.ui.common.animacionesActivas
import io.github.pauverblom.cronicas.ui.common.llamaDe
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Paleta
import io.github.pauverblom.cronicas.ui.theme.Tipos
import kotlin.random.Random

// ── Página ──

/** Motas de la noche: posiciones fijas en fracciones, para que no bailen al recomponer. */
private val Motas: List<Triple<Float, Float, Float>> = Random(11).let { r ->
    List(90) { Triple(r.nextFloat(), r.nextFloat(), 0.5f + r.nextFloat() * 0.8f) }
}

/** Fondo de toda la app: la noche con dos manchas de bruma y estrellas sueltas (fibras del papel en claro). */
fun Modifier.fondoNoche(c: Paleta): Modifier = this.drawWithCache {
    val bruma1 = Brush.radialGradient(listOf(c.bruma, Color.Transparent), Offset(size.width * 0.12f, size.height * 0.18f), size.maxDimension * 0.5f)
    val bruma2 = Brush.radialGradient(listOf(c.bruma, Color.Transparent), Offset(size.width * 0.92f, size.height * 0.88f), size.maxDimension * 0.48f)
    onDrawBehind {
        drawRect(c.noche)
        drawRect(bruma1)
        drawRect(bruma2)
        for ((x, y, r) in Motas) {
            drawCircle(c.estrella.copy(alpha = 0.55f), r * density, Offset(x * size.width, y * size.height))
        }
    }
}

// ── Ventana (Panel) ──

/** Las piezas que lleva toda ventana: un pico en cada lado y la clave colgando abajo. */
internal fun DrawScope.kitVentana(brocha: Brush) {
    val d = density
    ornamento(Orn.PICO, brocha, Rect(Offset(-6 * d, size.height / 2 - 28 * d), Size(14 * d, 56 * d)))
    ornamento(Orn.PICO, brocha, Rect(Offset(size.width - 8 * d, size.height / 2 - 28 * d), Size(14 * d, 56 * d)))
    ornamento(Orn.CLAVE, brocha, Rect(Offset(size.width / 2 - 20 * d, size.height - 3 * d), Size(40 * d, 22 * d)))
}

/** Cristal, textura y marco de una ventana, con su kit. [pintura] es oro, o el fuego de una tarjeta elegida. */
internal fun Modifier.marcoVentana(c: Paleta, pintura: Pintura = Pintura.Metal): Modifier = this.drawBehind {
    interior(c, FormaVentana, cristal(c), TipoTextura.VENTANA, sombra = true)
    val brocha = pintura.brocha(c)
    marco(Orn.PANEL, brocha, density)
    kitVentana(brocha)
}

/**
 * La ventana del HUD: cristal oscuro en un triple marco de oro con esquinas escalonadas, picos en los lados, una
 * clave colgando y el título sobre una placa que monta el borde de arriba. No se anidan ventanas.
 */
@Composable
fun Ventana(
    titulo: String?,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val c = Marca.colores
    Box(modifier.fillMaxWidth().padding(top = if (titulo != null) 16.dp else 0.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .marcoVentana(c)
                .padding(start = 30.dp, end = 30.dp, bottom = 34.dp, top = if (accion != null) 56.dp else if (titulo != null) 38.dp else 32.dp),
            content = contenido
        )
        if (titulo != null) PlacaTitulo(titulo, Modifier.align(Alignment.TopCenter).offset(y = (-15).dp))
        if (accion != null) Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 14.dp)) { accion() }
    }
}

/** Placa del título de una ventana (escala 0,625 del marco de placa). */
@Composable
fun PlacaTitulo(titulo: String, modifier: Modifier = Modifier) {
    val c = Marca.colores
    val s = 0.625f
    Box(
        modifier
            .height(30.dp)
            .drawBehind {
                halo(formaPlaca(s).contorno(this), c.noche.copy(alpha = 0.8f), 8.dp.toPx())
                interior(c, formaPlaca(s), SolidColor(c.vitela300))
                marco(Orn.PLACA, Pintura.Metal.brocha(c), s * density)
            }
            .padding(horizontal = 24.dp)
            .semantics { heading() },
        contentAlignment = Alignment.Center
    ) {
        Text(titulo.uppercase(), style = Tipos.encabezado.copy(fontSize = 14.sp, letterSpacing = 0.16.em), color = c.oro, maxLines = 1)
    }
}

/**
 * Placa escalonada: el segundo nivel del HUD (filas de la lista, ataques, notas, la bandeja de monedas).
 * [pintura] es oro para lo elegido o en uso, hierro para lo demás.
 */
@Composable
fun Placa(
    modifier: Modifier = Modifier,
    pintura: Pintura = Pintura.Hierro,
    relleno: Brush? = null,
    textura: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClickLargo: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val c = Marca.colores
    val interaccion = remember { MutableInteractionSource() }
    Column(
        modifier
            .then(
                when {
                    onClickLargo != null -> Modifier.foco(interaccion).combinedClickableSinIndicacion(interaccion, onClick ?: {}, onClickLargo)
                    onClick != null -> Modifier.foco(interaccion).clickable(interaccion, null, role = Role.Button, onClick = onClick)
                    else -> Modifier
                }
            )
            .drawBehind {
                interior(c, FormaPlaca, relleno ?: cristal(c), if (textura) TipoTextura.PLACA else null)
                marco(Orn.CHAFLAN, pintura.brocha(c), 16f / 18f * density)
            },
        content = contenido
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableSinIndicacion(interaccion: MutableInteractionSource, onClick: () -> Unit, onLargo: () -> Unit) =
    combinedClickable(interactionSource = interaccion, indication = null, role = Role.Button, onLongClick = onLargo, onClick = onClick)

// ── Separador (Divider) ──

/** Filete dorado que se desvanece hacia los lados hasta un medallón de rombo: cambio de tema dentro de una ventana. */
@Composable
fun Separador(modifier: Modifier = Modifier, compacto: Boolean = false) {
    val c = Marca.colores
    val ancho = if (compacto) 60.dp else 80.dp
    val alto = if (compacto) 15.dp else 20.dp
    Canvas(
        modifier
            .fillMaxWidth()
            .padding(vertical = if (compacto) 4.dp else 12.dp)
            .height(alto)
    ) {
        val wm = ancho.toPx()
        val y = size.height / 2f
        val izq = (size.width - wm) / 2f
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, c.dorado), 0f, izq), Offset(0f, y - 0.5f), Size(izq, 1.dp.toPx()))
        drawRect(Brush.horizontalGradient(listOf(c.dorado, Color.Transparent), izq + wm, size.width), Offset(izq + wm, y - 0.5f), Size(izq, 1.dp.toPx()))
        ornamento(Orn.MEDALLON, Pintura.Metal.brocha(c), Rect(Offset(izq, 0f), Size(wm, size.height)))
    }
}

/** Filete fino entre filas de una lista (la regla que se desvanece en los extremos). */
@Composable
fun Filete(modifier: Modifier = Modifier) {
    val c = Marca.colores
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.15f to c.regla, 0.85f to c.regla, 1f to Color.Transparent))
    }
}

/** Subtítulo dentro de una ventana: versalitas de oro, pequeñas. */
@Composable
fun Subtitulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(),
        style = Tipos.etiquetaCaps,
        color = Marca.colores.oro,
        modifier = modifier.semantics { heading() }
    )
}

// ── Diálogo ──

/**
 * Ventana modal para confirmaciones y formularios: marco de ventana con su cresta, título centrado sobre un
 * separador, el cuerpo (desplazable) y las acciones abajo a la derecha, con [extra] a la izquierda.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Dialogo(
    titulo: String,
    onDismiss: () -> Unit,
    acciones: @Composable RowScope.() -> Unit,
    extra: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val c = Marca.colores
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 28.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .marcoVentana(c)
                        .padding(start = 30.dp, end = 30.dp, top = 40.dp, bottom = 36.dp)
                ) {
                    Text(
                        titulo,
                        style = Tipos.tituloLg.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.06.em),
                        color = c.tinta,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().semantics { heading() }
                    )
                    Separador(compacto = true)
                    Column(
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalTextStyle provides Tipos.cuerpo.copy(color = c.tinta),
                            androidx.compose.material3.LocalContentColor provides c.tinta
                        ) { contenido() }
                    }
                    // Las acciones bajan de línea en pantallas estrechas; la principal queda la última
                    FlowRow(
                        Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (extra != null) {
                            extra()
                            Spacer(Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = acciones)
                    }
                }
                Canvas(Modifier.align(Alignment.TopCenter).offset(y = (-13).dp).size(80.dp, 28.dp)) {
                    ornamento(Orn.CRESTA, Pintura.Metal.brocha(c))
                }
            }
        }
    }
}

// ── Aviso ──

/** Aviso de regla generado por la app: placa en color de aviso con el triángulo en un engaste. No se descarta. */
@Composable
fun Aviso(texto: String, modifier: Modifier = Modifier, titulo: String? = null, peligro: Boolean = false) {
    val c = Marca.colores
    val tono = if (peligro) c.sangre else c.alerta
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                interior(c, FormaPlaca, SolidColor(if (peligro) c.peligroSuave else c.alertaSuave))
                marco(Orn.CHAFLAN, Pintura.Plana(tono).brocha(c), 16f / 18f * density)
            }
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).drawBehind {
            halo(androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width / 2, 0f); lineTo(size.width, size.height / 2); lineTo(size.width / 2, size.height); lineTo(0f, size.height / 2); close()
            }, tono.copy(alpha = 0.35f), 5.dp.toPx())
            ornamento(Orn.ENGASTE, SolidColor(tono))
        }, contentAlignment = Alignment.Center) {
            IconoGlifo(Glifo.AVISO, tamano = 18.dp, color = tono)
        }
        Column(Modifier.weight(1f)) {
            if (titulo != null) {
                Text(titulo.uppercase(), style = Tipos.etiquetaCaps, color = tono, modifier = Modifier.padding(bottom = 2.dp))
            }
            Text(texto, style = Tipos.cuerpoSm, color = c.tinta)
        }
    }
}

// ── Sigilo, llama y estado vacío ──

/** Círculo arcano en metal a media fuerza que gira una vez cada 90 s. Solo de fondo; uno por pantalla. */
@Composable
fun Sigilo(tamano: Dp, modifier: Modifier = Modifier, llama: Llama? = null, gira: Boolean = true, alfa: Float = 0.55f) {
    val c = Marca.colores
    val anima = gira && animacionesActivas()
    val angulo = if (anima) {
        rememberInfiniteTransition(label = "sigilo").animateFloat(0f, 360f, infiniteRepeatable(tween(90_000, easing = LinearEasing)), label = "giro").value
    } else 0f
    Canvas(modifier.size(tamano)) {
        llama?.let {
            drawCircle(Brush.radialGradient(listOf(it.brillo.copy(alpha = 0.5f), Color.Transparent), center, size.minDimension * 0.28f), size.minDimension * 0.28f)
        }
        rotate(angulo) {
            ornamento(Orn.SIGILO, Pintura.Metal.brocha(c), alfa = alfa)
        }
    }
}

/** Llama que parpadea, del color de [llama]. */
@Composable
fun LlamaViva(llama: Llama, tamano: Dp = 40.dp, modifier: Modifier = Modifier) {
    val anima = animacionesActivas()
    val t = if (anima) {
        rememberInfiniteTransition(label = "llama").animateFloat(0f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "t").value
    } else 0.5f
    val escala = 0.92f + 0.16f * t
    Box(
        modifier
            .size(tamano * 1.6f)
            .drawBehind {
                drawCircle(Brush.radialGradient(listOf(llama.brillo.copy(alpha = 0.45f), Color.Transparent), center, size.minDimension / 2f))
            },
        contentAlignment = Alignment.Center
    ) {
        IconoGlifo(Glifo.LLAMA, Modifier.graphicsLayer { scaleX = escala; scaleY = escala; alpha = 0.75f + 0.25f * t }, tamano = tamano, color = llama.brillo)
    }
}

/** Estado vacío: la llama dentro del sigilo, un título y una línea de lore; opcionalmente una acción. */
@Composable
fun EstadoVacio(titulo: String?, cuerpo: String?, modifier: Modifier = Modifier, llama: Llama = llamaDe(null), accion: (@Composable () -> Unit)? = null) {
    val c = Marca.colores
    Column(
        modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(bottom = 8.dp)) {
            Sigilo(132.dp)
            LlamaViva(llama)
        }
        if (titulo != null) {
            Text(titulo, style = Tipos.tituloXl.copy(fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 0.04.em), color = c.tinta, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 340.dp))
        }
        if (cuerpo != null) {
            Text(cuerpo, style = Tipos.lore, color = c.tintaSuave, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp).padding(bottom = 12.dp))
        }
        accion?.invoke()
    }
}

/** Mientras carga: una llama que late lenta dentro de un sigilo que gira, en el centro. */
@Composable
fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Sigilo(132.dp)
        LlamaViva(llamaDe(null), 32.dp)
    }
}

// ── Pestañas de la ficha (SheetTabs) ──

/** Fila de pestañas con muescas sobre un raíl de oro. La activa es de oro, más alta y brilla desde abajo. */
@Composable
fun PestanasHoja(pestanas: List<String>, activa: Int, onElegir: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Marca.colores
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val minimo = (maxWidth - 16.dp - 4.dp * (pestanas.size - 1)) / pestanas.size
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        Brush.horizontalGradient(0f to Color.Transparent, 0.12f to c.metalMedio, 0.5f to c.metalAlto, 0.88f to c.metalMedio, 1f to Color.Transparent),
                        Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx())
                    )
                }
                .horizontalScroll(rememberScrollState())
                .padding(start = 8.dp, end = 8.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            pestanas.forEachIndexed { i, t ->
                val sel = i == activa
                val interaccion = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .widthIn(min = minimo)
                        .height(if (sel) 50.dp else 42.dp)
                        .foco(interaccion)
                        .clickable(interaccion, null, role = Role.Tab) { onElegir(i) }
                        .semantics { selected = sel; role = Role.Tab }
                        .drawBehind {
                            val contorno = FormaPestana.contorno(this)
                            if (sel) {
                                drawPath(contorno, cristal(c))
                                drawPath(contorno, Brush.radialGradient(listOf(c.bruma, Color.Transparent), Offset(size.width / 2, size.height), size.width * 0.75f))
                            } else drawPath(contorno, c.vitela300)
                            marco(Orn.PESTANA, (if (sel) Pintura.Metal else Pintura.Hierro).brocha(c), density)
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        t.uppercase(),
                        style = Tipos.etiquetaCaps.copy(
                            fontSize = 10.5.sp,
                            letterSpacing = 0.08.em,
                            shadow = if (sel) Shadow(c.metalBajo, blurRadius = 20f) else null
                        ),
                        color = if (sel) c.oro else c.tintaSuave,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ── Cabecera de pantalla ──

/**
 * Cabecera de las pantallas: botón de volver o cerrar a la izquierda, el contenido en medio y acciones a la
 * derecha, sobre un degradado del cristal que se funde con la noche.
 */
@Composable
fun Cabecera(
    modifier: Modifier = Modifier,
    navegacion: (@Composable () -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
    contenido: @Composable RowScope.() -> Unit
) {
    val c = Marca.colores
    Row(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(c.vitela100, Color.Transparent)))
            .statusBarsPadding()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (navegacion != null) navegacion() else Spacer(Modifier.size(12.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), content = contenido)
        acciones()
    }
}

/** Título de pantalla en la cabecera. */
@Composable
fun TituloPantalla(titulo: String, subtitulo: String? = null) {
    val c = Marca.colores
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            titulo,
            style = Tipos.tituloLg.copy(fontSize = 22.sp, lineHeight = 28.sp),
            color = c.tinta,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        if (subtitulo != null) Text(subtitulo, style = Tipos.uiSm, color = c.tintaSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Contenedor que pinta el fondo de la noche y deja el contenido encima. */
@Composable
fun Noche(modifier: Modifier = Modifier, contenido: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().fondoNoche(Marca.colores), content = contenido)
}
