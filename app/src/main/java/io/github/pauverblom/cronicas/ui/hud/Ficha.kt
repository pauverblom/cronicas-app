package io.github.pauverblom.cronicas.ui.hud

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.pauverblom.cronicas.rules.Atributo
import io.github.pauverblom.cronicas.ui.theme.AlegreyaSans
import io.github.pauverblom.cronicas.ui.theme.Cinzel
import io.github.pauverblom.cronicas.ui.theme.ColoresFijos
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Paleta
import io.github.pauverblom.cronicas.ui.theme.Tipos

// ── Formato ──

/** Números con signo tipográfico: +2, −1, +0. */
fun conSigno(n: Int): String = when {
    n > 0 -> "+$n"
    n < 0 -> "−${-n}"
    else -> "+0"
}

/** Millares a la española a partir de cinco cifras: 2500, 12.500. */
fun miles(n: Int): String {
    if (kotlin.math.abs(n) < 10_000) return n.toString()
    // Separador de miles con punto, como en español: 12345 → «12.345»
    val cifras = kotlin.math.abs(n.toLong()).toString().reversed().chunked(3).joinToString(".").reversed()
    return if (n < 0) "-$cifras" else cifras
}

/** Color de los PG según su estado: sano > 50 %, herido ≤ 50 %, crítico ≤ 25 %, ninguno a 0. */
fun colorEstadoPg(c: Paleta, actuales: Int, maximos: Int): Color {
    if (maximos <= 0 || actuales <= 0) return c.pgNinguno
    val r = actuales.toFloat() / maximos
    return when {
        r <= 0.25f -> c.pgCritico
        r <= 0.5f -> c.pgHerido
        else -> c.pgSano
    }
}

private fun brillo(color: Color, radio: Float) = Shadow(color, Offset.Zero, radio)

// ── Marca de valor fijado a mano ──

/** El lápiz violeta: este número se ha fijado a mano, no se calcula. */
@Composable
fun MarcaFijada(tamano: Dp = 14.dp, modifier: Modifier = Modifier) {
    val c = Marca.colores
    Box(
        modifier
            .padding(start = 4.dp)
            .offset(y = (-6).dp)
            .drawBehind { drawCircle(Brush.radialGradient(listOf(c.violetaArcano.copy(alpha = 0.35f), Color.Transparent))) }
    ) { IconoGlifo(Glifo.LAPIZ, tamano = tamano, color = c.violetaArcano, descripcion = "Fijado a mano") }
}

// ── Medidor (Gauge) ──

/** Rayado diagonal (daño atenuado que volverá). */
private fun DrawScope.rayado(color: Color, rect: Rect, paso: Float) {
    clipPath(Path().apply { addRect(rect) }) {
        var x = rect.left - rect.height
        while (x < rect.right) {
            drawLine(color, Offset(x, rect.bottom), Offset(x + rect.height, rect.top), paso / 2f)
            x += paso
        }
    }
}

/**
 * La barra del HUD: una cápsula de metal en punta con remates de lanza, un relleno de cristal y rombos en el marco
 * que marcan umbrales. El color lleva el estado, los rombos la regla y la longitud la cantidad.
 */
