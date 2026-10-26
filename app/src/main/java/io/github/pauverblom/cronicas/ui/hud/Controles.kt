package io.github.pauverblom.cronicas.ui.hud

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.pauverblom.cronicas.ui.theme.Alegreya
import io.github.pauverblom.cronicas.ui.theme.Cinzel
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Paleta
import io.github.pauverblom.cronicas.ui.theme.Tipos

// ── Glifos ──

/** El juego pequeño de glifos del sistema. La estrella es «primaria», el lápiz «fijado a mano», la llama «lanzado». */
enum class Glifo(internal val clave: String) {
    LAPIZ("pencil"), ESTRELLA("star"), AVISO("warn"), VISTO("check"), LLAMA("flame"),
    MENOS("minus"), MAS("plus"), CERRAR("close"), ROMBO("diamond");

    internal val vector: ImageVector by lazy {
        ImageVector.Builder(clave, 24.dp, 24.dp, 24f, 24f)
            .addPath(PathParser().parsePathString(DATOS_GLIFOS.getValue(clave)).toNodes(), fill = SolidColor(Color.Black))
            .build()
    }
}

@Composable
fun IconoGlifo(glifo: Glifo, modifier: Modifier = Modifier, tamano: Dp = 20.dp, color: Color = LocalContentColor.current, descripcion: String? = null) {
    androidx.compose.material3.Icon(rememberVectorPainter(glifo.vector), descripcion, modifier.size(tamano), tint = color)
}

// ── Foco ──

/** Contorno de foco del sistema: 2 px sólidos, separados 3 px, cuando se navega con teclado o mando. */
@Composable
fun Modifier.foco(interaccion: MutableInteractionSource): Modifier {
    val enfocado by interaccion.collectIsFocusedAsState()
    val c = Marca.colores.foco
    return this.drawWithContent {
        drawContent()
        if (enfocado) {
            val m = 3.dp.toPx()
            drawRect(c, Offset(-m, -m), Size(size.width + 2 * m, size.height + 2 * m), style = Stroke(2.dp.toPx()))
        }
    }
}

// ── Botón ──

enum class Variante {
    /** La única llamada a la acción de la pantalla: «Crear personaje», «Siguiente», «Guardar», «Subir», «Descansar». */
    DORADO,
    /** Acciones secundarias con marco: «Atrás», «+ PX», «Importar personaje». */
    FANTASMA,
    /** Sin marco: «Cancelar», «Automático», descartes dentro de diálogos. */
    DISCRETO,
    /** Confirmar un diálogo destructivo. */
    PELIGRO,
    /** La acción destructiva secundaria de un formulario («Eliminar», «Quitar»). */
    PELIGRO_DISCRETO
}

/**
 * Botón del sistema: una placa de metal con puntas, remates de rombo y líneas atadas.
 * Deshabilitado sigue en su sitio (hierro y tinta tenue). [brillo] hace latir el oro (la llamada a subir de nivel).
 */
