package io.github.pauverblom.cronicas.ui.plataforma

import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import io.github.pauverblom.cronicas.web.jsEditorPoner
import io.github.pauverblom.cronicas.web.jsEditorColocar
import io.github.pauverblom.cronicas.web.jsEditorCerrar
import io.github.pauverblom.cronicas.web.jsEditorAbrir
import io.github.pauverblom.cronicas.ui.theme.FuentesWeb
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.FocusInteraction
import io.github.pauverblom.cronicas.web.jsPintarBarra
import io.github.pauverblom.cronicas.web.jsTactil
import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.pauverblom.cronicas.ui.foto.AlmacenRetratos
import io.github.pauverblom.cronicas.web.aBase64
import io.github.pauverblom.cronicas.web.deBase64
import io.github.pauverblom.cronicas.web.elegirArchivo
import io.github.pauverblom.cronicas.web.jsAhora
import io.github.pauverblom.cronicas.web.jsAnimaciones
import io.github.pauverblom.cronicas.web.jsDescargar
import io.github.pauverblom.cronicas.web.jsFechaHora
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * Versión web de ui/plataforma/Plataforma.kt de la app Android: mismas declaraciones, hechas con el navegador.
 */

/** Un archivo elegido por el usuario: para abrir trae su contenido; para guardar, solo el nombre y el tipo. */
class Archivo(val nombre: String, val bytes: ByteArray, val mime: String = "")

class Aviso(private val mostrar: (String, Boolean) -> Unit) {
    operator fun invoke(texto: String, largo: Boolean = false) = mostrar(texto, largo)
}

/** El aviso en pantalla; lo pinta [AvisoHost], encima de todo. */
private val avisoActual = mutableStateOf<Pair<String, Long>?>(null)

@Composable
fun rememberAviso(): Aviso = remember {
    Aviso { texto, largo -> avisoActual.value = texto to (if (largo) 3500L else 2000L) }
}

/** El «toast» de la web: un mensaje abajo durante un par de segundos. */
@Composable
fun AvisoHost() {
    val aviso = avisoActual.value ?: return
    LaunchedEffect(aviso) {
        delay(aviso.second)
        if (avisoActual.value == aviso) avisoActual.value = null
    }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomCenter) {
        Surface(shape = RoundedCornerShape(20.dp), color = androidx.compose.ui.graphics.Color(0xE6201812), modifier = Modifier.widthIn(max = 420.dp)) {
            Text(aviso.first, color = androidx.compose.ui.graphics.Color(0xFFF1E3C4), modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        }
    }
}

/** En la web no se elige dónde guardar: la función devuelta descarga el archivo con ese nombre. */
@Composable
fun rememberGuardarArchivo(mime: String, onElegido: (Archivo) -> Unit): (nombre: String) -> Unit =
    { nombre -> onElegido(Archivo(nombre, ByteArray(0), mime)) }

@Serializable
private class ArchivoJs(val nombre: String, val mime: String = "", val base64: String)

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun rememberAbrirArchivo(tipos: Array<String>, onElegido: (Archivo) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val accept = if ("*/*" in tipos) "" else tipos.joinToString(",")
    return {
        val esperar = elegirArchivo(accept)
        scope.launch {
            val elegido = esperar() ?: return@launch
            val a = json.decodeFromString(ArchivoJs.serializer(), elegido)
            onElegido(Archivo(a.nombre, deBase64(a.base64), a.mime))
        }
    }
}

@Suppress("UNUSED_PARAMETER")
suspend fun leerArchivo(app: Application, archivo: Archivo): ByteArray = archivo.bytes

@Suppress("UNUSED_PARAMETER")
suspend fun escribirArchivo(app: Application, archivo: Archivo, bytes: ByteArray) =
    jsDescargar(archivo.nombre, archivo.mime, aBase64(bytes))

fun modeloArchivo(archivo: Archivo): Any = archivo.bytes

fun modeloRetrato(path: String?): Any? = path?.let(AlmacenRetratos::leer)

fun ahora(): Long = jsAhora().toLong()

fun formatoFechaHora(ms: Long): String = jsFechaHora(ms.toDouble())

/** El navegador no tiene botón de atrás que capturar desde aquí: no hace nada. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ManejarAtras(activo: Boolean = true, onAtras: () -> Unit) {
}

@Composable
fun animacionesDelSistema(): Boolean = remember { jsAnimaciones() }

fun materialYouDisponible(): Boolean = false

@Suppress("UNUSED_PARAMETER")
@Composable
fun esquemaDinamico(oscuro: Boolean): ColorScheme? = null

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun anchoVentanaDp(): Int {
    val ancho = LocalWindowInfo.current.containerSize.width
    return with(LocalDensity.current) { ancho.toDp().value.toInt() }
}

/** La franja de arriba (bajo la barra de estado de iPhone/iPad) y el fondo de la página, del color de la noche. */
fun pintarBarraSistema(argb: Int) = jsPintarBarra(argb)

/**
 * En pantallas táctiles no: Safari (iPhone, iPad) solo saca el teclado si el foco llega dentro de un toque, y un
 * campo ya enfocado no vuelve a pedirlo al tocarlo, así que el teclado no salía nunca.
 */
fun enfocarSinToqueDisponible(): Boolean = !jsTactil()

fun propiedadesDialogo() = DialogProperties(usePlatformDefaultWidth = false)

// ── Escritura nativa en pantallas táctiles ──

