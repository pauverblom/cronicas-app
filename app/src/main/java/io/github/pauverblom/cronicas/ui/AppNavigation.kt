package io.github.pauverblom.cronicas.ui

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

@Serializable
data class RutaHoja(val personajeId: Long)

@Composable
fun AppNavigation(ajustes: AjustesApp) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = RutaHome) {
        composable<RutaHome> {
            HomeScreen(
                onAbrir = { nav.navigate(RutaHoja(it)) },
                onNuevo = { nav.navigate(RutaWizard) },
                onAjustes = { nav.navigate(RutaAjustes) }
            )
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
            HojaScreen(ajustes = ajustes, onVolver = { nav.popBackStack() })
        }
    }
}