@Composable
fun Boton(
    texto: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variante: Variante = Variante.FANTASMA,
    icono: Glifo? = null,
    activo: Boolean = true,
    bloque: Boolean = false,
    brillo: Boolean = false,
    descripcion: String? = null,
    compacto: Boolean = false
) {
    val c = Marca.colores
    val interaccion = remember { MutableInteractionSource() }
    val pulsado by interaccion.collectIsPressedAsState()
    val enmarcado = variante != Variante.DISCRETO && variante != Variante.PELIGRO_DISCRETO
    val color = when {
        !activo -> c.tintaTenue
        variante == Variante.DORADO -> c.sobreOro
        variante == Variante.PELIGRO -> c.sobreSangre
        variante == Variante.PELIGRO_DISCRETO -> c.sangre
        else -> c.oro
    }
    val pulso = if (brillo && activo && io.github.pauverblom.cronicas.ui.common.animacionesActivas()) {
        val t = rememberInfiniteTransition(label = "brillo")
        t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "pulso")
    } else null
    val forma = when {
        !activo || !enmarcado -> formaPlaca(1f)
        variante == Variante.DORADO || variante == Variante.PELIGRO -> formaPlaca(1f, margenV = 7f, base = 20.5f, punta = 11.5f)
        else -> formaPlaca(1f)
    }
    val relleno: Brush? = when {
        !enmarcado -> if (pulsado) SolidColor(c.vitela300) else null
        !activo -> SolidColor(c.vitela300)
        variante == Variante.DORADO -> rellenoOro(c)
        variante == Variante.PELIGRO -> SolidColor(c.sangreRelleno)
        else -> cristal(c)
    }
    Row(
        modifier
            .then(if (bloque) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = if (compacto) 36.dp else 48.dp)
            .offset(y = if (pulsado) 1.dp else 0.dp)
            .foco(interaccion)
            .clickable(interaccion, null, enabled = activo, role = Role.Button, onClick = onClick)
            .semantics { if (descripcion != null) contentDescription = descripcion }
            .drawBehind {
                if (pulso != null) halo(formaPlaca(1f).contorno(this), c.metalMedio.copy(alpha = 0.55f * pulso.value), 10.dp.toPx())
                interior(c, forma, relleno)
                if (enmarcado) marco(Orn.PLACA, (if (activo) Pintura.Metal else Pintura.Hierro).brocha(c), density)
            }
            .padding(horizontal = if (enmarcado) 36.dp else 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icono?.let { IconoGlifo(it, tamano = 18.dp, color = color) }
        if (texto != null) Text(texto.uppercase(), style = Tipos.boton, color = color, textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** Botón de solo icono, sin marco, con su objetivo táctil de 48 px (acciones de sistema y lápices). */
@Composable
fun BotonIcono(onClick: () -> Unit, descripcion: String, modifier: Modifier = Modifier, activo: Boolean = true, contenido: @Composable () -> Unit) {
    val c = Marca.colores
    val interaccion = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(48.dp)
            .foco(interaccion)
            .clickable(interaccion, null, enabled = activo, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (activo) c.oro else c.tintaTenue) { contenido() }
    }
}

// ── Contador (Stepper) ──

/** −1 / valor / +1 sobre una placa de hierro. Tocar el número abre la cantidad exacta si hay [onTocarValor]. */
@Composable
fun Contador(
    valor: Int,
    onValor: (Int) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    onTocarValor: (() -> Unit)? = null,
    formato: (Int) -> String = { it.toString() },
    compacto: Boolean = false
) {
    val c = Marca.colores
    Row(
        modifier
            .drawBehind {
                interior(c, formaPlaca(1f), SolidColor(c.vitela300))
                marco(Orn.PLACA, Pintura.Hierro.brocha(c), density)
            }
            .padding(horizontal = if (compacto) 10.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PasoContador(Glifo.MENOS, "$etiqueta −1", valor > min) { onValor((valor - 1).coerceAtLeast(min)) }
        Text(
            formato(valor),
            style = Tipos.cifraMd,
            color = c.tinta,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 40.dp)
                .then(if (onTocarValor != null) Modifier.clickable(role = Role.Button, onClickLabel = "Cantidad exacta", onClick = onTocarValor) else Modifier)
        )
        PasoContador(Glifo.MAS, "$etiqueta +1", valor < max) { onValor((valor + 1).coerceAtMost(max)) }
    }
}

@Composable
private fun PasoContador(glifo: Glifo, descripcion: String, activo: Boolean, onClick: () -> Unit) {
    val c = Marca.colores
    val interaccion = remember { MutableInteractionSource() }
    Box(
        Modifier
            .width(40.dp)
            .height(48.dp)
            .foco(interaccion)
            .clickable(interaccion, null, enabled = activo, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) { IconoGlifo(glifo, color = if (activo) c.oro else c.tintaTenue) }
}

// ── Casilla (Check) ──

enum class MarcaCasilla { VISTO, LLAMA }

/**
 * Casilla en rombo: un losange con borde de metal que se llena de oro y brilla al marcarse.
 * Con [MarcaCasilla.LLAMA] (copias de conjuro lanzadas) se enciende con el fuego común.
 */
@Composable
fun Casilla(
    marcada: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activa: Boolean = true,
    marca: MarcaCasilla = MarcaCasilla.VISTO,
    etiqueta: String? = null,
    descripcion: String? = null,
    contenido: (@Composable RowScope.() -> Unit)? = null
) {
    val c = Marca.colores
    val interaccion = remember { MutableInteractionSource() }
    Row(
        modifier
            .defaultMinSize(48.dp, 48.dp)
            .toggleable(marcada, interaccion, null, enabled = activa, role = Role.Checkbox, onValueChange = onCambio)
            .semantics { if (descripcion != null) contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(48.dp).foco(interaccion), contentAlignment = Alignment.Center) {
            Rombo(c, marcada, activa, marca)
        }
        if (etiqueta != null) {
            Text(etiqueta, style = Tipos.ui, color = if (activa) c.tinta else c.tintaTenue, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
        }
        contenido?.invoke(this)
    }
}

private val FuegoCasilla = io.github.pauverblom.cronicas.ui.common.llamaDe(null)

@Composable
private fun Rombo(c: Paleta, marcada: Boolean, activa: Boolean, marca: MarcaCasilla) {
    Box(Modifier.size(22.dp).rotate(45f).drawBehind {
        val borde = 1.5.dp.toPx()
        val anillo = 2.dp.toPx()
        // Anillo exterior: 2 px de pozo y 1 px de metal oscuro
        drawRect(if (activa) c.metalBajo else c.hierroBajo, Offset(-anillo - 1.dp.toPx(), -anillo - 1.dp.toPx()), Size(size.width + 2 * (anillo + 1.dp.toPx()), size.height + 2 * (anillo + 1.dp.toPx())))
        drawRect(c.vitela300, Offset(-anillo, -anillo), Size(size.width + 2 * anillo, size.height + 2 * anillo))
        val metal = if (activa) Brush.linearGradient(listOf(c.metalAlto, c.metalMedio, c.metalBajo, c.metalMedio, c.metalAlto))
        else Brush.linearGradient(listOf(c.hierroAlto, c.hierro, c.hierroBajo, c.hierro, c.hierroAlto))
        drawRect(metal)
        val dentro = Offset(borde, borde)
        val lado = Size(size.width - 2 * borde, size.height - 2 * borde)
        when {
            !marcada -> drawRect(c.vitela300, dentro, lado)
            marca == MarcaCasilla.LLAMA -> {
                halo(androidx.compose.ui.graphics.Path().apply { addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size)) }, FuegoCasilla.brillo.copy(alpha = 0.8f), 6.dp.toPx())
                drawRect(Brush.radialGradient(0f to FuegoCasilla.chispa, 0.6f to FuegoCasilla.brillo, 1f to FuegoCasilla.brillo), dentro, lado)
            }
            else -> {
                halo(androidx.compose.ui.graphics.Path().apply { addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size)) }, c.metalMedio.copy(alpha = 0.7f), 5.dp.toPx())
                drawRect(rellenoOro(c), dentro, lado)
            }
        }
    }, contentAlignment = Alignment.Center) {
        if (marcada) {
            IconoGlifo(
                if (marca == MarcaCasilla.LLAMA) Glifo.LLAMA else Glifo.VISTO,
                Modifier.rotate(-45f),
                tamano = 16.dp,
                color = if (marca == MarcaCasilla.LLAMA) FuegoCasilla.nucleo else c.sobreOro
            )
        }
    }
}

