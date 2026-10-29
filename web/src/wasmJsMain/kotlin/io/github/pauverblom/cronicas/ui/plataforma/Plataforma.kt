package io.github.pauverblom.cronicas.ui.plataforma

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

/**
 * En pantallas táctiles no: Safari (iPhone, iPad) solo saca el teclado si el foco llega dentro de un toque, y un
 * campo ya enfocado no vuelve a pedirlo al tocarlo, así que el teclado no salía nunca.
 */
fun enfocarSinToqueDisponible(): Boolean = !jsTactil()

fun propiedadesDialogo() = DialogProperties(usePlatformDefaultWidth = false)
