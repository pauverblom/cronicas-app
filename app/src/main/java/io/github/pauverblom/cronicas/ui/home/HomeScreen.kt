package io.github.pauverblom.cronicas.ui.home

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FileOpen
import androidx.compose.material.icons.sharp.Settings
import io.github.pauverblom.cronicas.ui.hud.MenuHud
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pauverblom.cronicas.CronicasApp
import io.github.pauverblom.cronicas.data.Backup
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.ResumenPersonaje
import io.github.pauverblom.cronicas.rules.data.Clases
import io.github.pauverblom.cronicas.rules.data.Especies
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
import io.github.pauverblom.cronicas.ui.common.llamaDe
import io.github.pauverblom.cronicas.ui.foto.Retratos
import io.github.pauverblom.cronicas.ui.hud.Boton
import io.github.pauverblom.cronicas.ui.hud.BotonIcono
import io.github.pauverblom.cronicas.ui.hud.Cabecera
import io.github.pauverblom.cronicas.ui.hud.Cargando
import io.github.pauverblom.cronicas.ui.hud.EntradaLista
import io.github.pauverblom.cronicas.ui.hud.EstadoVacio
import io.github.pauverblom.cronicas.ui.hud.Glifo
import io.github.pauverblom.cronicas.ui.hud.TituloPantalla
import io.github.pauverblom.cronicas.ui.hud.Variante
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as CronicasApp).repository

    val personajes = repo.resumenes().map<List<ResumenPersonaje>, List<ResumenPersonaje>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun eliminar(p: Personaje) = viewModelScope.launch { repo.deletePersonaje(p) }

    fun exportar(pid: Long, uri: Uri, onFin: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = runCatching {
            val json = Backup.exportar(repo, pid)
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) }
            }
        }.isSuccess
        onFin(ok)
    }

    fun importar(uri: Uri, onFin: (Long?) -> Unit) = viewModelScope.launch {
        val app = getApplication<Application>()
        val id = runCatching {
            val texto = withContext(Dispatchers.IO) {
                app.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
            Backup.importar(repo, texto, Retratos.directorio(app))
        }.getOrNull()
        onFin(id)
    }
}

@Composable
fun HomeScreen(
    onAbrir: (Long) -> Unit,
    onNuevo: () -> Unit,
    onAjustes: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val personajes by vm.personajes.collectAsState()
    val context = LocalContext.current
    var exportando by remember { mutableStateOf<Personaje?>(null) }
    var borrando by remember { mutableStateOf<Personaje?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val p = exportando ?: return@rememberLauncherForActivityResult
        exportando = null
        if (uri != null) vm.exportar(p.id, uri) { ok ->
            Toast.makeText(context, if (ok) "Personaje exportado" else "No se pudo exportar", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importar(uri) { id ->
            if (id != null) onAbrir(id) else Toast.makeText(context, "El archivo no es un personaje válido", Toast.LENGTH_LONG).show()
        }
    }
    val importar = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }

    Column(Modifier.fillMaxSize()) {
        Cabecera(
            acciones = {
                BotonIcono(importar, "Importar personaje") { Icon(Icons.Sharp.FileOpen, null, Modifier.size(24.dp)) }
                BotonIcono(onAjustes, "Ajustes") { Icon(Icons.Sharp.Settings, null, Modifier.size(24.dp)) }
            }
        ) { TituloPantalla("Crónicas de la Marca") }
        val lista = personajes
        when {
            lista == null -> Cargando()
            lista.isEmpty() -> SinPersonajes(onNuevo, importar)
            else -> {
                Boton(
                    "Nuevo personaje", onNuevo,
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).widthIn(max = 420.dp).align(Alignment.CenterHorizontally),
                    variante = Variante.DORADO, icono = Glifo.MAS, bloque = true
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(340.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(lista, key = { it.personaje.id }) { r ->
                        FilaPersonaje(
                            r,
                            onClick = { onAbrir(r.personaje.id) },
                            onExportar = {
                                exportando = r.personaje
                                exportLauncher.launch("${r.personaje.nombre.ifBlank { "personaje" }}.cronicas.json")
                            },
                            onEliminar = { borrando = r.personaje }
                        )
                    }
                    item { Box(Modifier.navigationBarsPadding()) }
                }
            }
        }
    }

    borrando?.let { p ->
        ConfirmarDialog(
            titulo = "Eliminar personaje",
            texto = "¿Eliminar a ${p.nombre}? Se borrará toda su ficha. Esta acción no se puede deshacer.",
            onDismiss = { borrando = null },
            onConfirm = { vm.eliminar(p) }
        )
    }
}

@Composable
private fun SinPersonajes(onNuevo: () -> Unit, onImportar: () -> Unit) {
    Box(Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        EstadoVacio(
            "Bienvenido a la Marca del Este",
            "Crea tu primer aventurero para empezar su ficha."
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Boton("Crear personaje", onNuevo, variante = Variante.DORADO)
                Boton("Importar personaje", onImportar)
            }
        }
    }
}

@Composable
private fun FilaPersonaje(r: ResumenPersonaje, onClick: () -> Unit, onExportar: () -> Unit, onEliminar: () -> Unit) {
    val p = r.personaje
    var menu by remember { mutableStateOf(false) }
    val c = Marca.colores
    val clase = Clases.porId(p.claseId)?.nombre ?: p.claseNombre.ifBlank { null }
    val especie = Especies.porId(p.especieId)?.nombre ?: p.especieNombre
    // Igual que en la cabecera de la ficha: «Druida · Semiorco · NV 1»
    val linea = listOfNotNull(clase ?: "Sin clase", especie.ifBlank { null }, "NV ${p.nivel}").joinToString(" · ")
    Box {
        EntradaLista(
            nombre = p.nombre,
            retrato = remember(p.fotoPath) { p.fotoPath?.let(::File)?.takeIf { it.exists() } },
            linea = linea,
            pg = r.estado?.pgActuales,
            pgMax = r.estado?.pgMaximos,
            llama = llamaDe(p.claseId),
            onClick = onClick,
            onClickLargo = { menu = true }
        )
        MenuHud(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Exportar", style = Tipos.ui, color = c.tinta) }, onClick = { menu = false; onExportar() })
            DropdownMenuItem(text = { Text("Eliminar", style = Tipos.ui, color = c.sangre) }, onClick = { menu = false; onEliminar() })
        }
    }
}