@Composable
fun Medidor(
    valor: Float,
    color: Brush,
    modifier: Modifier = Modifier,
    extra: Float = 0f,
    marcas: List<Float> = emptyList(),
    pequeno: Boolean = false,
    descripcion: String? = null
) {
    val c = Marca.colores
    val v by animateFloatAsState(valor.coerceIn(0f, 1f), tween(400), label = "medidor")
    val ext = extra.coerceIn(0f, 1f - v)
    Canvas(
        modifier
            .fillMaxWidth()
            .padding(horizontal = if (pequeno) 0.dp else 16.dp)
            .height(if (pequeno) 10.dp else 16.dp)
            .semantics { if (descripcion != null) { contentDescription = descripcion; stateDescription = "${(valor * 100).toInt()} %" } }
    ) {
        val d = density
        val m = (if (pequeno) 2f else 3f) * d
        val punta = (if (pequeno) 3f else 5f) * d
        val pista = Rect(m, m, size.width - m, size.height - m)
        val forma = Path().apply {
            moveTo(pista.left + punta, pista.top); lineTo(pista.right - punta, pista.top); lineTo(pista.right, pista.center.y)
            lineTo(pista.right - punta, pista.bottom); lineTo(pista.left + punta, pista.bottom); lineTo(pista.left, pista.center.y); close()
        }
        clipPath(forma) {
            drawRect(c.vitela300, pista.topLeft, pista.size)
            val ancho = pista.width * v
            drawRect(color, pista.topLeft, Size(ancho, pista.height))
            drawRect(Brush.verticalGradient(listOf(c.brilloCristal, Color.Transparent), pista.top, pista.top + pista.height * 0.45f), pista.topLeft, Size(ancho, pista.height * 0.45f))
            if (ext > 0f) rayado(c.pgNinguno, Rect(pista.left + ancho, pista.top, pista.left + ancho + pista.width * ext, pista.bottom), 6f * d)
        }
        val metal = Pintura.Metal.brocha(c)
        marco(Orn.BARRA, metal, (if (pequeno) 5f else 8f) / 12f * d)
        if (!pequeno) {
            val alto = 16f * d
            ornamento(Orn.PUNTA, metal, Rect(Offset(-16f * d, size.height / 2 - alto / 2), Size(18f * d, alto)))
            scale(-1f, 1f, Offset(size.width / 2, size.height / 2)) {
                ornamento(Orn.PUNTA, metal, Rect(Offset(-16f * d, size.height / 2 - alto / 2), Size(18f * d, alto)))
            }
        }
        for (t in marcas) {
            val x = pista.left + pista.width * t
            val r = 4f * d
            val rombo = Path().apply { moveTo(x, size.height / 2 - r); lineTo(x + r, size.height / 2); lineTo(x, size.height / 2 + r); lineTo(x - r, size.height / 2); close() }
            drawPath(rombo, c.vitela300, style = androidx.compose.ui.graphics.drawscope.Stroke(3f * d))
            drawPath(rombo, rellenoOro(c))
        }
    }
}

/** El oro de los marcos como relleno de barra (la de PX). */
@Composable
fun brochaMetal(): Brush {
    val c = Marca.colores
    return remember(c) { Brush.horizontalGradient(0f to c.metalAlto, 0.2f to c.metalMedio, 0.36f to c.metalBajo, 0.5f to c.metalMedio, 0.64f to c.metalAlto, 0.8f to c.metalMedio, 1f to c.metalBajo) }
}

// ── Orbe de PG (HPMeter) ──

/**
 * El corazón de la ficha: un orbe en un aro de oro tachonado que se llena del color del estado de los PG, con una
 * banda rayada para el daño atenuado que volverá. −1 y +1 son medallones a los lados; a 0 PG aparece el aviso.
 */
