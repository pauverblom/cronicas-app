package io.github.pauverblom.cronicas.ui.ajustes

import android.app.Application
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pauverblom.cronicas.CronicasApp
import io.github.pauverblom.cronicas.data.AjustesApp
import io.github.pauverblom.cronicas.data.Modo
import io.github.pauverblom.cronicas.data.Tema
import io.github.pauverblom.cronicas.ui.common.SeccionCard
import kotlinx.coroutines.launch

class AjustesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as CronicasApp).ajustes
    val ajustes = repo.ajustes
    fun setTema(t: Tema) = viewModelScope.launch { repo.setTema(t) }
    fun setModo(m: Modo) = viewModelScope.launch { repo.setModo(m) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(onVolver: () -> Unit, vm: AjustesViewModel = viewModel()) {
    val ajustes by vm.ajustes.collectAsState(initial = AjustesApp())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SeccionCard("Tema") {
                Tema.entries.forEach { t ->
                    val disponible = t != Tema.MATERIAL_YOU || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = ajustes.tema == t, enabled = disponible, role = Role.RadioButton) { vm.setTema(t) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = ajustes.tema == t, onClick = null, enabled = disponible)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(t.etiqueta, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (disponible) t.descripcion else "Necesita Android 12 o superior",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            SeccionCard("Modo") {
                val fijo = ajustes.tema == Tema.PERGAMINO_OSCURO || ajustes.tema == Tema.PERGAMINO_CLARO
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Modo.entries.forEachIndexed { i, m ->
                        SegmentedButton(
                            selected = ajustes.modo == m,
                            onClick = { vm.setModo(m) },
                            enabled = !fijo,
                            shape = SegmentedButtonDefaults.itemShape(i, Modo.entries.size)
                        ) { Text(m.etiqueta) }
                    }
                }
                if (fijo) {
                    Text(
                        "Los temas pergamino tienen su propio modo claro u oscuro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
