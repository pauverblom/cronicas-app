package io.github.pauverblom.cronicas.ui

import io.github.pauverblom.cronicas.ui.hud.Pantallas
import io.github.pauverblom.cronicas.ui.hud.ConTransicion
import io.github.pauverblom.cronicas.ui.common.animacionesActivas
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.pauverblom.cronicas.data.AjustesApp
import io.github.pauverblom.cronicas.ui.ajustes.AjustesScreen
import io.github.pauverblom.cronicas.ui.hoja.HojaScreen
import io.github.pauverblom.cronicas.ui.home.HomeScreen
import io.github.pauverblom.cronicas.ui.wizard.WizardScreen
import kotlinx.serialization.Serializable

@Serializable
object RutaHome

@Serializable
object RutaWizard

@Serializable
object RutaAjustes

/**
 * La ficha de [personajeId]. Nombre, retrato y clase llegan ya desde la lista para pintar la cabecera desde el
 * primer fotograma, mientras se carga la ficha: así el retrato tiene adónde volar en la transición.
 */
@Serializable
data class RutaHoja(val personajeId: Long, val nombre: String = "", val foto: String? = null, val claseId: String? = null)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(ajustes: AjustesApp) {
    val nav = rememberNavController()
    val anima = animacionesActivas()
    SharedTransitionLayout {
        NavHost(
            navController = nav,
            startDestination = RutaHome,
            enterTransition = { Pantallas.entrar(anima) },
            exitTransition = { Pantallas.salir(anima) },
            popEnterTransition = { Pantallas.volver(anima) },
            popExitTransition = { Pantallas.cerrar(anima) }
        ) {
            composable<RutaHome> {
                ConTransicion(this@SharedTransitionLayout, this) {
                    HomeScreen(
                        onAbrir = { nav.navigate(RutaHoja(it)) },
                    onAbrirFicha = { p -> nav.navigate(RutaHoja(p.id, p.nombre, p.fotoPath, p.claseId)) },
                        onNuevo = { nav.navigate(RutaWizard) },
                        onAjustes = { nav.navigate(RutaAjustes) },
                        ajustes = ajustes
                    )
                }
            }
            composable<RutaWizard> {
                WizardScreen(
                    onCancelar = { nav.popBackStack() },
                    onCreado = { id ->
                        nav.navigate(RutaHoja(id)) { popUpTo(RutaHome) }
                    }
                )
            }
            composable<RutaAjustes> {
                AjustesScreen(onVolver = { nav.popBackStack() })
            }
            composable<RutaHoja> {
                ConTransicion(this@SharedTransitionLayout, this) {
                    HojaScreen(ajustes = ajustes, onVolver = { nav.popBackStack() })
                }
            }
        }
    }
}
