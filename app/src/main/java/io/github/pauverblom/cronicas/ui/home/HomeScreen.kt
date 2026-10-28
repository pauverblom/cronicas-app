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
import androidx.compose.material.icons.sharp.Sync
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pauverblom.cronicas.CronicasApp
import io.github.pauverblom.cronicas.data.Backup
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.ResumenPersonaje
import io.github.pauverblom.cronicas.data.sync.Sincronizador
import io.github.pauverblom.cronicas.rules.data.Clases
import io.github.pauverblom.cronicas.rules.data.Especies
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
import io.github.pauverblom.cronicas.ui.common.FormularioDialog
import io.github.pauverblom.cronicas.ui.common.llamaDe
import io.github.pauverblom.cronicas.ui.foto.Retratos
import io.github.pauverblom.cronicas.ui.hud.Boton
import io.github.pauverblom.cronicas.ui.hud.BotonIcono
import io.github.pauverblom.cronicas.ui.hud.Cabecera
import io.github.pauverblom.cronicas.ui.hud.Campo
import io.github.pauverblom.cronicas.ui.hud.Cargando
import io.github.pauverblom.cronicas.ui.hud.Dialogo
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
    private val sync = (app as CronicasApp).sincronizador

    val syncDisponible = sync.disponible
    val vinculos = sync.vinculos

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

    fun activarSync(pid: Long, onFin: (String?) -> Unit) = viewModelScope.launch {
        onFin(runCatching { sync.activar(pid) }.getOrNull())
    }

    /** Devuelve el id del personaje vinculado, o el mensaje de error. */
    fun vincular(codigo: String, onFin: (Result<Long>) -> Unit) = viewModelScope.launch {
        onFin(runCatching { sync.vincular(codigo) })
    }

    fun desvincular(pid: Long) = sync.desvincular(pid)

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
    val vinculos by vm.vinculos.collectAsState()
    val context = LocalContext.current
    var exportando by remember { mutableStateOf<Personaje?>(null) }
    var borrando by remember { mutableStateOf<Personaje?>(null) }
    var sincronizando by remember { mutableStateOf<Personaje?>(null) }
    var vinculando by remember { mutableStateOf(false) }

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
                if (vm.syncDisponible) {
                    BotonIcono({ vinculando = true }, "Vincular personaje") { Icon(Icons.Sharp.Sync, null, Modifier.size(24.dp)) }
                }
                BotonIcono(importar, "Importar personaje") { Icon(Icons.Sharp.FileOpen, null, Modifier.size(24.dp)) }
                BotonIcono(onAjustes, "Ajustes") { Icon(Icons.Sharp.Settings, null, Modifier.size(24.dp)) }
            }
        ) { TituloPantalla("Crónicas de la Marca") }
        val lista = personajes
        when {
            lista == null -> Cargando()
            lista.isEmpty() -> SinPersonajes(onNuevo, importar, if (vm.syncDisponible) ({ vinculando = true }) else null)
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
                            onEliminar = { borrando = r.personaje },
                            onSincronizar = if (vm.syncDisponible) ({ sincronizando = r.personaje }) else null
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
            texto = if (p.id in vinculos) {
                "¿Eliminar a ${p.nombre} de este dispositivo? Se borrará su ficha aquí y dejará de sincronizarse. " +
                    "En los demás dispositivos vinculados seguirá estando."
            } else {
                "¿Eliminar a ${p.nombre}? Se borrará toda su ficha. Esta acción no se puede deshacer."
            },
            onDismiss = { borrando = null },
            onConfirm = { vm.eliminar(p) }
        )
    }

    sincronizando?.let { p ->
        SincronizarDialog(
            personaje = p,
            codigo = vinculos[p.id],
            onActivar = { onFin ->
                vm.activarSync(p.id) { codigo ->
                    onFin()
                    if (codigo == null) {
                        Toast.makeText(context, "No se pudo activar. ¿Hay conexión a internet?", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDesvincular = { vm.desvincular(p.id) },
            onDismiss = { sincronizando = null }
        )
    }

    if (vinculando) {
        VincularDialog(
            onVincular = { codigo, onError ->
                vm.vincular(codigo) { r ->
                    r.onSuccess { id ->
                        vinculando = false
                        onAbrir(id)
                    }.onFailure { e ->
                        onError(
                            when (e) {
                                is IllegalArgumentException, is IllegalStateException -> e.message ?: "No se pudo vincular"
                                else -> "No se pudo vincular. ¿Hay conexión a internet?"
                            }
                        )
                    }
                }
            },
            onDismiss = { vinculando = false }
        )
    }
}

/** Activa la sincronización de un personaje o muestra su código para vincularlo en otro dispositivo. */
@Composable
private fun SincronizarDialog(
    personaje: Personaje,
    codigo: String?,
    onActivar: (onFin: () -> Unit) -> Unit,
    onDesvincular: () -> Unit,
    onDismiss: () -> Unit
) {
    var activando by remember { mutableStateOf(false) }
    val portapapeles = LocalClipboardManager.current
    val context = LocalContext.current
    val c = Marca.colores
    if (codigo == null) {
        Dialogo(
            "Sincronizar",
            onDismiss = onDismiss,
            acciones = {
                Boton("Cancelar", onDismiss, variante = Variante.DISCRETO)
                Boton(if (activando) "Activando…" else "Activar", {
                    activando = true
                    onActivar { activando = false }
                }, variante = Variante.DORADO, activo = !activando)
            }
        ) {
            Text("Sincroniza la ficha de ${personaje.nombre} con tus otros dispositivos: lo que cambies en uno aparecerá en los demás.")
            Text("Al activarla tendrás un código para vincular el personaje en el otro dispositivo.", style = Tipos.uiSm, color = c.tintaSuave)
        }
    } else {
        val visible = Sincronizador.formatear(codigo)
        Dialogo(
            "Sincronizado",
            onDismiss = onDismiss,
            extra = {
                Boton("Dejar de sincronizar", { onDesvincular(); onDismiss() }, variante = Variante.PELIGRO_DISCRETO)
            },
            acciones = {
                Boton("Copiar", {
                    portapapeles.setText(AnnotatedString(visible))
                    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                })
                Boton("Cerrar", onDismiss, variante = Variante.DORADO)
            }
        ) {
            Text("Para tener a ${personaje.nombre} en otro dispositivo, pulsa allí «Vincular personaje» y escribe este código:")
            Text(visible, style = Tipos.tituloLg, color = c.oro, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally))
            Text("Comparte el código solo con quien quieras que pueda ver y cambiar la ficha.", style = Tipos.uiSm, color = c.tintaSuave)
        }
    }
}

/** Pide un código de sincronización y descarga ese personaje. */
@Composable
private fun VincularDialog(onVincular: (codigo: String, onError: (String) -> Unit) -> Unit, onDismiss: () -> Unit) {
    var codigo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var vinculando by remember { mutableStateOf(false) }
    val completo = Sincronizador.normalizar(codigo).length == Sincronizador.LARGO_CODIGO
    FormularioDialog(
        titulo = "Vincular personaje",
        onDismiss = onDismiss,
        guardar = if (vinculando) "Vinculando…" else "Vincular",
        guardarActivo = completo && !vinculando,
        onGuardar = {
            vinculando = true
            error = null
            onVincular(codigo) { mensaje ->
                vinculando = false
                error = mensaje
            }
        }
    ) {
        Text("Escribe el código que aparece en el otro dispositivo, en «Sincronizar» del personaje.")
        Campo(
            codigo,
            { codigo = it; error = null },
            "Código",
            marcador = "XXXX-XXXX-XXXX",
            error = error,
            enfocarAlAbrir = true
        )
    }
}

@Composable
private fun SinPersonajes(onNuevo: () -> Unit, onImportar: () -> Unit, onVincular: (() -> Unit)?) {
    Box(Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        EstadoVacio(
            "Bienvenido a la Marca del Este",
            "Crea tu primer aventurero para empezar su ficha."
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Boton("Crear personaje", onNuevo, variante = Variante.DORADO)
                Boton("Importar personaje", onImportar)
                if (onVincular != null) Boton("Vincular personaje", onVincular)
            }
        }
    }
}

@Composable
private fun FilaPersonaje(
    r: ResumenPersonaje,
    onClick: () -> Unit,
    onExportar: () -> Unit,
    onEliminar: () -> Unit,
    onSincronizar: (() -> Unit)?
) {
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
            if (onSincronizar != null) {
                DropdownMenuItem(text = { Text("Sincronizar", style = Tipos.ui, color = c.tinta) }, onClick = { menu = false; onSincronizar() })
            }
            DropdownMenuItem(text = { Text("Exportar", style = Tipos.ui, color = c.tinta) }, onClick = { menu = false; onExportar() })
            DropdownMenuItem(text = { Text("Eliminar", style = Tipos.ui, color = c.sangre) }, onClick = { menu = false; onEliminar() })
        }
    }
}
