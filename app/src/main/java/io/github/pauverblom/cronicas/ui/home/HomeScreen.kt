package io.github.pauverblom.cronicas.ui.home

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import io.github.pauverblom.cronicas.ui.foto.RetratoAvatar
import io.github.pauverblom.cronicas.ui.foto.RetratoTarjeta
import io.github.pauverblom.cronicas.ui.foto.Retratos
import io.github.pauverblom.cronicas.ui.hoja.colorPg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crónicas de la Marca", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Importar personaje")
                    }
                    IconButton(onClick = onAjustes) { Icon(Icons.Default.Settings, contentDescription = "Ajustes") }
                }
            )
        },
        floatingActionButton = {
            if (!personajes.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onNuevo,
                    icon = { Icon(Icons.Default.PersonAdd, null) },
                    text = { Text("Nuevo personaje") }
                )
            }
        }
    ) { padding ->
        val lista = personajes
        when {
            lista == null -> Box(Modifier.fillMaxSize().padding(padding))
            lista.isEmpty() -> SinPersonajes(Modifier.padding(padding), onNuevo)
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lista, key = { it.personaje.id }) { r ->
                    TarjetaPersonaje(
                        r,
                        onClick = { onAbrir(r.personaje.id) },
                        onExportar = {
                            exportando = r.personaje
                            exportLauncher.launch("${r.personaje.nombre.ifBlank { "personaje" }}.cronicas.json")
                        },
                        onEliminar = { borrando = r.personaje }
                    )
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
private fun SinPersonajes(modifier: Modifier, onNuevo: () -> Unit) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Bienvenido a la Marca del Este", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Crea tu primer aventurero para empezar su ficha.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onNuevo) {
                Icon(Icons.Default.PersonAdd, null)
                Text("Crear personaje", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TarjetaPersonaje(r: ResumenPersonaje, onClick: () -> Unit, onExportar: () -> Unit, onEliminar: () -> Unit) {
    val p = r.personaje
    var menu by remember { mutableStateOf(false) }
    val clase = Clases.porId(p.claseId)?.nombre ?: p.claseNombre.ifBlank { "Sin clase" }
    val especie = Especies.porId(p.especieId)?.nombre ?: p.especieNombre
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onClick, onLongClick = { menu = true }),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(Modifier.padding(10.dp)) {
                RetratoAvatar(
                    p.fotoPath, p.nombre, 160.dp,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    shape = RetratoTarjeta
                )
                Spacer(Modifier.height(8.dp))
                Text(p.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "$clase ${p.nivel}" + if (especie.isNotBlank()) " · $especie" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                r.estado?.let { e ->
                    Spacer(Modifier.height(6.dp))
                    val ratio = if (e.pgMaximos > 0) e.pgActuales.toFloat() / e.pgMaximos else 0f
                    val color = colorPg(e.pgActuales, e.pgMaximos)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                .background(color)
                        )
                    }
                    Text(
                        "PG ${e.pgActuales}/${e.pgMaximos}",
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Exportar") }, onClick = { menu = false; onExportar() })
            DropdownMenuItem(text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onEliminar() })
        }
    }
}
