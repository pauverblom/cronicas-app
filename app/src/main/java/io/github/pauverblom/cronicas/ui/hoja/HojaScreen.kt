package io.github.pauverblom.cronicas.ui.hoja

import androidx.compose.runtime.LaunchedEffect
import io.github.pauverblom.cronicas.ui.common.FormularioDialog
import io.github.pauverblom.cronicas.ui.ajustes.ListaTemas
import io.github.pauverblom.cronicas.data.coloresRetrato
import io.github.pauverblom.cronicas.data.coloresPropios
import io.github.pauverblom.cronicas.data.ColoresTema
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.ui.plataforma.rememberGuardarArchivo
import io.github.pauverblom.cronicas.ui.plataforma.rememberAviso
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.MoreVert
import io.github.pauverblom.cronicas.ui.hud.MenuHud
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pauverblom.cronicas.data.AjustesApp
import io.github.pauverblom.cronicas.data.Tema
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
import io.github.pauverblom.cronicas.ui.common.LocalAnimacionesVisibles
import io.github.pauverblom.cronicas.ui.foto.RetratoAvatar
import io.github.pauverblom.cronicas.ui.hud.BotonIcono
import io.github.pauverblom.cronicas.ui.hud.Cabecera
import io.github.pauverblom.cronicas.ui.hud.Cargando
import io.github.pauverblom.cronicas.ui.hud.Noche
import io.github.pauverblom.cronicas.ui.hud.PestanasHoja
import io.github.pauverblom.cronicas.ui.theme.CronicasMarcaTheme
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos
import kotlinx.coroutines.launch

enum class Pestana(val titulo: String) { RESUMEN("Resumen"), PERSONAJE("Personaje"), EQUIPO("Equipo"), MAGIA("Magia"), NOTAS("Notas") }

@Composable
fun HojaScreen(ajustes: AjustesApp, onVolver: () -> Unit, vm: HojaViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    // La ficha se pinta con su propio tema (o el de la app); mientras se elige, con el que se está probando
    var previa by remember { mutableStateOf<Personaje?>(null) }
    // Al guardarse el tema, la ficha ya trae el nuevo: se deja de probar (sin parpadeo entre medias)
    LaunchedEffect(ui?.personaje) { previa = null }
    CronicasMarcaTheme(ajustes.paraPersonaje(previa ?: ui?.personaje)) {
        Noche { HojaContenido(ui, vm, ajustes, onPrevia = { previa = it }, onVolver) }
    }
}