@Composable
fun OrbePg(
    actuales: Int,
    maximos: Int,
    atenuado: Int,
    maxFijado: Boolean,
    onMenos: () -> Unit,
    onMas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Marca.colores
    val estado by animateColorAsState(colorEstadoPg(c, actuales, maximos), tween(400), label = "pg")
    val pct = if (maximos > 0) (actuales.toFloat() / maximos).coerceIn(0f, 1f) else 0f
    val nivel by animateFloatAsState(pct, tween(500), label = "liquido")
    val ext = if (maximos > 0) (atenuado.toFloat() / maximos).coerceIn(0f, 1f - pct) else 0f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Medallon(Glifo.MENOS, "Quitar 1 PG", actuales > 0, onMenos)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(168.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "Puntos de golpe: $actuales de $maximos" }
                        .drawBehind {
                            val r = size.minDimension * (1f - 0.23f) / 2f
                            val vaso = Path().apply { addOval(Rect(center, r)) }
                            clipPath(vaso) {
                                drawCircle(Brush.radialGradient(0f to c.vitela200, 0.72f to c.vitela300, center = Offset(center.x, size.height * 0.3f), radius = r * 1.4f), r)
                                val arriba = center.y + r - 2 * r * nivel
                                drawRect(Brush.verticalGradient(listOf(estado, lerpColor(estado, c.vitela300, 0.6f)), arriba, center.y + r), Offset(0f, arriba), Size(size.width, center.y + r - arriba), alpha = 0.42f)
                                if (nivel > 0f) {
                                    drawRect(estado.copy(alpha = 0.35f), Offset(0f, arriba - 4.dp.toPx()), Size(size.width, 8.dp.toPx()))
                                    drawRect(estado, Offset(0f, arriba - 1.dp.toPx()), Size(size.width, 2.dp.toPx()))
                                }
                                if (ext > 0f) {
                                    val alto = 2 * r * ext
                                    rayado(c.pgNinguno.copy(alpha = 0.7f), Rect(0f, arriba - alto, size.width, arriba), 7.dp.toPx())
                                }
                            }
                            // Sombra interior del vaso redondo: un solo degradado radial
                            drawCircle(Brush.radialGradient(0.8f to Color.Transparent, 1f to Color.Black.copy(alpha = if (c.oscuro) 0.6f else 0.12f), center = center, radius = r), r)
                            ornamento(Orn.ORBE, Pintura.Metal.brocha(c))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$actuales",
                            style = Tipos.cifraXl.copy(fontSize = 58.sp, lineHeight = 58.sp, shadow = brillo(estado, 36f)),
                            color = estado
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("/ $maximos", style = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp, shadow = brillo(c.noche, 6f)), color = c.tintaSuave)
                            if (maxFijado) MarcaFijada()
                        }
                    }
                }
            }
            Medallon(Glifo.MAS, "Sumar 1 PG", actuales < maximos, onMas)
        }
        if (atenuado > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    buildAnnotatedString {
                        append("Daño atenuado: ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$atenuado") }
                    },
                    style = Tipos.uiSm, color = c.tinta
                )
                Text("Recupera 1 PG por minuto", style = Tipos.uiSm, color = c.tintaSuave)
            }
        }
        if (actuales <= 0) {
            val aviso = if (atenuado > 0) "Inconsciente por daño atenuado: no se desangra ni muere, y recupera 1 PG por minuto."
            else "Inconsciente. De -1 a -9 pierde 1 PG por asalto; a -10 muere."
            Text(
                aviso,
                style = Tipos.cuerpoSm,
                color = c.tinta,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val forma = formaCortada(8.dp).contorno(this)
                        drawPath(forma, c.peligroSuave)
                        drawPath(forma, c.sangre, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx() * 2))
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

/** Botón redondo en un anillo tachonado (los −1/+1 de los PG). */
@Composable
fun Medallon(glifo: Glifo, descripcion: String, activo: Boolean, onClick: () -> Unit, tamano: Dp = 56.dp) {
    val c = Marca.colores
    val interaccion = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(tamano)
            .foco(interaccion)
            .clickable(interaccion, null, enabled = activo, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descripcion }
            .drawBehind {
                drawCircle(Brush.radialGradient(0f to c.vitela200, 0.7f to c.vitela300, center = center, radius = size.minDimension / 2f), size.minDimension / 2f * 0.9f)
                ornamento(Orn.ANILLO, (if (activo) Pintura.Metal else Pintura.Hierro).brocha(c))
            },
        contentAlignment = Alignment.Center
    ) { IconoGlifo(glifo, tamano = 24.dp, color = if (activo) c.oro else c.tintaTenue) }
}

// ── Insignia (StatTile) ──

