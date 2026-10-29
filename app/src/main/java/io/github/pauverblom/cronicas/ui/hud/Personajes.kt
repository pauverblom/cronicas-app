package io.github.pauverblom.cronicas.ui.hud

import androidx.compose.material3.Icon
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.pauverblom.cronicas.ui.common.Llama
import io.github.pauverblom.cronicas.ui.common.animacionesActivas
import io.github.pauverblom.cronicas.ui.theme.Cinzel
import io.github.pauverblom.cronicas.ui.theme.CinzelDecorativa
import io.github.pauverblom.cronicas.ui.theme.ColoresFijos
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos

// ── Retrato (Portrait) ──

/**
 * El retrato redondo del personaje dentro del doble anillo tachonado. Sin foto, la inicial en Cinzel Decorative
 * brilla sobre el núcleo del fuego de su clase. Con [anillo], el aro arde con ese fuego.
 */
@Composable
fun Retrato(modelo: Any?, nombre: String, llama: Llama, tamano: Dp, modifier: Modifier = Modifier, anillo: Boolean = false) {
    val c = Marca.colores
    val fase = if (anillo) rememberFaseFuego() else null
    // El aro que arde va en su propia capa (con sitio para el halo): su animación solo repinta el retrato,
    // no la ventana que lo contiene
    val conCapa = if (anillo) Modifier.capa(lado = 14.dp, arriba = 14.dp, abajo = 14.dp, siempre = true) else Modifier
    Box(
        modifier
            .then(conCapa)
            .size(tamano)
            .drawWithContent {
                drawCircle(Brush.radialGradient(0f to llama.nucleo, 1.2f to c.vitela300, center = Offset(size.width / 2, size.height * 0.4f), radius = size.minDimension * 0.6f))
                drawContent()
                val m = size.width * 0.04f
                val destino = Rect(Offset(-m, -m), Size(size.width + 2 * m, size.height + 2 * m))
                if (anillo) halo(Path().apply { addOval(destino) }, llama.brillo.copy(alpha = 0.6f), 10.dp.toPx())
                // La fase se lee aquí, al pintar: el fuego corre sin recomponer el retrato
                ornamento(Orn.ANILLO, (if (fase != null) Pintura.Fuego(llama, fase.value) else Pintura.Metal).brocha(c), destino)
            },
        contentAlignment = Alignment.Center
    ) {
        if (modelo != null) {
            AsyncImage(
                model = modelo,
                contentDescription = "Retrato de $nombre",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(0.84f).clip(CircleShape)
            )
        } else {
            Text(
                nombre.trim().firstOrNull()?.uppercase() ?: "?",
                style = TextStyle(
                    fontFamily = CinzelDecorativa,
                    fontWeight = FontWeight.Bold,
                    fontSize = (tamano.value * 0.4f).sp,
                    lineHeight = (tamano.value * 0.4f).sp,
                    shadow = Shadow(llama.brillo, blurRadius = 16f)
                ),
                color = llama.chispa,
                modifier = Modifier.semantics { contentDescription = nombre }
            )
        }
    }
}

// ── Entrada de la lista de personajes (RosterEntry) ──

/** Un personaje en la lista: placa escalonada con su retrato, el nombre, «Clase nivel · Especie» y sus PG. */
@Composable
fun EntradaLista(
    nombre: String,
    retrato: Any?,
    linea: String,
    pg: Int?,
    pgMax: Int?,
    llama: Llama,
    onClick: () -> Unit,
    onClickLargo: () -> Unit,
    modifier: Modifier = Modifier,
    /** Los tres puntos que abren el menú de la entrada (el mismo que mantener pulsado). */
    onMenu: (() -> Unit)? = null,
    /** Con una clave, el retrato y el nombre vuelan a la pantalla que se abre (ver [compartido]). */
    clave: String? = null
) {
    val c = Marca.colores
    Placa(modifier.fillMaxWidth(), pintura = Pintura.Metal, onClick = onClick, onClickLargo = onClickLargo) {
        Row(Modifier.padding(start = 24.dp, end = if (onMenu != null) 8.dp else 24.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Retrato(retrato, nombre, llama, 56.dp, if (clave != null) Modifier.compartido("retrato-$clave") else Modifier)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    nombre, style = TextStyle(fontFamily = Cinzel, fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.03.em),
                    color = c.tinta, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = if (clave != null) Modifier.textoCompartido("nombre-$clave") else Modifier
                )
                Text(linea, style = Tipos.uiSm, color = c.tintaSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (pg != null && pgMax != null) {
                    val color = colorEstadoPg(c, pg, pgMax)
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Medidor(if (pgMax > 0) pg.toFloat() / pgMax else 0f, SolidColor(color), Modifier.weight(1f), pequeno = true)
                        Text("PG $pg/$pgMax", style = TextStyle(fontFamily = Cinzel, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em), color = color, maxLines = 1)
                    }
                }
            }
            if (onMenu != null) {
                BotonIcono(onMenu, "Más acciones") { Icon(Icons.Sharp.MoreVert, null, Modifier.size(24.dp)) }
            }
        }
    }
}