@Composable
private fun HojaContenido(ui: HojaUi?, vm: HojaViewModel, ajustes: AjustesApp, onPrevia: (Personaje?) -> Unit, onVolver: () -> Unit) {
    val aviso = rememberAviso()
    val c = Marca.colores
    var menu by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf(false) }
    var tema by remember { mutableStateOf(false) }
    val exportar = rememberGuardarArchivo("application/json") { archivo ->
        vm.exportar(archivo) { ok -> aviso(if (ok) "Personaje exportado" else "No se pudo exportar") }
    }
    val pestanas = Pestana.entries.filter { it != Pestana.MAGIA || ui?.muestraMagia == true }
    val pager = rememberPagerState { pestanas.size }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Cabecera(
            navegacion = {
                BotonIcono(onVolver, "Volver a la lista de personajes") { Icon(Icons.AutoMirrored.Sharp.ArrowBack, null, Modifier.size(24.dp)) }
            },
            acciones = {
                Box {
                    BotonIcono({ menu = true }, "Más acciones: tema, exportar o eliminar") { Icon(Icons.Sharp.MoreVert, null, Modifier.size(24.dp)) }
                    MenuHud(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Tema de la ficha…", style = Tipos.ui, color = c.tinta) }, onClick = {
                            menu = false
                            tema = true
                        })
                        DropdownMenuItem(text = { Text("Exportar", style = Tipos.ui, color = c.tinta) }, onClick = {
                            menu = false
                            exportar("${ui?.personaje?.nombre ?: "personaje"}.cronicas.json")
                        })
                        DropdownMenuItem(text = { Text("Eliminar", style = Tipos.ui, color = c.sangre) }, onClick = {
                            menu = false
                            borrar = true
                        })
                    }
                }
            }
        ) {
            if (ui != null) {
                RetratoAvatar(ui.personaje.fotoPath, ui.personaje.nombre, 52.dp, claseId = ui.personaje.claseId)
                Column(Modifier.weight(1f)) {
                    Text(
                        ui.personaje.nombre,
                        style = Tipos.tituloXl.copy(fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = 0.04.em),
                        color = c.tinta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${ui.hoja.nombreClase} · ${ui.hoja.nombreEspecie} · NV ${ui.hoja.nivel}",
                        style = Tipos.uiSm,
                        color = c.tintaSuave,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (ui == null) {
            Cargando()
            return@Column
        }
        PestanasHoja(
            pestanas.map { it.titulo },
            pager.currentPage.coerceAtMost(pestanas.lastIndex),
            onElegir = { i -> scope.launch { pager.animateScrollToPage(i) } }
        )
        Box(Modifier.weight(1f)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            // Las pestañas vecinas siguen compuestas para deslizar sin esperas, pero con sus animaciones quietas
            CompositionLocalProvider(LocalAnimacionesVisibles provides (page == pager.settledPage)) {
                when (pestanas[page]) {
                    Pestana.RESUMEN -> ResumenTab(ui, vm)
                    Pestana.PERSONAJE -> PersonajeTab(ui, vm)
                    Pestana.EQUIPO -> EquipoTab(ui, vm)
                    Pestana.MAGIA -> MagiaTab(ui, vm)
                    Pestana.NOTAS -> NotasTab(ui, vm)
                }
            }
        }
            // Sombra bajo el raíl: lo que sube se funde con la noche al pasar por debajo de las pestañas
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Brush.verticalGradient(0f to c.noche, 0.35f to c.noche.copy(alpha = 0.75f), 1f to c.noche.copy(alpha = 0f)))
            )
        }
    }

    if (tema && ui != null) {
        TemaFichaDialog(
            ui.personaje, ajustes,
            onPrevia = onPrevia,
            onGuardar = { t, colores -> vm.setTema(t, colores); tema = false },
            onDismiss = { tema = false; onPrevia(null) }
        )
    }

    if (borrar && ui != null) {
        ConfirmarDialog(
            titulo = "Eliminar personaje",
            texto = "¿Eliminar a ${ui.personaje.nombre}? Se borrará toda su ficha. Esta acción no se puede deshacer.",
            onDismiss = { borrar = false },
            onConfirm = { vm.eliminar(onVolver) }
        )
    }
}

/**
 * El tema de esta ficha: el de la app, uno de los temas, uno personalizado o los colores del retrato.
 * Mientras se elige, la ficha se ve con el tema que se está probando ([onPrevia]); [onGuardar] cierra el diálogo.
 */
@Composable
private fun TemaFichaDialog(
    p: Personaje,
    ajustes: AjustesApp,
    onPrevia: (Personaje?) -> Unit,
    onGuardar: (Tema?, ColoresTema?) -> Unit,
    onDismiss: () -> Unit
) {
    var elegido by remember { mutableStateOf(p.tema?.let { n -> Tema.entries.firstOrNull { it.name == n } }) }
    var colores by remember { mutableStateOf(p.coloresPropios() ?: ajustes.colores) }
    fun probar() = onPrevia(p.copy(tema = elegido?.name, colorAcento = colores.acento, colorMarco = colores.marco, colorFondo = colores.fondo))
    FormularioDialog("Tema de la ficha", onDismiss = onDismiss, onGuardar = {
        onGuardar(elegido, colores.takeIf { elegido == Tema.PERSONALIZADO })
    }) {
        ListaTemas(
            opciones = listOf<Tema?>(null) + Tema.entries,
            elegido = elegido,
            onElegir = { elegido = it; probar() },
            colores = colores,
            onColores = { colores = it; probar() },
            coloresRetrato = p.coloresRetrato(),
            descripcionApp = "El tema elegido en Ajustes (${ajustes.tema.etiqueta})"
        )
    }
}
