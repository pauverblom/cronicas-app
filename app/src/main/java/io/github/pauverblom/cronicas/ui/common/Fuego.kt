package io.github.pauverblom.cronicas.ui.common

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Paleta y carácter de un fuego (fire-*-core/glow/spark del sistema de diseño): [nucleo] es la base honda,
 * [brillo] el cuerpo y los halos, [chispa] las puntas y las brasas.
 * [ascenso] escala la velocidad de subida y [vaiven] la amplitud del balanceo lateral.
 */
@Immutable
data class Llama(
    val nucleo: Color,
    val brillo: Color,
    val chispa: Color,
    val ascenso: Float = 1f,
    val vaiven: Float = 1f
)

private val FuegoComun = Llama(nucleo = Color(0xFFA8380A), brillo = Color(0xFFFF8A2A), chispa = Color(0xFFFFD27A))

/** Fuego de cada clase; el fuego común mientras no se ha elegido ninguna (y para Guerrero y Bárbaro). */
fun llamaDe(claseId: String?): Llama = when (claseId) {
    "clerigo", "paladin" -> Llama(Color(0xFFA8740A), Color(0xFFF5C542), Color(0xFFFFF3C4), ascenso = 0.8f, vaiven = 0.6f)
    "mago", "ilusionista" -> Llama(Color(0xFF4227A6), Color(0xFF8A5CF6), Color(0xFFA9D4FF), vaiven = 1.4f)
    "druida", "explorador" -> Llama(Color(0xFF2D6A1D), Color(0xFF6FCE4A), Color(0xFFDCF6A8), ascenso = 0.8f)
    // El más lento en subir y el que más se mece
    "armonico_hueso" -> Llama(Color(0xFF2F5F4A), Color(0xFFA6E3C4), Color(0xFFEFFFF6), ascenso = 0.6f, vaiven = 2f)
    "asesino", "ladron" -> Llama(Color(0xFF4F0D0D), Color(0xFFB3261E), Color(0xFF9A958F), ascenso = 0.9f, vaiven = 1.3f)
    "bardo" -> Llama(Color(0xFFA82D64), Color(0xFFFF7EB6), Color(0xFFFFD36E), vaiven = 1.6f)
    "caballero" -> Llama(Color(0xFF2B4767), Color(0xFF7FA7D1), Color(0xFFF4F8FF), ascenso = 0.8f, vaiven = 0.6f)
    "mistico" -> Llama(Color(0xFF561C84), Color(0xFFC05CE0), Color(0xFFFFB3E6), ascenso = 0.7f, vaiven = 1.2f)
    else -> FuegoComun
}

/** La misma llama con los colores animados, para que el cambio de clase no sea brusco. */
@Composable
fun animarLlama(llama: Llama): Llama {
    val nucleo by animateColorAsState(llama.nucleo, tween(600), label = "nucleo")
    val brillo by animateColorAsState(llama.brillo, tween(600), label = "brillo")
    val chispa by animateColorAsState(llama.chispa, tween(600), label = "chispa")
    return llama.copy(nucleo = nucleo, brillo = brillo, chispa = chispa)
}

/**
 * Falso en lo que no se ve aunque esté compuesto (las pestañas vecinas del pager): ahí las animaciones continuas
 * se quedan quietas para no pintar fotogramas que nadie mira.
 */
val LocalAnimacionesVisibles = staticCompositionLocalOf { true }

/**
 * Falso si el usuario ha quitado las animaciones del sistema (escala de animación 0): entonces no hay partículas.
 * También es falso fuera de la pestaña visible ([LocalAnimacionesVisibles]).
 */
@Composable
fun animacionesActivas(): Boolean {
    val context = LocalContext.current
    val sistema = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    return sistema && LocalAnimacionesVisibles.current
}

// ── Partículas ──

/** Una brasa: posición y velocidad en px, vida en segundos. */
private class Particula(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val duracion: Float,
    val radio: Float,
    val fase: Float,
    /** Amplitud del balanceo lateral en px/s. */
    val vaiven: Float,
    /** Frenado por segundo (1 = ninguno). */
    val frenado: Float,
    /** Aceleración vertical en px/s² (negativa = sube). */
    val empuje: Float,
    /** Radio del halo respecto al núcleo; pequeño en los estallidos, que caen sobre las ilustraciones claras. */
    val halo: Float = 5f
) {
    var vida = 0f

    fun avanzar(dt: Float) {
        vida += dt
        val f = Math.pow(frenado.toDouble(), dt.toDouble()).toFloat()
        vx *= f
        vy = vy * f + empuje * dt
        x += (vx + sin(fase + vida * 2.3f) * vaiven) * dt
        y += vy * dt
    }

    val viva: Boolean get() = vida < duracion
}