/** Insignia de corte de gema para una cifra de combate (CA, BA, DG, MOV). Siempre se puede tocar. */
@Composable
fun Insignia(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    fijado: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val c = Marca.colores
    val interaccion = remember { MutableInteractionSource() }
    BoxWithConstraints(
        modifier
            .widthIn(max = 84.dp)
            .aspectRatio(84f / 108f)
            .capa(lado = 4.dp, arriba = 4.dp, abajo = 4.dp)
            .then(if (onClick != null) Modifier.foco(interaccion).clickable(interaccion, null, role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}
            .drawBehind {
                interior(c, FormaInsignia, cristal(c), TipoTextura.PLACA, sombra = true)
                ornamento(Orn.INSIGNIA, Pintura.Metal.brocha(c))
            }
            .padding(horizontal = 4.dp)
    ) {
        // Unidades del marco (84 × 108): la etiqueta va encima del separador (y = 33) y la cifra centrada debajo.
        // Se colocan por la línea base, no por la caja del texto, que cambia con las métricas de la fuente
        // de cada plataforma
        val u = maxHeight / 108f
        Row(Modifier.align(Alignment.TopCenter).padding(top = u * 13).height(u * 16), verticalAlignment = Alignment.CenterVertically) {
            Text(etiqueta, style = Tipos.etiquetaCaps, color = c.oro, maxLines = 1)
            if (fijado) MarcaFijada(12.dp, Modifier.offset(y = 4.dp))
        }
        Text(
            valor,
            style = Tipos.cifraLg.copy(
                fontSize = if (valor.length > 4) 19.sp else 27.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.01).em,
                shadow = brillo(c.metalBajo, 24f)
            ),
            color = c.tinta,
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopCenter).paddingFromBaseline(top = u * 69)
        )
        if (sub != null) {
            Text(
                sub, style = Tipos.caption.copy(fontSize = 11.sp, lineHeight = 14.sp), color = c.tintaSuave, maxLines = 1,
                textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.TopCenter).paddingFromBaseline(top = u * 85)
            )
        }
    }
}

// ── Escudo de característica (AbilityScore) ──

/**
 * Una característica como escudo heráldico: gema de su color en el borde de arriba, abreviatura, el modificador
 * grande y brillante y la puntuación debajo (la tirada entre paréntesis si la especie la cambia). Las primarias
 * llevan marco de oro y estrella.
 */
