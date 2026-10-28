package io.github.pauverblom.cronicas.ui.plataforma

import android.app.Application
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Lo que depende de Android en la interfaz. La versión web de la app (módulo web) tiene un archivo con las mismas
 * declaraciones; el resto de la interfaz se comparte tal cual.
 */

/** Un archivo elegido por el usuario (para abrir o guardar). */
typealias Archivo = Uri

/** Mensaje breve al usuario (un toast). */
class Aviso(private val mostrar: (String, Boolean) -> Unit) {
    operator fun invoke(texto: String, largo: Boolean = false) = mostrar(texto, largo)
}

@Composable
fun rememberAviso(): Aviso {
    val context = LocalContext.current
    return remember(context) {
        Aviso { texto, largo -> Toast.makeText(context, texto, if (largo) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show() }
    }
}

/** Pide dónde guardar un archivo de tipo [mime]; la función devuelta recibe el nombre sugerido. */
@Composable
fun rememberGuardarArchivo(mime: String, onElegido: (Archivo) -> Unit): (nombre: String) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mime)) { uri ->
        if (uri != null) onElegido(uri)
    }
    return { nombre -> launcher.launch(nombre) }
}

/** Pide un archivo para abrir, de alguno de los tipos [tipos]. */
@Composable
fun rememberAbrirArchivo(tipos: Array<String>, onElegido: (Archivo) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onElegido(uri)
    }
    return { launcher.launch(tipos) }
}

suspend fun leerArchivo(app: Application, archivo: Archivo): ByteArray = withContext(Dispatchers.IO) {
    app.contentResolver.openInputStream(archivo)!!.use { it.readBytes() }
}

suspend fun escribirArchivo(app: Application, archivo: Archivo, bytes: ByteArray) = withContext(Dispatchers.IO) {
    app.contentResolver.openOutputStream(archivo)!!.use { it.write(bytes) }
}

/** Lo que Coil necesita para mostrar un archivo elegido (p. ej. el retrato antes de recortarlo). */
fun modeloArchivo(archivo: Archivo): Any = archivo

/** Lo que Coil necesita para mostrar el retrato guardado en [path], o null si no hay. */
fun modeloRetrato(path: String?): Any? = path?.let(::File)?.takeIf { it.exists() }

fun ahora(): Long = System.currentTimeMillis()

private val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))

/** «31/12/2026 18:05». */
fun formatoFechaHora(ms: Long): String = synchronized(formatoFecha) { formatoFecha.format(Date(ms)) }

/** Botón o gesto de atrás del sistema. */
@Composable
fun ManejarAtras(activo: Boolean = true, onAtras: () -> Unit) = BackHandler(enabled = activo, onBack = onAtras)

/** Falso si el usuario ha quitado las animaciones del sistema (escala de animación 0). */
@Composable
fun animacionesDelSistema(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

/** Si el sistema da los colores del fondo de pantalla (Material You, Android 12+). */
fun materialYouDisponible(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Colores del fondo de pantalla (Material You, Android 12+), o null si el sistema no los da. */
@Composable
fun esquemaDinamico(oscuro: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return remember(oscuro) { if (oscuro) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) }
}

/** Ventana de los diálogos: ocupa la pantalla para poder apartarse del teclado. */
fun propiedadesDialogo() = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