/** Dibuja las partículas: un halo suave y un núcleo que pasa de [Llama.chispa] a [Llama.nucleo] al envejecer. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.dibujar(particulas: List<Particula>, llama: Llama) {
    for (p in particulas) {
        val t = (p.vida / p.duracion).coerceIn(0f, 1f)
        val parpadeo = 0.75f + 0.25f * sin(p.fase * 7f + p.vida * 14f)
        val alfa = (sin(PI.toFloat() * t) * parpadeo).coerceIn(0f, 1f)
        val centro = Offset(p.x, p.y)
        val halo = p.radio * p.halo
        drawCircle(
            Brush.radialGradient(listOf(llama.brillo.copy(alpha = alfa * 0.55f), Color.Transparent), centro, halo),
            halo, centro, blendMode = BlendMode.Plus
        )
        drawCircle(lerp(llama.chispa, llama.nucleo, t * t).copy(alpha = alfa), p.radio, centro, blendMode = BlendMode.Plus)
    }
}

/**
 * Bucle de partículas a ritmo de fotograma. Las partículas viven en una lista normal (no en estado de Compose):
 * solo un contador de fotograma, leído al dibujar, provoca el repintado, así que no se recompone nada.
 * El bucle arranca con cada [clave] y, si no es [continuo], se detiene cuando no quedan partículas.
 */
@Composable
private fun CampoParticulas(
    modifier: Modifier,
    llama: Llama,
    clave: Any?,
    continuo: Boolean,
    generar: (dt: Float, tamano: Size, particulas: MutableList<Particula>) -> Unit
) {
    val particulas = remember { ArrayList<Particula>() }
    val fotograma = remember { mutableLongStateOf(0L) }
    val tamano = remember { floatArrayOf(0f, 0f) }
    val generador by rememberUpdatedState(generar)
    LaunchedEffect(clave) {
        var anterior = 0L
        var primero = true
        while (continuo || primero || particulas.isNotEmpty()) {
            primero = false
            withFrameNanos { ahora ->
                val dt = if (anterior == 0L) 0f else ((ahora - anterior) / 1e9f).coerceAtMost(0.05f)
                anterior = ahora
                generador(dt, Size(tamano[0], tamano[1]), particulas)
                particulas.forEach { it.avanzar(dt) }
                particulas.removeAll { !it.viva }
                fotograma.longValue = ahora
            }
        }
    }
    Canvas(modifier) {
        tamano[0] = size.width
        tamano[1] = size.height
        fotograma.longValue
        dibujar(particulas, llama)
    }
}

/**
 * Brasas que suben desde el borde inferior y se apagan. [densidad] 1 son unas 10 brasas por segundo en un
 * ancho de teléfono. Sin animaciones del sistema no dibuja nada.
 */
@Composable
fun Brasas(llama: Llama, modifier: Modifier = Modifier, densidad: Float = 1f) {
    if (!animacionesActivas()) {
        Box(modifier)
        return
    }
    val px = LocalDensity.current.density
    val pendientes = remember { floatArrayOf(0f) }
    CampoParticulas(modifier, llama, clave = Unit, continuo = true) { dt, tamano, lista ->
        if (tamano.width <= 0f) return@CampoParticulas
        pendientes[0] += densidad * tamano.width / (110f * px) * dt
        while (pendientes[0] >= 1f) {
            pendientes[0] -= 1f
            val duracion = Random.nextFloat() * 3f + 2.5f
            lista += Particula(
                x = Random.nextFloat() * tamano.width,
                y = tamano.height + 6f * px,
                vx = 0f,
                vy = -(Random.nextFloat() * 50f + 35f) * px * llama.ascenso,
                duracion = duracion,
                radio = (Random.nextFloat() * 1.8f + 1.2f) * px,
                fase = Random.nextFloat() * 2f * PI.toFloat(),
                vaiven = (Random.nextFloat() * 12f + 6f) * px * llama.vaiven,
                frenado = 1f,
                empuje = -8f * px * llama.ascenso
            )
        }
    }
}

/**
 * Estallido de chispas desde el centro cada vez que [encendido] pasa a verdadero (no al aparecer ya encendido).
 */
@Composable
fun Estallido(encendido: Boolean, llama: Llama, modifier: Modifier = Modifier, chispas: Int = 40) {
    if (!animacionesActivas()) return
    val px = LocalDensity.current.density
    // Solo cuenta el paso de apagado a encendido; una tarjeta que aparece ya elegida no estalla
    val previo = remember { booleanArrayOf(encendido) }
    val contador = remember { intArrayOf(0) }
    if (encendido && !previo[0]) contador[0]++
    previo[0] = encendido
    val disparos = contador[0]
    if (disparos == 0) return
    val pendiente = remember(disparos) { booleanArrayOf(true) }
    CampoParticulas(modifier, llama, clave = disparos, continuo = false) { _, tamano, lista ->
        if (!pendiente[0] || tamano.width <= 0f) return@CampoParticulas
        pendiente[0] = false
        repeat(chispas) {
            val angulo = Random.nextFloat() * 2f * PI.toFloat()
            val velocidad = (Random.nextFloat() * 220f + 90f) * px
            lista += Particula(
                x = tamano.width / 2f,
                y = tamano.height / 2f,
                vx = cos(angulo) * velocidad,
                vy = sin(angulo) * velocidad,
                duracion = Random.nextFloat() * 0.6f + 0.6f,
                radio = (Random.nextFloat() * 1.6f + 1.2f) * px,
                fase = Random.nextFloat() * 2f * PI.toFloat(),
                vaiven = 10f * px * llama.vaiven,
                frenado = 0.15f,
                empuje = -120f * px * llama.ascenso,
                halo = 2.2f
            )
        }
    }
}
