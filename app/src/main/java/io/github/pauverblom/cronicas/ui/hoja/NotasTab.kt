package io.github.pauverblom.cronicas.ui.hoja

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.pauverblom.cronicas.data.Nota
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
import io.github.pauverblom.cronicas.ui.common.FormularioDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotasTab(ui: HojaUi, vm: HojaViewModel) {
    var editando by remember { mutableStateOf<Nota?>(null) }
    var borrando by remember { mutableStateOf<Nota?>(null) }
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES")) }

    Box(Modifier.fillMaxSize()) {
        if (ui.notas.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Sin notas", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Pulsa + para apuntar algo de la partida", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(320.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ui.notas, key = { it.id }) { n ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { editando = n },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    n.titulo.ifBlank { "Sin título" },
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(onClick = { borrando = n }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, "Eliminar", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                            if (n.contenido.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(n.contenido, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(formato.format(Date(n.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editando = Nota(personajeId = ui.personaje.id) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.AutoMirrored.Filled.NoteAdd, "Nueva nota") }
    }

    editando?.let { n ->
        var titulo by remember(n) { mutableStateOf(n.titulo) }
        var contenido by remember(n) { mutableStateOf(n.contenido) }
        FormularioDialog(
            if (n.id == 0L) "Nueva nota" else "Editar nota",
            onDismiss = { editando = null },
            guardarActivo = titulo.isNotBlank() || contenido.isNotBlank(),
            onGuardar = { vm.guardarNota(n.copy(titulo = titulo, contenido = contenido)); editando = null }
        ) {
            OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(contenido, { contenido = it }, label = { Text("Contenido") }, modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp))
        }
    }
    borrando?.let { n ->
        ConfirmarDialog("Eliminar nota", "¿Eliminar «${n.titulo.ifBlank { "Sin título" }}»?", onDismiss = { borrando = null }) { vm.eliminarNota(n.id) }
    }
}
