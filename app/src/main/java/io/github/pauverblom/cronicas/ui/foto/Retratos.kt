package io.github.pauverblom.cronicas.ui.foto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.palette.graphics.Palette
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import io.github.pauverblom.cronicas.ui.plataforma.Archivo
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
    suspend fun guardar(context: Context, pid: Long, uri: Archivo, anterior: String?): Pair<String, Int?> = withContext(Dispatchers.IO) {
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

    fun borrar(path: String) {
        File(path).delete()
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

/**
 * Lanzador del selector de retrato: galería o cámara, y luego recorte cuadrado.
 * Devuelve una función que se llama con `camara = true/false`.
 */
@Composable
fun rememberSelectorRetrato(onRecortado: (Archivo) -> Unit): (camara: Boolean) -> Unit {
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