@Composable
fun EscudoCaracteristica(
    atributo: Atributo,
    puntuacion: Int,
    base: Int?,
    mod: Int,
    primaria: Boolean,
    fijado: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val c = Marca.colores
    val ab = c.atributo(atributo)
    val interaccion = remember { MutableInteractionSource() }
    Box(
        modifier
            .widthIn(max = 96.dp)
            .aspectRatio(96f / 120f)
            .then(if (onClick != null) Modifier.foco(interaccion).clickable(interaccion, null, role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}
            .drawBehind {
                interior(c, FormaEscudo, cristal(c), TipoTextura.PLACA, sombra = true)
                ornamento(Orn.ESCUDO, (if (primaria) Pintura.Metal else Pintura.Hierro).brocha(c))
            }
    ) {
        Gema(ab, 10.dp, Modifier.align(Alignment.TopCenter).offset(y = (-4).dp))
        // Unidades del marco (96 × 120): el nombre arriba, el modificador y la puntuación centrados sobre la punta.
        // La cifra y la puntuación se colocan por la línea base (ver Insignia)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val u = maxWidth / 96f
            // El nombre entero («CONSTITUCIÓN»), encogido para caber en el escudo
            Row(
                Modifier.align(Alignment.TopCenter).padding(top = u * 18).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TextoAjustado(
                    atributo.nombre.uppercase(), atributo.abrev,
                    Tipos.etiquetaCaps.copy(letterSpacing = 0.08.em), ab,
                    Modifier.weight(1f, fill = false), minimo = 7.5.sp
                )
                if (primaria) IconoGlifo(Glifo.ESTRELLA, tamano = 12.dp, color = c.oro, descripcion = "Primaria")
            }
            Row(Modifier.align(Alignment.TopCenter).paddingFromBaseline(top = u * 66), verticalAlignment = Alignment.CenterVertically) {
                Text(conSigno(mod), style = Tipos.cifraLg.copy(fontSize = 32.sp, lineHeight = 38.sp, shadow = brillo(ab, 28f)), color = ab)
                if (fijado) MarcaFijada(12.dp)
            }
            Text(
                buildAnnotatedString {
                    append("$puntuacion")
                    if (base != null && base != puntuacion) withStyle(SpanStyle(fontFamily = AlegreyaSans, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = c.tintaSuave)) { append(" ($base)") }
                },
                style = TextStyle(fontFamily = Cinzel, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
                color = c.tinta,
                modifier = Modifier.align(Alignment.TopCenter).paddingFromBaseline(top = u * 89)
            )
        }
    }
}

/** Gema tallada en rombo del color de una característica, con su brillo. */
@Composable
fun Gema(color: Color, tamano: Dp, modifier: Modifier = Modifier) {
    val c = Marca.colores
    Canvas(modifier.size(tamano).rotate(45f)) {
        drawCircle(color.copy(alpha = 0.35f), size.minDimension)
        drawRect(Brush.radialGradient(0f to c.metalAlto, 0.45f to color, 1f to color, center = Offset(size.width * 0.35f, size.height * 0.35f), radius = size.width))
    }
}

// ── Fila de salvación (SaveRow) ──

/** Una fila de «Salvaciones y tiradas»: la gema en su engaste, la abreviatura, el bonificador y el ND en una placa. */
@Composable
fun FilaSalvacion(atributo: Atributo, bonus: Int, nd: Int, primaria: Boolean, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = Marca.colores
    val ab = c.atributo(atributo)
    val pintura = if (primaria) Pintura.Metal else Pintura.Hierro
    val interaccion = remember { MutableInteractionSource() }
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .then(if (onClick != null) Modifier.foco(interaccion).clickable(interaccion, null, role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}
            .drawBehind {
                // El engaste: un rombo hundido con la gema dentro
                val e = 26.dp.toPx()
                val cx = 22.dp.toPx()
                val engaste = Path().apply { moveTo(cx, size.height / 2 - e * 0.707f); lineTo(cx + e * 0.707f, size.height / 2); lineTo(cx, size.height / 2 + e * 0.707f); lineTo(cx - e * 0.707f, size.height / 2); close() }
                drawPath(engaste, c.vitela300)
                sombraInterior(engaste, Color.Black.copy(alpha = 0.5f), 6.dp.toPx())
                val contorno = FormaFila.contorno(this)
                drawPath(contorno, cristal(c))
                texturaGrabada(c, TipoTextura.PLACA, contorno)
                marco(Orn.FILA, pintura.brocha(c), density)
            }
    ) {
        Gema(ab, 11.dp, Modifier.align(Alignment.CenterStart).offset(x = 16.5.dp))
        Row(
            Modifier.fillMaxSize().padding(start = 54.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // El nombre entero de la característica, encogido si hace falta; la abreviatura solo si no cabe
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextoAjustado(
                    atributo.nombre.uppercase(), atributo.abrev,
                    Tipos.etiquetaCaps.copy(fontSize = 13.sp, letterSpacing = 0.1.em), ab,
                    Modifier.weight(1f, fill = false), minimo = 9.sp
                )
                if (primaria) IconoGlifo(Glifo.ESTRELLA, tamano = 12.dp, color = c.oro, descripcion = "Primaria")
            }
            Text(
                conSigno(bonus),
                style = Tipos.cifraMd.copy(fontSize = 19.sp, lineHeight = 24.sp, shadow = brillo(c.metalBajo, 20f)),
                color = c.tinta,
                textAlign = TextAlign.End,
                modifier = Modifier.width(36.dp)
            )
            val k = 26f / 48f
            Box(
                Modifier
                    .height(26.dp)
                    .drawBehind {
                        interior(c, formaPlaca(k), SolidColor(c.vitela300))
                        marco(Orn.PLACA, pintura.brocha(c), k * density)
                    }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("ND $nd", style = TextStyle(fontFamily = Cinzel, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em), color = if (primaria) c.oro else c.tintaSuave)
            }
        }
    }
}

// ── Ataque (AttackRow) ──

/** Un modo de uso de un arma: «Cuerpo a cuerpo», «Distancia»… con su BA y su daño. */
data class ModoAtaque(val etiqueta: String, val ba: Int, val dano: String)

/** Un arma de «Ataques» en su propia placa: oro mientras está en mano, hierro si no. */
@Composable
fun PlacaAtaque(nombre: String, enMano: Boolean, modos: List<ModoAtaque>, notas: String?, aviso: String?, modifier: Modifier = Modifier) {
    val c = Marca.colores
    Placa(modifier.fillMaxWidth(), pintura = if (enMano) Pintura.Metal else Pintura.Hierro) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(nombre, style = TextStyle(fontFamily = Cinzel, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.04.em), color = c.tinta, modifier = Modifier.weight(1f))
                if (enMano) Etiqueta("EN MANO", tono = Tono.ORO, insignia = true)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                modos.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(m.etiqueta, style = Tipos.cuerpoSm, color = c.tintaSuave, modifier = Modifier.weight(1f))
                        Hueco(64.dp, Pintura.Metal) {
                            Text("BA", style = TextStyle(fontFamily = Cinzel, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.12.em), color = c.tintaSuave)
                            Text(conSigno(m.ba), style = TextStyle(fontFamily = Cinzel, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold), color = c.oro)
                        }
                        Hueco(76.dp, Pintura.Hierro) {
                            Text(m.dano, style = TextStyle(fontFamily = Cinzel, fontSize = if (m.dano.length > 6) 14.sp else 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold), color = c.tinta, maxLines = 1)
                        }
                    }
                }
            }
            if (!notas.isNullOrBlank()) Text(notas, style = Tipos.caption, color = c.tintaSuave, modifier = Modifier.padding(top = 8.dp))
            if (aviso != null) {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconoGlifo(Glifo.AVISO, tamano = 14.dp, color = c.alerta)
                    Text(aviso, style = Tipos.caption.copy(fontWeight = FontWeight.Bold), color = c.alerta)
                }
            }
        }
    }
}