/** Casilla de opción única (para elegir tema): el mismo rombo, con semántica de radio. */
@Composable
fun Opcion(seleccionada: Boolean, onElegir: () -> Unit, modifier: Modifier = Modifier, activa: Boolean = true, contenido: @Composable RowScope.() -> Unit) {
    val c = Marca.colores
    val interaccion = remember { MutableInteractionSource() }
    Row(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .foco(interaccion)
            .clickable(interaccion, null, enabled = activa, role = Role.RadioButton, onClick = onElegir),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Rombo(c, seleccionada, activa, MarcaCasilla.VISTO) }
        contenido()
    }
}

// ── Campo (Field) ──

/**
 * Campo de texto: etiqueta en versalitas con un rombo de oro, una placa de hierro con esquinas escalonadas que se
 * vuelve de oro y brilla al enfocarla, y la ayuda o el error debajo. [error] sustituye a la ayuda y tiñe el marco.
 * Con [onClick] el campo es de solo lectura y abre algo (los desplegables).
 */
@Composable
fun Campo(
    valor: String,
    onValor: (String) -> Unit,
    etiqueta: String?,
    modifier: Modifier = Modifier,
    unaLinea: Boolean = true,
    minLineas: Int = 1,
    soporte: String? = null,
    error: String? = null,
    sufijo: String? = null,
    marcador: String? = null,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    onClick: (() -> Unit)? = null,
    final: (@Composable () -> Unit)? = null,
    enfocarAlAbrir: Boolean = false
) {
    val c = Marca.colores
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    // El texto con su selección: así un campo que se enfoca al abrir puede traer el valor ya seleccionado,
    // y lo que se escribe lo sustituye en vez de añadirse delante («0» + «120» = «1200»)
    var campo by remember { mutableStateOf(TextFieldValue(valor, TextRange(valor.length))) }
    if (campo.text != valor) campo = campo.copy(text = valor, selection = TextRange(valor.length))
    val foco = remember { FocusRequester() }
    if (enfocarAlAbrir) {
        LaunchedEffect(Unit) {
            foco.requestFocus()
            campo = campo.copy(selection = TextRange(0, campo.text.length))
        }
    }
    val multilinea = !unaLinea || minLineas > 1
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (etiqueta != null) EtiquetaCampo(etiqueta)
        val estilo = (if (multilinea) Tipos.cuerpo.copy(fontFamily = Alegreya) else Tipos.ui).copy(color = c.tinta)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (multilinea) (minLineas * 24 + 32).coerceAtLeast(120).dp else 52.dp)
                .drawBehind {
                    if (enfocado && error == null) halo(FormaPlaca.contorno(this), c.metalBajo.copy(alpha = 0.6f), 5.dp.toPx())
                    interior(c, FormaPlaca, SolidColor(c.vitela300), TipoTextura.PLACA)
                    val pintura = when {
                        error != null -> Pintura.Plana(c.sangre)
                        enfocado -> Pintura.Metal
                        else -> Pintura.Hierro
                    }
                    marco(Orn.CHAFLAN, pintura.brocha(c), 16f / 18f * density)
                }
                .then(if (onClick != null) Modifier.foco(interaccion).clickable(interaccion, null, role = Role.Button, onClick = onClick) else Modifier),
            verticalAlignment = if (multilinea) Alignment.Top else Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).padding(horizontal = 24.dp, vertical = if (multilinea) 16.dp else 14.dp)) {
                if (onClick != null) {
                    Text(valor.ifEmpty { marcador.orEmpty() }, style = estilo, color = if (valor.isEmpty()) c.tintaTenue else c.tinta)
                } else {
                    BasicTextField(
                        campo,
                        { nuevo ->
                            campo = nuevo
                            if (nuevo.text != valor) onValor(nuevo.text)
                        },
                        textStyle = estilo,
                        singleLine = unaLinea,
                        minLines = if (unaLinea) 1 else minLineas,
                        keyboardOptions = teclado,
                        cursorBrush = SolidColor(c.oro),
                        interactionSource = interaccion,
                        modifier = Modifier.fillMaxWidth().focusRequester(foco).semantics { if (etiqueta != null) contentDescription = etiqueta },
                        decorationBox = { campo ->
                            if (valor.isEmpty() && marcador != null) Text(marcador, style = estilo, color = c.tintaTenue)
                            campo()
                        }
                    )
                }
            }
            if (sufijo != null) {
                Text(sufijo, style = TextStyle(fontFamily = Cinzel, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em), color = c.tintaSuave, modifier = Modifier.padding(end = 24.dp))
            }
            final?.let { Box(Modifier.padding(end = 8.dp)) { it() } }
        }
        val mensaje = error ?: soporte
        if (mensaje != null) Text(mensaje, style = Tipos.caption, color = if (error != null) c.sangre else c.tintaSuave)
    }
}

