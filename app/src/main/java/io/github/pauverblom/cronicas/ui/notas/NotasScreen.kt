package io.github.pauverblom.cronicas.ui.notas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pauverblom.cronicas.data.Nota
import io.github.pauverblom.cronicas.ui.MainViewModel
import io.github.pauverblom.cronicas.ui.theme.GoldAccent
import io.github.pauverblom.cronicas.ui.theme.GoldDim
import io.github.pauverblom.cronicas.ui.theme.Parchment
import io.github.pauverblom.cronicas.ui.theme.ParchmentSurface
import io.github.pauverblom.cronicas.ui.theme.SilverDim
import io.github.pauverblom.cronicas.ui.theme.SilverText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotasScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val notas = state.notas
    var editingNota by remember { mutableStateOf<Nota?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (notas.isEmpty()) {
            EmptyNotasState()
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth > 700.dp
                if (isWide) {
                    // Tablet / horizontal: cuadrícula de 2 columnas
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notas, key = { it.id }) { nota ->
                            NotaCard(
                                nota = nota,
                                onClick = { editingNota = nota },
                                onDelete = { viewModel.deleteNota(nota.id) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notas, key = { it.id }) { nota ->
                            NotaCard(
                                nota = nota,
                                onClick = { editingNota = nota },
                                onDelete = { viewModel.deleteNota(nota.id) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp),
            shape = CircleShape,
            containerColor = GoldAccent.copy(alpha = 0.15f),
            contentColor = GoldAccent
        ) {
            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Nueva nota", modifier = Modifier.size(26.dp))
        }
    }

    if (showAddDialog) {
        NotaDialog(
            title = "Nueva nota",
            initialTitulo = "",
            initialContenido = "",
            onDismiss = { showAddDialog = false },
            onSave = { titulo, contenido ->
                viewModel.addNota(titulo, contenido)
                showAddDialog = false
            }
        )
    }

    editingNota?.let { nota ->
        NotaDialog(
            title = "Editar nota",
            initialTitulo = nota.titulo,
            initialContenido = nota.contenido,
            onDismiss = { editingNota = null },
            onSave = { titulo, contenido ->
                viewModel.updateNota(nota.copy(titulo = titulo, contenido = contenido))
                editingNota = null
            }
        )
    }
}

@Composable
private fun EmptyNotasState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = SilverDim.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(12.dp))
            Text("Sin notas", color = SilverDim, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text("Pulsa + para crear una", color = SilverDim.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}

@Composable
fun NotaCard(nota: Nota, onClick: () -> Unit, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = GoldDim
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    nota.titulo.ifBlank { "Sin título" },
                    modifier = Modifier.weight(1f),
                    color = Parchment,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Eliminar",
                        modifier = Modifier.size(16.dp),
                        tint = SilverDim
                    )
                }
            }
            if (nota.contenido.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    nota.contenido,
                    color = SilverText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                dateFormat.format(Date(nota.timestamp)),
                color = SilverDim,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun NotaDialog(
    title: String,
    initialTitulo: String,
    initialContenido: String,
    onDismiss: () -> Unit,
    onSave: (titulo: String, contenido: String) -> Unit
) {
    var titulo by remember { mutableStateOf(initialTitulo) }
    var contenido by remember { mutableStateOf(initialContenido) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = ParchmentSurface,
        title = { Text(title, color = GoldAccent, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = GoldAccent,
                        focusedBorderColor = GoldAccent,
                        focusedLabelColor = GoldAccent
                    )
                )
                OutlinedTextField(
                    value = contenido,
                    onValueChange = { contenido = it },
                    label = { Text("Contenido") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    maxLines = 15,
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = GoldAccent,
                        focusedBorderColor = GoldAccent,
                        focusedLabelColor = GoldAccent
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (titulo.isNotBlank() || contenido.isNotBlank()) {
                    onSave(titulo, contenido)
                }
            }) {
                Text("Guardar", color = GoldAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SilverDim)
            }
        }
    )
}