/** Engaste pequeño (el tercer nivel del HUD): BA y daño en las placas de ataque. */
@Composable
private fun Hueco(ancho: Dp, pintura: Pintura, contenido: @Composable () -> Unit) {
    val c = Marca.colores
    Row(
        Modifier
            .width(ancho)
            .height(32.dp)
            .drawBehind {
                interior(c, FormaEngaste, SolidColor(c.vitela300))
                marco(Orn.CHAFLAN, pintura.brocha(c), 0.5f * density)
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) { contenido() }
}

// ── Experiencia (XPBar) ──

@Composable
fun BarraPx(px: Int, desde: Int, siguiente: Int?, nivel: Int, modifier: Modifier = Modifier) {
    val c = Marca.colores
    val pct = siguiente?.let { if (it > desde) (px - desde).toFloat() / (it - desde) else 1f } ?: 1f
    val listo = siguiente != null && px >= siguiente
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(miles(px), style = Tipos.cifraLg.copy(fontSize = 32.sp), color = c.tinta)
            Text(
                siguiente?.let { "/ ${miles(it)} PX para nivel ${nivel + 1}" } ?: "PX",
                style = Tipos.uiSm, color = c.tintaSuave, modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Box(Modifier.drawBehind { if (listo) halo(Path().apply { addRect(Rect(Offset(16.dp.toPx(), 0f), Size(size.width - 32.dp.toPx(), size.height))) }, c.metalMedio.copy(alpha = 0.6f), 8.dp.toPx()) }) {
            Medidor(pct, brochaMetal(), descripcion = "Experiencia")
        }
        if (listo) {
            Text(
                "¡Tienes PX suficientes para subir de nivel!",
                style = TextStyle(fontFamily = Cinzel, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.04.em, shadow = brillo(c.metalBajo, 20f)),
                color = c.oro
            )
        }
    }
}

// ── Carga (CargaBar) ──

enum class BandaCarga { LIGERA, MEDIA, SOBRECARGADO }

@Composable
fun BarraCarga(total: String, limite: String, fraccion: Float, banda: BandaCarga, modifier: Modifier = Modifier, monedas: String? = null) {
    val c = Marca.colores
    val color = when (banda) {
        BandaCarga.LIGERA -> c.pgSano
        BandaCarga.MEDIA -> c.pgHerido
        BandaCarga.SOBRECARGADO -> c.pgCritico
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(total, style = Tipos.cifraLg.copy(fontSize = 32.sp), color = color)
            Text("/ $limite VE", style = Tipos.uiSm, color = c.tintaSuave, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .height(24.dp)
                    .drawBehind {
                        interior(c, formaPlaca(0.5f), SolidColor(c.vitela300))
                        marco(Orn.PLACA, SolidColor(color), 0.5f * density)
                    }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(banda.name, style = TextStyle(fontFamily = Cinzel, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.16.em), color = color)
            }
        }
        Medidor(fraccion, SolidColor(color), marcas = listOf(1f / 3f), descripcion = "Carga")
        if (monedas != null) Text(monedas, style = Tipos.caption, color = c.tintaSuave)
    }
}