// ── Tarjeta de elección (ChoiceCard) ──

/**
 * Marco de una tarjeta del carrusel: una ventana coronada por la cresta. Elegida, se enciende: el fuego de la clase
 * recorre el marco, la cresta toma el color de la chispa, la tarjeta respira un halo y se eleva.
 */
@Composable
fun Modifier.marcoEleccion(elegida: Boolean, llama: Llama?): Modifier {
    val c = Marca.colores
    val anima = animacionesActivas()
    val fase = if (elegida && llama != null) rememberFaseFuego() else null
    val respiro = if (elegida && llama != null && anima) {
        rememberInfiniteTransition(label = "respiro").animateFloat(10f, 22f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "halo")
    } else null
    val elevacion by animateFloatAsState(
        if (elegida) -4f else 0f,
        if (anima) spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow) else tween(0),
        label = "elevar"
    )
    return this
        .graphicsLayer { translationY = elevacion * density }
        .drawWithContent {
            // Fase y respiro se leen al pintar, no al componer
            val pintura = if (elegida && llama != null) Pintura.Fuego(llama, fase?.value ?: 0f) else Pintura.Metal
            if (elegida && llama != null) halo(FormaVentana.contorno(this), llama.brillo.copy(alpha = 0.55f), (respiro?.value ?: 14f).dp.toPx())
            interior(c, FormaVentana, cristal(c), TipoTextura.VENTANA, sombra = true)
            val brocha = pintura.brocha(c)
            marco(Orn.PANEL, brocha, density)
            kitVentana(brocha)
            drawContent()
            val cresta = if (elegida && llama != null) SolidColor(llama.chispa) else Pintura.Metal.brocha(c)
            ornamento(Orn.CRESTA, cresta, Rect(Offset(size.width / 2 - 40.dp.toPx(), -13.dp.toPx()), Size(80.dp.toPx(), 28.dp.toPx())))
        }
}

/**
 * Ilustración del reglamento en una ventanita escalonada de papel cálido (son tinta sobre blanco y se multiplican).
 * Sin imagen y con [archivoFaltante], el aviso deliberado «MISSING PICTURE» con la ruta que falta; sin nada,
 * una «?» grande (opciones personalizadas).
 */
@Composable
fun VentanaArte(uri: String?, nombre: String, alto: Dp, modifier: Modifier = Modifier, archivoFaltante: String? = null, medir: Boolean = false) {
    val c = Marca.colores
    val forma = formaCortada(10.dp)
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .padding(top = 8.dp)
            .height(alto)
            .clip(forma)
            .background(ColoresFijos.papelArte)
            .drawWithContent {
                drawContent()
                val contorno = forma.contorno(this)
                sombraInterior(contorno, c.metalBajo.copy(alpha = 0.5f), 9.dp.toPx())
                drawPath(contorno, c.metalBajo, style = Stroke(2.dp.toPx()))
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            medir -> Unit
            uri != null -> AsyncImage(
                model = uri,
                contentDescription = nombre,
                contentScale = ContentScale.Fit,
                // Multiplicar sobre el papel: el blanco de la ilustración desaparece y queda la tinta
                colorFilter = ColorFilter.tint(ColoresFijos.papelArte, BlendMode.Multiply),
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
            archivoFaltante != null -> Column(
                Modifier
                    .padding(12.dp)
                    .drawBehind {
                        drawRect(ColoresFijos.arteFaltante, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
                    }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("MISSING PICTURE", style = TextStyle(fontFamily = Cinzel, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.14.em), color = ColoresFijos.arteFaltante)
                Text("assets/ilustraciones/$archivoFaltante", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp), color = ColoresFijos.tintaArte, textAlign = TextAlign.Center)
            }
            else -> Text("?", style = TextStyle(fontFamily = CinzelDecorativa, fontSize = 96.sp, fontWeight = FontWeight.Bold), color = ColoresFijos.tintaArte)
        }
    }
}

/** Nombre centrado de una tarjeta, sobre un separador pequeño. */
@Composable
fun NombreTarjeta(nombre: String, detalle: String? = null) {
    val c = Marca.colores
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(nombre, style = Tipos.tituloLg.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.04.em), color = c.tinta, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        if (detalle != null) Text(detalle, style = TextStyle(fontFamily = Cinzel, fontSize = 15.sp, fontWeight = FontWeight.Bold), color = c.oro, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Separador(compacto = true)
    }
}