/**
 * En pantallas táctiles el campo de texto de Compose no se lleva bien con el teclado de iPhone y iPad (no siempre
 * sale, no hay selección ni pegar del sistema, borrar no salta por palabras). Mientras se escribe, se pone encima
 * del campo un <input> real y transparente (ayudas.js, cronicasEditor); Compose solo pinta el texto cuando no se
 * escribe. [abrir] hay que llamarlo dentro del toque: Safari solo saca el teclado así.
 */
class EditorNativo internal constructor(private val interaccion: MutableInteractionSource) {
    internal var editando by mutableStateOf(false)
    private var elemento: JsAny? = null
    private var enfoque: FocusInteraction.Focus? = null
    /** Caja del texto en px CSS, relativa a #app. */
    private var caja = floatArrayOf(0f, 0f, 0f, 0f)
    // Lo último que se pintó: valor, a quién avisar y cómo es el texto
    internal var valor = ""
    internal var onValor: (String) -> Unit = {}
    internal var opciones = ""

    fun abrir() {
        elemento?.let { return }
        val e = jsEditorAbrir(opciones, valor, caja[0], caja[1], caja[2], caja[3], { nuevo -> valor = nuevo; onValor(nuevo) }, { cerrar() })
        elemento = e
        editando = true
        enfoque = FocusInteraction.Focus().also { interaccion.tryEmit(it) }
    }

    internal fun cerrar() {
        val e = elemento ?: return
        elemento = null
        jsEditorCerrar(e)
        editando = false
        enfoque?.let { interaccion.tryEmit(FocusInteraction.Unfocus(it)) }
        enfoque = null
    }

    internal fun colocar(x: Float, y: Float, ancho: Float, alto: Float) {
        caja = floatArrayOf(x, y, ancho, alto)
        elemento?.let { jsEditorColocar(it, x, y, ancho, alto) }
    }

    internal fun sincronizar(texto: String) {
        valor = texto
        elemento?.let { jsEditorPoner(it, texto) }
    }

    /** El texto del campo: pintado por Compose, salvo mientras se escribe en el <input> de encima. */
    @Composable
    fun Texto(
        valor: String,
        onValor: (String) -> Unit,
        estilo: TextStyle,
        unaLinea: Boolean,
        minLineas: Int,
        teclado: KeyboardOptions,
        marcador: String?,
        colorMarcador: Color,
        colorCursor: Color,
        descripcion: String?
    ) {
        this.onValor = onValor
        opciones = opcionesEditor(estilo, unaLinea, teclado, colorCursor, descripcion)
        LaunchedEffect(valor) { sincronizar(valor) }
        DisposableEffect(Unit) { onDispose { cerrar() } }
        val densidad = LocalDensity.current.density
        Box(
            Modifier.fillMaxWidth().onGloballyPositioned { c ->
                val r = c.boundsInWindow()
                colocar(r.left / densidad, r.top / densidad, r.width / densidad, r.height / densidad)
            }
        ) {
            if (valor.isEmpty() && marcador != null) Text(marcador, style = estilo, color = colorMarcador, maxLines = if (unaLinea) 1 else Int.MAX_VALUE)
            Text(
                valor,
                style = estilo,
                // Mientras se escribe, el texto lo pinta el <input>; este solo guarda el sitio
                color = if (editando) Color.Transparent else estilo.color,
                maxLines = if (unaLinea) 1 else Int.MAX_VALUE,
                minLines = minLineas,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** El editor nativo del campo, solo en pantallas táctiles (null: el campo de Compose de siempre). */
@Composable
fun rememberEditorNativo(interaccion: MutableInteractionSource): EditorNativo? =
    remember(interaccion) { if (jsTactil()) EditorNativo(interaccion) else null }

private fun opcionesEditor(estilo: TextStyle, unaLinea: Boolean, teclado: KeyboardOptions, cursor: Color, descripcion: String?): String {
    val familia = when (estilo.fontFamily) {
        FuentesWeb.alegreya -> "Alegreya, serif"
        FuentesWeb.alegreyaSans -> "'Alegreya Sans', sans-serif"
        FuentesWeb.cinzel -> "Cinzel, serif"
        else -> "serif"
    }
    val tamano = if (estilo.fontSize.isSp) estilo.fontSize.value else 16f
    val interlineado = if (estilo.lineHeight.isSp) estilo.lineHeight.value else tamano * 1.3f
    val espaciado = when {
        estilo.letterSpacing.isSp -> estilo.letterSpacing.value
        estilo.letterSpacing.isEm -> estilo.letterSpacing.value * tamano
        else -> 0f
    }
    val modo = when (teclado.keyboardType) {
        KeyboardType.Number, KeyboardType.NumberPassword -> "numeric"
        KeyboardType.Decimal -> "decimal"
        KeyboardType.Phone -> "tel"
        KeyboardType.Email -> "email"
        KeyboardType.Uri -> "url"
        else -> "text"
    }
    val mayusculas = teclado.capitalization != KeyboardCapitalization.None || modo == "text"
    return buildJsonObject {
        put("multilinea", !unaLinea)
        put("modo", modo)
        put("mayusculas", mayusculas && modo == "text")
        put("fuente", familia)
        put("tamano", tamano)
        put("peso", estilo.fontWeight?.weight ?: 400)
        put("cursiva", estilo.fontStyle == FontStyle.Italic)
        put("interlineado", interlineado)
        put("espaciado", espaciado)
        put("color", cssColor(estilo.color))
        put("cursor", cssColor(cursor))
        descripcion?.let { put("etiqueta", it) }
    }.toString()
}

private fun cssColor(c: Color): String {
    val argb = c.toArgb()
    return "rgba(${argb shr 16 and 0xFF}, ${argb shr 8 and 0xFF}, ${argb and 0xFF}, ${(argb ushr 24) / 255f})"
}
