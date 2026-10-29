import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import coil3.compose.setSingletonImageLoaderFactory
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.github.pauverblom.cronicas.CronicasApp
import io.github.pauverblom.cronicas.data.AjustesRepository
import io.github.pauverblom.cronicas.data.Almacen
import io.github.pauverblom.cronicas.data.CampanasRepository
import io.github.pauverblom.cronicas.data.Repository
import io.github.pauverblom.cronicas.data.catalogoConjurosDesdeJson
import io.github.pauverblom.cronicas.data.sync.Sincronizador
import io.github.pauverblom.cronicas.ui.AppNavigation
import io.github.pauverblom.cronicas.ui.hud.Noche
import io.github.pauverblom.cronicas.ui.plataforma.AvisoHost
import io.github.pauverblom.cronicas.ui.theme.CronicasMarcaTheme
import io.github.pauverblom.cronicas.ui.theme.FuentesWeb
import io.github.pauverblom.cronicas.web.Nube
import io.github.pauverblom.cronicas.web.jsListo
import io.github.pauverblom.cronicas.web.recursos.Res
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    val scope = MainScope()
    scope.launch {
        FuentesWeb.cargar()
        val catalogo = catalogoConjurosDesdeJson(Res.readBytes("files/conjuros.json").decodeToString())
            .mapIndexed { i, c -> c.copy(id = i + 1L) }
        val almacen = Almacen(catalogo, scope)
        val app = CronicasApp(
            repository = Repository(almacen),
            ajustes = AjustesRepository(),
            sincronizador = Sincronizador(almacen, Nube.disponible(), scope),
            campanas = CampanasRepository()
        )
        app.sincronizador.iniciar()
        jsListo()
        // En #app, que deja libre la barra de estado y el indicador de inicio (ver index.html)
        ComposeViewport(document.getElementById("app")!!) {
            setSingletonImageLoaderFactory { contexto ->
                ImageLoader.Builder(contexto).components { add(KtorNetworkFetcherFactory()) }.build()
            }
            val ajustes by app.ajustes.ajustes.collectAsState(initial = app.ajustes.actuales)
            CronicasMarcaTheme(ajustes) {
                // La app aparece fundiéndose y creciendo un poco. Además deja preparado en la GPU lo que necesitan las
                // capas translúcidas y escaladas, que si no se prepararía al abrir la primera ficha, en plena transición
                val aparicion = remember { Animatable(0f) }
                LaunchedEffect(Unit) { aparicion.animateTo(1f, tween(350)) }
                Box(Modifier.fillMaxSize().graphicsLayer {
                    alpha = aparicion.value
                    val e = 0.97f + 0.03f * aparicion.value
                    scaleX = e
                    scaleY = e
                }) {
                    Noche { AppNavigation(app, ajustes) }
                }
            }
            AvisoHost()
        }
    }
}