/** Descripción de lore de una tarjeta, en cursiva centrada. */
@Composable
fun DescripcionTarjeta(texto: String) {
    Text(texto, style = Tipos.lore.copy(fontSize = 15.sp, lineHeight = 22.sp), color = Marca.colores.tintaSuave, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

/** Lista de datos de una tarjeta: etiqueta en versalitas de oro y valor. */
@Composable
fun DatosTarjeta(datos: List<Pair<String, String>>) {
    val c = Marca.colores
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        datos.forEach { (k, v) ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Las etiquetas largas («Característica principal») bajan de línea antes que estrujar el valor
                Text(k.uppercase(), style = TextStyle(fontFamily = Cinzel, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em), color = c.oro, modifier = Modifier.padding(top = 1.dp).widthIn(max = 120.dp))
                Text(v, style = Tipos.uiSm.copy(fontSize = 13.sp, lineHeight = 18.sp), color = c.tinta, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ── Mecha del asistente (WizardFuse) ──

/** El progreso del asistente como una mecha que arde: lo quemado brilla con el fuego de la clase y la chispa parpadea. */
@Composable
fun MechaPasos(paso: Int, total: Int, titulo: String, llama: Llama, modifier: Modifier = Modifier) {
    val c = Marca.colores
    val anima = animacionesActivas()
    val avance by animateFloatAsState((paso.toFloat() / total).coerceIn(0f, 1f), tween(if (anima) 600 else 0), label = "mecha")
    val chispa = if (anima) {
        rememberInfiniteTransition(label = "chispa").animateFloat(0.75f, 1.1f, infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "parpadeo")
    } else null
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Paso $paso de $total: ", style = Tipos.uiSm, color = c.tintaSuave)
            Text(titulo, style = TextStyle(fontFamily = Cinzel, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em), color = c.tinta)
        }
        Canvas(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(16.dp).semantics { contentDescription = "Paso $paso de $total" }) {
            val d = density
            val y = size.height / 2
            val cuerda = Rect(0f, y - 3 * d, size.width, y + 3 * d)
            // Cuerda trenzada: rayas de hierro sobre el pozo
            drawRect(c.vitela300, cuerda.topLeft, cuerda.size)
            clipRect(cuerda.left, cuerda.top, cuerda.right, cuerda.bottom) {
                var x = -cuerda.height
                while (x < size.width) {
                    drawLine(c.hierro, Offset(x, cuerda.bottom), Offset(x + cuerda.height * 0.47f, cuerda.top), 4 * d * 0.9f)
                    x += 7 * d
                }
            }
            val punta = size.width * avance
            drawRect(Brush.horizontalGradient(listOf(llama.nucleo, llama.brillo), 0f, punta.coerceAtLeast(1f)), cuerda.topLeft, Size(punta, cuerda.height))
            val metal = Pintura.Metal.brocha(c)
            val capa = Rect(Offset(-18 * d, y - 8 * d), Size(18 * d, 16 * d))
            ornamento(Orn.PUNTA, metal, capa)
            scale(-1f, 1f, Offset(size.width / 2, y)) { ornamento(Orn.PUNTA, metal, capa) }
            if (avance < 1f) {
                val chispa = chispa?.value ?: 1f
                drawCircle(Brush.radialGradient(listOf(llama.brillo.copy(alpha = 0.7f), Color.Transparent), Offset(punta, y), 16 * d * chispa), 16 * d * chispa, Offset(punta, y))
                drawCircle(llama.chispa, 6 * d * chispa, Offset(punta, y))
            }
        }
    }
}

// ── Sello de lacre (WaxSeal) ──

private val GotaLacre: Path by lazy {
    PathParser().parsePathString(
        "M50 3c7 0 9 5 15 6s11-2 15 3 1 10 4 15 9 7 9 13-6 8-6 14 4 11 0 16-10 3-14 7-5 10-11 11-9-4-14-4-9 6-15 3-5-8-10-11-11-2-14-7 2-10 0-15-8-8-7-14 7-7 9-12-1-11 4-15 10 0 15-3S43 3 50 3z"
    ).toPath()
}

/** Sello de lacre con la inicial del personaje: cae sobre la forja. */
@Composable
fun SelloLacre(letra: String, modifier: Modifier = Modifier, tamano: Dp = 96.dp) {
    val c = Marca.colores
    Box(
        modifier
            .size(tamano)
            .drawBehind {
                val k = size.minDimension / 100f
                translate(0f, 4 * density) { scale(k, k, Offset.Zero) { drawPath(GotaLacre, c.velo) } }
                scale(k, k, Offset.Zero) {
                    drawPath(GotaLacre, c.sangreRelleno)
                    drawCircle(c.sobreSangre.copy(alpha = 0.35f), 32f, Offset(50f, 50f), style = Stroke(2f))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            letra.trim().firstOrNull()?.uppercase() ?: "?",
            style = TextStyle(fontFamily = CinzelDecorativa, fontWeight = FontWeight.Bold, fontSize = (tamano.value * 0.42f).sp, lineHeight = (tamano.value * 0.42f).sp),
            color = c.sobreSangre
        )
    }
}

/** Botón flotante del HUD: un medallón grande con anillo de oro. */
@Composable
fun MedallonFlotante(glifo: Glifo, descripcion: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) { Medallon(glifo, descripcion, true, onClick, 64.dp) }
}