/** Etiqueta de campo: versalitas en tinta suave con un rombo de oro delante. */
@Composable
fun EtiquetaCampo(texto: String, modifier: Modifier = Modifier) {
    val c = Marca.colores
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(6.dp).rotate(45f)) { drawRect(rellenoOro(c)) }
        Text(texto.uppercase(), style = Tipos.etiquetaCaps.copy(letterSpacing = 0.12.em), color = c.tintaSuave)
    }
}

/** Opciones de teclado numérico para [Campo]. */
fun tecladoNumerico(decimales: Boolean = false) =
    KeyboardOptions(keyboardType = if (decimales) KeyboardType.Decimal else KeyboardType.Number)

// ── Etiqueta (Tag) ──

enum class Tono { LISO, ORO, ARCANO, PELIGRO }

/**
 * Placa pequeña: condiciones, sugerencias, filtros. [insignia] es la versión en versalitas de oro («EN MANO»);
 * sin [onClick] es solo informativa.
 */
@Composable
fun Etiqueta(
    texto: String,
    modifier: Modifier = Modifier,
    tono: Tono = Tono.LISO,
    nota: String? = null,
    insignia: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val c = Marca.colores
    val k = when {
        insignia -> 0.5f
        onClick != null -> 32f / 48f
        else -> 28f / 48f
    }
    val pintura = when (tono) {
        Tono.LISO -> Pintura.Hierro
        Tono.ORO -> Pintura.Metal
        Tono.ARCANO -> Pintura.Plana(c.azulArcano)
        Tono.PELIGRO -> Pintura.Plana(c.sangre)
    }
    val color = when (tono) {
        Tono.LISO -> c.tinta
        Tono.ORO -> c.oro
        Tono.ARCANO -> c.azulArcano
        Tono.PELIGRO -> c.sangre
    }
    val interaccion = remember { MutableInteractionSource() }
    val alto = when {
        insignia -> 24.dp
        onClick != null -> 32.dp
        else -> 28.dp
    }
    // El objetivo táctil llega a 48 px aunque la placa mida menos
    Box(
        modifier
            .then(if (onClick != null) Modifier.heightIn(min = 48.dp).foco(interaccion).clickable(interaccion, null, role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .height(alto)
                .drawBehind {
                    interior(c, formaPlaca(k), SolidColor(if (insignia) c.vitela300 else c.vitela200))
                    marco(Orn.PLACA, pintura.brocha(c), k * density)
                }
                .padding(horizontal = if (insignia) 18.dp else if (onClick != null) 22.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                texto,
                style = if (insignia) TextStyle(fontFamily = Cinzel, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.14.em) else Tipos.uiSm,
                color = color,
                maxLines = 1
            )
            if (nota != null) Text(": $nota", style = Tipos.uiSm, color = c.tintaSuave, maxLines = 1)
        }
    }
}

// ── Texto que se ajusta ──

/**
 * Muestra [completo] en una línea, encogiendo la letra desde la de [estilo] hasta [minimo] si hace falta; si ni así
 * cabe, muestra [abreviado] al tamaño normal. Para poner la palabra entera («CONSTITUCIÓN») donde quepa.
 */
@Composable
fun TextoAjustado(
    completo: String,
    abreviado: String,
    estilo: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minimo: androidx.compose.ui.unit.TextUnit = 8.sp,
    textAlign: TextAlign? = null
) {
    val medidor = androidx.compose.ui.text.rememberTextMeasurer()
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val ancho = constraints.maxWidth
        val elegido = remember(completo, abreviado, estilo, ancho) {
            var tamano = estilo.fontSize.value
            var res: Pair<String, TextStyle>? = null
            while (tamano >= minimo.value) {
                val e = estilo.copy(fontSize = tamano.sp)
                if (medidor.measure(completo, e, maxLines = 1, softWrap = false).size.width <= ancho) { res = completo to e; break }
                tamano -= 0.5f
            }
            res ?: (abreviado to estilo)
        }
        Text(elegido.first, style = elegido.second, color = color, maxLines = 1, softWrap = false, textAlign = textAlign,
            modifier = if (textAlign != null) Modifier.fillMaxWidth() else Modifier)
    }
}
