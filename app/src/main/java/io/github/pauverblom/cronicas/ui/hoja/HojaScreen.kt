package io.github.pauverblom.cronicas.ui.hoja

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pauverblom.cronicas.data.AjustesApp
import io.github.pauverblom.cronicas.data.Tema
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
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
    val semilla = ui?.personaje?.colorSemilla
    // Con "Desde retrato" la ficha entera adopta los colores del personaje, noche incluida
    if (ajustes.tema == Tema.RETRATO) {
        CronicasMarcaTheme(ajustes, semilla) { Noche { HojaContenido(ui, vm, onVolver) } }
    } else {
        HojaContenido(ui, vm, onVolver)
    }
}

@Composable
private fun HojaContenido(ui: HojaUi?, vm: HojaViewModel, onVolver: () -> Unit) {
    val context = LocalContext.current
    val c = Marca.colores
    var menu by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportar(uri) { ok ->
            Toast.makeText(context, if (ok) "Personaje exportado" else "No se pudo exportar", Toast.LENGTH_SHORT).show()
        }
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
                    BotonIcono({ menu = true }, "Más acciones: exportar o eliminar") { Icon(Icons.Sharp.MoreVert, null, Modifier.size(24.dp)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Exportar", style = Tipos.ui, color = c.tinta) }, onClick = {
                            menu = false
                            exportLauncher.launch("${ui?.personaje?.nombre ?: "personaje"}.cronicas.json")
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
                        "${ui.hoja.nombreClase} ${ui.hoja.nivel} · ${ui.hoja.nombreEspecie}",
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
        HorizontalPager(state = pager, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
            when (pestanas[page]) {
                Pestana.RESUMEN -> ResumenTab(ui, vm)
                Pestana.PERSONAJE -> PersonajeTab(ui, vm)
                Pestana.EQUIPO -> EquipoTab(ui, vm)
                Pestana.MAGIA -> MagiaTab(ui, vm)
                Pestana.NOTAS -> NotasTab(ui, vm)
            }
        }
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
