package io.github.pauverblom.cronicas

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.pauverblom.cronicas.data.AjustesApp
import io.github.pauverblom.cronicas.ui.AppNavigation
import io.github.pauverblom.cronicas.ui.theme.CronicasMarcaTheme
import io.github.pauverblom.cronicas.ui.theme.esTemaOscuro

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ajustesRepo = (application as CronicasApp).ajustes
        setContent {
            val ajustes by ajustesRepo.ajustes.collectAsState(initial = AjustesApp())
            val oscuro = esTemaOscuro(ajustes)
            // Iconos de la barra de estado claros u oscuros según el tema elegido, no el del sistema
            DisposableEffect(oscuro) {
                val estilo = if (oscuro) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose {}
            }
            CronicasMarcaTheme(ajustes) {
                AppNavigation(ajustes)
            }
        }
    }
}