// ── Bolsa de monedas (CoinPurse) ──

enum class Moneda(val nombre: String, val abrev: String, internal val cara: Color) {
    PLATINO("Platino", "mpt", ColoresFijos.monedaPlatino),
    ORO("Oro", "mo", ColoresFijos.monedaOro),
    ELECTRO("Electro", "me", ColoresFijos.monedaElectro),
    PLATA("Plata", "mp", ColoresFijos.monedaPlata),
    COBRE("Cobre", "mc", ColoresFijos.monedaCobre)
}

/** La bandeja del tesoro: un pozo de hierro con una fila por moneda (siempre de platino a cobre). */
@Composable
fun Bolsa(
    cantidades: Map<Moneda, Int>,
    onCambio: (Moneda, Int) -> Unit,
    onExacta: (Moneda) -> Unit,
    total: String,
    modifier: Modifier = Modifier
) {
    val c = Marca.colores
    Column(modifier) {
        Placa(Modifier.fillMaxWidth(), relleno = SolidColor(c.vitela300)) {
            // En un teléfono estrecho la bandeja se aprieta para que el nombre de la moneda quepa en una línea
            BoxWithConstraints(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                val estrecha = maxWidth < 320.dp
                Column {
                    Moneda.entries.forEachIndexed { i, m ->
                        if (i > 0) Filete(Modifier.padding(horizontal = 8.dp))
                        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (estrecha) 8.dp else 12.dp)) {
                            CaraMoneda(m)
                            Text(
                                m.nombre,
                                style = TextStyle(fontFamily = Cinzel, fontSize = if (estrecha) 12.sp else 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.04.em),
                                color = c.tinta,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f)
                            )
                            Contador(cantidades[m] ?: 0, { onCambio(m, it) }, m.nombre, min = 0, onTocarValor = { onExacta(m) }, compacto = estrecha)
                        }
                    }
                }
            }
        }
        Text(
            buildAnnotatedString {
                append("Total: ")
                withStyle(SpanStyle(color = c.oro, fontFamily = Cinzel, fontSize = 16.sp, fontWeight = FontWeight.Bold)) { append(total) }
            },
            style = Tipos.uiSm, color = c.tintaSuave, modifier = Modifier.padding(top = 12.dp)
        )
    }
}

