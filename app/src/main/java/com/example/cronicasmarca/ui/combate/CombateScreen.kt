package com.example.cronicasmarca.ui.combate

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cronicasmarca.data.Arma
import com.example.cronicasmarca.data.EfectoActivo
import com.example.cronicasmarca.ui.DiceRollResult
import com.example.cronicasmarca.ui.MainViewModel
import com.example.cronicasmarca.ui.theme.ArcaneBlue
import com.example.cronicasmarca.ui.theme.ArcanePurple
import com.example.cronicasmarca.ui.theme.ForestGreen
import com.example.cronicasmarca.ui.theme.GoldAccent
import com.example.cronicasmarca.ui.theme.GoldDim
import com.example.cronicasmarca.ui.theme.HpAmber
import com.example.cronicasmarca.ui.theme.HpRed
import com.example.cronicasmarca.ui.theme.Parchment
import com.example.cronicasmarca.ui.theme.ParchmentSurface
import com.example.cronicasmarca.ui.theme.ParchmentSurfaceVariant
import com.example.cronicasmarca.ui.theme.SilverDim
import com.example.cronicasmarca.ui.theme.SilverText

@Composable
fun CombateScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val combate = state.combate
    var attackResult by remember { mutableStateOf<DiceRollResult?>(null) }
    var showAddEffect by remember { mutableStateOf(false) }

    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GoldAccent)
        }
        return
    }
    if (combate == null) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 700.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeaponsBlock(state.armas, viewModel, onResult = { attackResult = it })
                    RoundTrackerBlock(viewModel)
                    Spacer(Modifier.height(60.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EffectsManagerBlock(state.efectos, viewModel, onAdd = { showAddEffect = true })
                    AttenuatedDamageBlock(combate.danoAtenuado, viewModel)
                    Spacer(Modifier.height(60.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeaponsBlock(state.armas, viewModel, onResult = { attackResult = it })
                RoundTrackerBlock(viewModel)
                EffectsManagerBlock(state.efectos, viewModel, onAdd = { showAddEffect = true })
                AttenuatedDamageBlock(combate.danoAtenuado, viewModel)
                Spacer(Modifier.height(60.dp))
            }
        }
    }

    // Resultado de ataque / daño
    attackResult?.let { result ->
        AlertDialog(
            onDismissRequest = { attackResult = null },
            confirmButton = {
                TextButton(onClick = { attackResult = null }) {
                    Text("Cerrar", color = GoldAccent)
                }
            },
            title = { Text("Resultado", color = GoldAccent, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(result.formula, color = SilverDim, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(result.breakdown, color = SilverText, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${result.total}",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = ParchmentSurface
        )
    }

    if (showAddEffect) {
        AddEffectDialog(
            onDismiss = { showAddEffect = false },
            onAdd = { nombre, unidad, duracion ->
                viewModel.addEffect(nombre, unidad, duracion)
                showAddEffect = false
            }
        )
    }
}

// ── Armas ──

@Composable
fun WeaponsBlock(armas: List<Arma>, viewModel: MainViewModel, onResult: (DiceRollResult) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "ARMAS",
                style = MaterialTheme.typography.labelMedium,
                color = GoldDim,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(10.dp))

            if (armas.isEmpty()) {
                Text("Sin armas equipadas", color = SilverDim, fontSize = 14.sp)
            }

            armas.forEach { arma ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ParchmentSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (arma.tipo == "distancia") Icons.Default.GpsFixed else Icons.Default.Gavel,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = SilverDim
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                arma.nombre,
                                modifier = Modifier.weight(1f),
                                color = Parchment,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = HpRed.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    arma.formulaDano,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = HpRed.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        if (arma.notas.isNotBlank()) {
                            Text(
                                arma.notas,
                                modifier = Modifier.padding(top = 2.dp),
                                color = SilverDim,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onResult(viewModel.rollAttack(arma)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ArcaneBlue.copy(alpha = 0.2f),
                                    contentColor = ArcaneBlue
                                )
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Atacar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { viewModel.rollDamage(arma)?.let { onResult(it) } },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HpRed.copy(alpha = 0.2f),
                                    contentColor = HpRed
                                )
                            ) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Daño", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Control de tiempo ──

@Composable
fun RoundTrackerBlock(viewModel: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "CONTROL DE TIEMPO",
                style = MaterialTheme.typography.labelMedium,
                color = GoldDim,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeButton("+1 Asalto", "~6 seg", ArcaneBlue, Modifier.weight(1f)) {
                    viewModel.advanceRound()
                }
                TimeButton("+1 Turno", "~1 min", ArcanePurple, Modifier.weight(1f)) {
                    viewModel.advanceTurn()
                }
                TimeButton("+1 Minuto", "Curar aten.", ForestGreen, Modifier.weight(1f)) {
                    viewModel.advanceMinute()
                }
            }
        }
    }
}

@Composable
fun TimeButton(
    label: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = color.copy(alpha = 0.5f), fontSize = 9.sp)
        }
    }
}

// ── Efectos activos ──

@Composable
fun EffectsManagerBlock(efectos: List<EfectoActivo>, viewModel: MainViewModel, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, ArcanePurple.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "EFECTOS ACTIVOS",
                    style = MaterialTheme.typography.labelMedium,
                    color = ArcanePurple,
                    letterSpacing = 3.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Añadir efecto",
                        modifier = Modifier.size(20.dp),
                        tint = ArcanePurple
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            if (efectos.isEmpty()) {
                Text(
                    "Sin efectos activos",
                    color = SilverDim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            efectos.forEach { efecto ->
                val expiring = efecto.restante <= 1
                val color = if (expiring) HpRed else ArcanePurple

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (expiring) Icons.Default.Warning else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = color.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(efecto.nombre, color = Parchment, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${efecto.restante} ${efecto.unidad}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.removeEffect(efecto.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Eliminar",
                            modifier = Modifier.size(16.dp),
                            tint = SilverDim
                        )
                    }
                }
            }
        }
    }
}

// ── Daño atenuado ──

@Composable
fun AttenuatedDamageBlock(danoAtenuado: Int, viewModel: MainViewModel) {
    if (danoAtenuado <= 0) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, HpAmber.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "DAÑO ATENUADO",
                style = MaterialTheme.typography.labelMedium,
                color = HpAmber,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(6.dp))
            Text("$danoAtenuado PG", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = HpAmber)
            Text("Se recupera 1 PG por minuto", fontSize = 11.sp, color = SilverDim)
        }
    }
}

// ── Diálogo de nuevo efecto ──

@Composable
private fun AddEffectDialog(onDismiss: () -> Unit, onAdd: (nombre: String, unidad: String, duracion: Int) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var duracion by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("turno") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo efecto", color = GoldAccent, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del efecto") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = GoldAccent,
                        focusedBorderColor = GoldAccent
                    )
                )
                OutlinedTextField(
                    value = duracion,
                    onValueChange = { duracion = it.filter { c -> c.isDigit() } },
                    label = { Text("Duración") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = GoldAccent,
                        focusedBorderColor = GoldAccent
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("asalto" to "Asalto", "turno" to "Turno", "minuto" to "Minuto").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedUnit == key,
                            onClick = { selectedUnit = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ArcanePurple.copy(alpha = 0.2f),
                                selectedLabelColor = ArcanePurple
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dur = duracion.toIntOrNull() ?: 0
                if (nombre.isNotBlank() && dur > 0) {
                    onAdd(nombre, selectedUnit, dur)
                }
            }) {
                Text("Añadir", color = GoldAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SilverDim)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = ParchmentSurface
    )
}
