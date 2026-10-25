package io.github.pauverblom.cronicas.ui.hoja

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pauverblom.cronicas.data.AjustesApp
import io.github.pauverblom.cronicas.data.Tema
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
import io.github.pauverblom.cronicas.ui.foto.RetratoAvatar
import io.github.pauverblom.cronicas.ui.theme.CronicasMarcaTheme
import io.github.pauverblom.cronicas.ui.theme.LocalColoresHoja
import kotlinx.coroutines.launch

enum class Pestana(val titulo: String) { RESUMEN("Resumen"), PERSONAJE("Personaje"), EQUIPO("Equipo"), MAGIA("Magia"), NOTAS("Notas") }

@Composable
fun colorPg(actuales: Int, maximos: Int): Color {
    val c = LocalColoresHoja.current
    if (maximos <= 0 || actuales <= 0) return c.pgCero
    val ratio = actuales.toFloat() / maximos
    return when {
        ratio <= 0.25f -> c.pgMal
        ratio <= 0.5f -> c.pgMedio
        else -> c.pgBien
    }
}

@Composable
fun HojaScreen(ajustes: AjustesApp, onVolver: () -> Unit, vm: HojaViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    val semilla = ui?.personaje?.colorSemilla
    // Con "Desde retrato" la ficha entera adopta los colores del personaje
    if (ajustes.tema == Tema.RETRATO) {
        CronicasMarcaTheme(ajustes, semilla) { HojaContenido(ui, vm, onVolver) }
    } else {
        HojaContenido(ui, vm, onVolver)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaContenido(ui: HojaUi?, vm: HojaViewModel, onVolver: () -> Unit) {
    val context = LocalContext.current
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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                    title = {
                        if (ui != null) Row(verticalAlignment = Alignment.CenterVertically) {
                            RetratoAvatar(ui.personaje.fotoPath, ui.personaje.nombre, 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(ui.personaje.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${ui.hoja.nombreClase} ${ui.hoja.nivel} · ${ui.hoja.nombreEspecie}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Más opciones") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Exportar") }, onClick = {
                                menu = false
                                exportLauncher.launch("${ui?.personaje?.nombre ?: "personaje"}.cronicas.json")
                            })
                            DropdownMenuItem(text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) }, onClick = {
                                menu = false
                                borrar = true
                            })
                        }
                    }
                )
                PrimaryScrollableTabRow(selectedTabIndex = pager.currentPage.coerceAtMost(pestanas.lastIndex), edgePadding = 8.dp) {
                    pestanas.forEachIndexed { i, p ->
                        Tab(
                            selected = pager.currentPage == i,
                            onClick = { scope.launch { pager.animateScrollToPage(i) } },
                            text = { Text(p.titulo) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (ui == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().padding(padding), beyondViewportPageCount = 1) { page ->
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
