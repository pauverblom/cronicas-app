package io.github.pauverblom.cronicas.ui

import io.github.pauverblom.cronicas.ui.hud.Pantallas
import io.github.pauverblom.cronicas.ui.hud.ConTransicion
import io.github.pauverblom.cronicas.ui.common.animacionesActivas
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.pauverblom.cronicas.CronicasApp
import io.github.pauverblom.cronicas.data.AjustesApp
import io.github.pauverblom.cronicas.ui.ajustes.AjustesScreen
import io.github.pauverblom.cronicas.ui.ajustes.AjustesViewModel
import io.github.pauverblom.cronicas.ui.hoja.HojaScreen
import io.github.pauverblom.cronicas.ui.hoja.HojaViewModel
import io.github.pauverblom.cronicas.ui.home.HomeScreen
import io.github.pauverblom.cronicas.ui.home.HomeViewModel
import io.github.pauverblom.cronicas.ui.wizard.WizardScreen
import io.github.pauverblom.cronicas.ui.wizard.WizardViewModel
import kotlinx.serialization.Serializable

@Serializable
object RutaHome

@Serializable
object RutaWizard

@Serializable
object RutaAjustes

@Serializable
data class RutaHoja(val personajeId: Long)

/** Igual que en Android, pero creando los ViewModels a mano (en la web no hay fábrica por reflexión). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(app: CronicasApp, ajustes: AjustesApp) {
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
                        onNuevo = { nav.navigate(RutaWizard) },
                        onAjustes = { nav.navigate(RutaAjustes) },
                        ajustes = ajustes,
                        vm = viewModel { HomeViewModel(app) }
                    )
                }
            }
            composable<RutaWizard> {
                WizardScreen(
                    onCancelar = { nav.popBackStack() },
                    onCreado = { id ->
                        nav.navigate(RutaHoja(id)) { popUpTo(RutaHome) }
                    },
                    vm = viewModel { WizardViewModel(app) }
                )
            }
            composable<RutaAjustes> {
                AjustesScreen(onVolver = { nav.popBackStack() }, vm = viewModel { AjustesViewModel(app) })
            }
            composable<RutaHoja> {
                ConTransicion(this@SharedTransitionLayout, this) {
                    HojaScreen(
                        ajustes = ajustes,
                        onVolver = { nav.popBackStack() },
                        vm = viewModel { HojaViewModel(app, createSavedStateHandle()) }
                    )
                }
            }
        }
    }
}
