package io.github.pauverblom.cronicas.ui.foto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object Retratos {

    private const val LADO = 512

    fun directorio(context: Context): File = File(context.filesDir, "retratos").apply { mkdirs() }

    /**
     * Copia la imagen recortada a filesDir/retratos (512 px, JPEG) y extrae su color dominante.
     * Cada guardado usa un nombre nuevo para que la caché de imágenes no muestre el retrato anterior.
     * Devuelve la ruta y el color semilla (ARGB).
     */
    suspend fun guardar(context: Context, pid: Long, uri: Uri, anterior: String?): Pair<String, Int?> = withContext(Dispatchers.IO) {
        val original = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
            ?: error("No se pudo leer la imagen")
        val escala = LADO.toFloat() / maxOf(original.width, original.height)
        val bmp = if (escala < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * escala).toInt(), (original.height * escala).toInt(), true)
        } else original
        val file = File(directorio(context), "retrato_${pid}_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        anterior?.let { File(it).takeIf { f -> f.absolutePath != file.absolutePath }?.delete() }
        file.absolutePath to colorSemilla(bmp)
    }

    /** Color dominante del retrato; si es muy apagado, el vibrante. */
    fun colorSemilla(bmp: Bitmap): Int? {
        val palette = Palette.from(bmp).maximumColorCount(16).generate()
        val dominante = palette.dominantSwatch
        val vibrante = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.darkVibrantSwatch
        val saturacion = dominante?.hsl?.get(1) ?: 0f
        return (if (saturacion < 0.15f && vibrante != null) vibrante else dominante ?: vibrante)?.rgb
    }
}

/** Retrato del personaje, o su inicial sobre el color primario si no tiene. */
@Composable
fun RetratoAvatar(
    path: String?,
    nombre: String,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape
) {
    if (path != null) {
        AsyncImage(
            model = File(path),
            contentDescription = "Retrato de $nombre",
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                nombre.trim().firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}

val RetratoTarjeta = RoundedCornerShape(12.dp)

/**
 * Lanzador del selector de retrato: galería o cámara, y luego recorte cuadrado.
 * Devuelve una función que se llama con `camara = true/false`.
 */
@Composable
fun rememberSelectorRetrato(onRecortado: (Uri) -> Unit): (camara: Boolean) -> Unit {
    val launcher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) result.uriContent?.let(onRecortado)
    }
    return { camara ->
        launcher.launch(
            CropImageContractOptions(
                uri = null,
                cropImageOptions = CropImageOptions(
                    imageSourceIncludeGallery = !camara,
                    imageSourceIncludeCamera = camara,
                    fixAspectRatio = true,
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    cropShape = CropImageView.CropShape.RECTANGLE,
                    guidelines = CropImageView.Guidelines.ON_TOUCH,
                    outputCompressFormat = Bitmap.CompressFormat.JPEG,
                    outputRequestWidth = 1024,
                    outputRequestHeight = 1024,
                    outputRequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_INSIDE,
                    activityTitle = "Recortar retrato",
                    cropMenuCropButtonTitle = "Usar"
                )
            )
        )
    }
}
