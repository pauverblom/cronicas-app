package io.github.pauverblom.cronicas.ui.hud

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.ScaleToBounds
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/*
 * Transiciones entre pantallas. Al abrir un personaje, su retrato y su nombre vuelan de la tarjeta de la lista a la
 * cabecera de la ficha (elementos compartidos), mientras la ficha aparece creciendo un poco; al volver, al revés.
 */

private const val DURACION = 420

/** La transición en curso: la capa de elementos compartidos y la pantalla que entra o sale. */
@OptIn(ExperimentalSharedTransitionApi::class)
class TransicionCompartida(val compartida: SharedTransitionScope, val visibilidad: AnimatedVisibilityScope)

val LocalTransicion = compositionLocalOf<TransicionCompartida?> { null }

/** Da a [contenido] la transición de su pantalla, para que sus elementos compartidos la sigan. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ConTransicion(compartida: SharedTransitionScope, visibilidad: AnimatedVisibilityScope, contenido: @Composable () -> Unit) =
    CompositionLocalProvider(LocalTransicion provides TransicionCompartida(compartida, visibilidad), content = contenido)

/** Un elemento que vuela de una pantalla a otra (el retrato), si hay transición; si no, nada. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.compartido(clave: String): Modifier {
    val t = LocalTransicion.current ?: return this
    return with(t.compartida) {
        this@compartido.sharedElement(
            rememberSharedContentState(clave), t.visibilidad,
            boundsTransform = { _, _ -> tween(DURACION, easing = FastOutSlowInEasing) }
        )
    }
}

/** Como [compartido], para un texto que cambia de tamaño (el nombre): se escala y funde de uno a otro. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.textoCompartido(clave: String): Modifier {
    val t = LocalTransicion.current ?: return this
    return with(t.compartida) {
        this@textoCompartido.sharedBounds(
            rememberSharedContentState(clave), t.visibilidad,
            enter = fadeIn(tween(DURACION)), exit = fadeOut(tween(DURACION)),
            boundsTransform = { _, _ -> tween(DURACION, easing = FastOutSlowInEasing) },
            resizeMode = ScaleToBounds()
        )
    }
}

/** Cómo entran y salen las pantallas al navegar; sin animaciones del sistema, de golpe. */
object Pantallas {
    fun entrar(anima: Boolean): EnterTransition =
        if (!anima) EnterTransition.None
        else fadeIn(tween(DURACION - 80, delayMillis = 60)) + scaleIn(tween(DURACION, easing = FastOutSlowInEasing), initialScale = 0.94f)

    fun salir(anima: Boolean): ExitTransition =
        if (!anima) ExitTransition.None else fadeOut(tween(DURACION - 120))

    fun volver(anima: Boolean): EnterTransition =
        if (!anima) EnterTransition.None else fadeIn(tween(DURACION - 80, delayMillis = 60))

    fun cerrar(anima: Boolean): ExitTransition =
        if (!anima) ExitTransition.None
        else fadeOut(tween(DURACION - 120)) + scaleOut(tween(DURACION, easing = FastOutSlowInEasing), targetScale = 0.94f)
}