/** Cara de metal acuñado de una moneda, en su aro dorado. */
@Composable
fun CaraMoneda(m: Moneda, tamano: Dp = 36.dp) {
    val c = Marca.colores
    Box(
        Modifier
            .size(tamano)
            .drawBehind {
                val r = size.minDimension / 2f
                drawCircle(c.dorado, r + 3.dp.toPx())
                drawCircle(c.vitela300, r + 2.dp.toPx())
                drawCircle(m.cara, r)
                // Relieve: sombra abajo y brillo arriba
                drawCircle(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.35f), 0.25f to Color.Transparent, 0.8f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.25f)), r)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(m.abrev, style = TextStyle(fontFamily = Cinzel, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em), color = c.sobreOro)
    }
}

// ── Espacios de conjuro (SpellSlots) ──

data class EspacioNivel(val nivel: Int, val preparados: Int, val espacios: Int, val fijado: Boolean)

/** Espacios de una clase lanzadora: una celda escalonada por nivel, con un marco arcano. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EspaciosConjuro(clase: String, nivelLanzador: Int, niveles: List<EspacioNivel>, onNivel: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Marca.colores
    Column(modifier) {
        Text("$clase · nivel de lanzador $nivelLanzador", style = Tipos.uiSm, color = c.tintaSuave, modifier = Modifier.padding(bottom = 12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            niveles.forEach { n ->
                val exceso = n.preparados > n.espacios
                val color = if (exceso) c.sangre else c.azulArcano
                val interaccion = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .defaultMinSize(70.dp, 60.dp)
                        .foco(interaccion)
                        .clickable(interaccion, null, role = Role.Button) { onNivel(n.nivel) }
                        .drawBehind {
                            interior(c, FormaPlaca, SolidColor(c.vitela300), TipoTextura.PLACA)
                            marco(Orn.CHAFLAN, SolidColor(color), 16f / 18f * density)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (n.nivel == 0) "TRUCOS" else "NIVEL ${n.nivel}",
                        style = TextStyle(fontFamily = Cinzel, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.14.em),
                        color = color
                    )
                    Text(
                        buildAnnotatedString {
                            append("${n.preparados}/${n.espacios}")
                            if (n.fijado) withStyle(SpanStyle(color = c.violetaArcano)) { append("*") }
                        },
                        style = TextStyle(fontFamily = Cinzel, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
                        color = if (exceso) c.sangre else c.tinta
                    )
                }
            }
        }
    }
}

// ── Conjuro preparado (SpellEntry) ──

/** Un conjuro preparado: nombre, «N de M disponibles» o «Gastado», y una casilla de llama por copia. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntradaConjuro(
    nombre: String,
    copias: Int,
    lanzadas: Int,
    onLanzadas: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onNombre: (() -> Unit)? = null
) {
    val c = Marca.colores
    val quedan = copias - lanzadas
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .then(if (onNombre != null) Modifier.clickable(onClickLabel = "Ver detalles", onClick = onNombre) else Modifier)
            ) {
                Text(
                    nombre,
                    style = TextStyle(
                        fontFamily = Cinzel, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.03.em,
                        textDecoration = if (quedan == 0) TextDecoration.LineThrough else null
                    ),
                    color = if (quedan == 0) c.tintaSuave else c.tinta
                )
                Text(if (quedan == 0) "Gastado" else "$quedan de $copias disponibles", style = Tipos.caption, color = if (quedan == 0) c.tintaSuave else c.azulArcano)
            }
            FlowRow(horizontalArrangement = Arrangement.End, modifier = Modifier.widthIn(max = 200.dp)) {
                repeat(copias) { i ->
                    Casilla(
                        marcada = i < lanzadas,
                        onCambio = { marcar -> onLanzadas(if (marcar) i + 1 else i) },
                        marca = MarcaCasilla.LLAMA,
                        descripcion = "Lanzado ${i + 1}"
                    )
                }
            }
        }
        Filete()
    }
}
