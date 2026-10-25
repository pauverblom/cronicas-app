package com.example.cronicasmarca.ui.equipo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cronicasmarca.data.Objeto
import com.example.cronicasmarca.data.Tesoro
import com.example.cronicasmarca.ui.MainViewModel
import com.example.cronicasmarca.ui.theme.ArcanePurple
import com.example.cronicasmarca.ui.theme.ForestGreen
import com.example.cronicasmarca.ui.theme.GoldAccent
import com.example.cronicasmarca.ui.theme.GoldDim
import com.example.cronicasmarca.ui.theme.HpAmber
import com.example.cronicasmarca.ui.theme.HpRed
import com.example.cronicasmarca.ui.theme.Parchment
import com.example.cronicasmarca.ui.theme.ParchmentSurface
import com.example.cronicasmarca.ui.theme.SilverDim

@Composable
fun EquipoScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showAddObjeto by remember { mutableStateOf(false) }

    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GoldAccent)
        }
        return
    }

    val tesoro = state.tesoro ?: Tesoro(personajeId = 0)
    val totalVe = viewModel.getTotalVe()
    val carryLimit = viewModel.getCarryLimit()

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
                    CarryLimitBlock(totalVe, carryLimit)
                    ObjetosBlock(
                        state.objetos,
                        state.armas.sumOf { it.ve },
                        viewModel,
                        onAdd = { showAddObjeto = true }
                    )
                    Spacer(Modifier.height(60.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MonedasBlock(tesoro, viewModel)
                    GemasBlock(tesoro, viewModel)
                    MagicItemsBlock(state.objetos.filter { it.esMagico })
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
                CarryLimitBlock(totalVe, carryLimit)
                MonedasBlock(tesoro, viewModel)
                ObjetosBlock(
                    state.objetos,
                    state.armas.sumOf { it.ve },
                    viewModel,
                    onAdd = { showAddObjeto = true }
                )
                GemasBlock(tesoro, viewModel)
                MagicItemsBlock(state.objetos.filter { it.esMagico })
                Spacer(Modifier.height(60.dp))
            }
        }
    }

    if (showAddObjeto) {
        AddObjetoDialog(
            onDismiss = { showAddObjeto = false },
            onAdd = { nombre, cantidad, ve, esMagico, desc ->
                viewModel.addObjeto(nombre, cantidad, ve, esMagico, desc)
                showAddObjeto = false
            }
        )
    }
}

// ── Carga ──

@Composable
fun CarryLimitBlock(totalVe: Float, carryLimit: Int) {
    val ratio = if (carryLimit > 0) totalVe / carryLimit else 0f
    val isOver = totalVe > carryLimit
    val barColor = when {
        isOver -> HpRed
        ratio > 0.8f -> HpAmber
        else -> ForestGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, barColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "CARGA",
                style = MaterialTheme.typography.labelMedium,
                color = GoldDim,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "${totalVe.toInt()}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
                Text(
                    " / $carryLimit VE",
                    fontSize = 18.sp,
                    color = SilverDim,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF1A1510))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.6f), barColor)))
                )
            }

            if (isOver) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "¡Sobrecargado! Velocidad reducida",
                    color = HpRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Monedas ──

@Composable
fun MonedasBlock(tesoro: Tesoro, viewModel: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "MONEDAS",
                style = MaterialTheme.typography.labelMedium,
                color = GoldDim,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(10.dp))
            CoinRow("Platino", tesoro.platino, Color(0xFFB8C4D0), "platino", viewModel)
            CoinRow("Oro", tesoro.oro, GoldAccent, "oro", viewModel)
            CoinRow("Plata", tesoro.plata, Color(0xFFA8A8B0), "plata", viewModel)
            CoinRow("Cobre", tesoro.cobre, Color(0xFFB87333), "cobre", viewModel)
        }
    }
}

@Composable
fun CoinRow(label: String, value: Int, color: Color, tipo: String, viewModel: MainViewModel) {
    var editing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(label.first().toString(), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = Parchment, fontSize = 14.sp, modifier = Modifier.width(60.dp))
        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = { viewModel.updateMonedas(tipo, -1) },
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("-", color = color, fontSize = 16.sp)
        }

        Spacer(Modifier.width(8.dp))

        if (editing) {
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it.filter { c -> c.isDigit() } },
                modifier = Modifier
                    .width(72.dp)
                    .height(40.dp),
                textStyle = LocalTextStyle.current.copy(
                    color = color,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = color,
                    focusedBorderColor = color
                )
            )
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = {
                    val newVal = editText.toIntOrNull() ?: value
                    viewModel.setMonedas(tipo, newVal)
                    editing = false
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Confirmar",
                    modifier = Modifier.size(16.dp),
                    tint = ForestGreen
                )
            }
        } else {
            // Tocar el valor permite editarlo directamente
            Surface(
                onClick = {
                    editText = value.toString()
                    editing = true
                },
                modifier = Modifier.widthIn(min = 56.dp),
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.08f)
            ) {
                Text(
                    "$value",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        OutlinedButton(
            onClick = { viewModel.updateMonedas(tipo, 1) },
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("+", color = color, fontSize = 16.sp)
        }
    }
}

// ── Objetos ──

@Composable
fun ObjetosBlock(objetos: List<Objeto>, armaVe: Int, viewModel: MainViewModel, onAdd: () -> Unit) {
    val nonMagical = objetos.filter { !it.esMagico }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "OBJETOS",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldDim,
                    letterSpacing = 3.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Añadir objeto",
                        modifier = Modifier.size(20.dp),
                        tint = GoldAccent
                    )
                }
            }

            // Las armas también cuentan para la carga
            if (armaVe > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Gavel,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = SilverDim.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Armas equipadas", color = SilverDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("VE $armaVe", color = SilverDim, fontSize = 12.sp)
                }
            }

            if (nonMagical.isEmpty() && armaVe == 0) {
                Text(
                    "Sin objetos",
                    color = SilverDim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            nonMagical.forEach { obj ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(obj.nombre, color = Parchment, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (obj.cantidad > 1) {
                        Text("×${obj.cantidad}", color = SilverDim, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("VE ${(obj.ve * obj.cantidad).toInt()}", color = SilverDim, fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.deleteObjeto(obj) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Eliminar",
                            modifier = Modifier.size(14.dp),
                            tint = SilverDim.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

// ── Gemas y joyas ──

@Composable
fun GemasBlock(tesoro: Tesoro, viewModel: MainViewModel) {
    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(tesoro.gemas) }

    // Si cambian desde fuera (otro personaje, deshacer…) y no se está editando, sincroniza
    LaunchedEffect(tesoro.gemas) {
        if (!editing) text = tesoro.gemas
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "GEMAS Y JOYAS",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldDim,
                    letterSpacing = 3.sp,
                    modifier = Modifier.weight(1f)
                )
                if (editing) {
                    IconButton(
                        onClick = {
                            viewModel.setGemas(text)
                            editing = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Guardar",
                            modifier = Modifier.size(18.dp),
                            tint = ForestGreen
                        )
                    }
                } else {
                    IconButton(
                        onClick = { editing = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            modifier = Modifier.size(16.dp),
                            tint = SilverDim
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            if (editing) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    placeholder = { Text("Rubí, zafiro, diamante...", color = SilverDim.copy(alpha = 0.4f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = GoldAccent,
                        focusedBorderColor = GoldAccent
                    )
                )
            } else {
                if (tesoro.gemas.isBlank()) {
                    Text("Sin gemas ni joyas", color = SilverDim, fontSize = 14.sp)
                } else {
                    Text(tesoro.gemas, color = Parchment, fontSize = 14.sp)
                }
            }
        }
    }
}

// ── Objetos mágicos ──

@Composable
fun MagicItemsBlock(magicItems: List<Objeto>) {
    if (magicItems.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, ArcanePurple.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "OBJETOS MÁGICOS",
                style = MaterialTheme.typography.labelMedium,
                color = ArcanePurple,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(8.dp))

            magicItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ArcanePurple.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.nombre, color = Parchment, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (item.descripcion.isNotBlank()) {
                            Text(item.descripcion, color = SilverDim, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Diálogo de nuevo objeto ──

@Composable
private fun AddObjetoDialog(
    onDismiss: () -> Unit,
    onAdd: (nombre: String, cantidad: Int, ve: Float, esMagico: Boolean, descripcion: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    var ve by remember { mutableStateOf("0") }
    var esMagico by remember { mutableStateOf(false) }
    var descripcion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo objeto", color = GoldAccent, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = GoldAccent,
                        focusedBorderColor = GoldAccent
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it.filter { c -> c.isDigit() } },
                        label = { Text("Cantidad") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            cursorColor = GoldAccent,
                            focusedBorderColor = GoldAccent
                        )
                    )
                    OutlinedTextField(
                        value = ve,
                        onValueChange = { ve = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("VE") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            cursorColor = GoldAccent,
                            focusedBorderColor = GoldAccent
                        )
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = esMagico,
                        onCheckedChange = { esMagico = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = ArcanePurple,
                            checkmarkColor = Parchment
                        )
                    )
                    Text("Objeto mágico", color = Parchment, fontSize = 14.sp)
                }
                if (esMagico) {
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            cursorColor = GoldAccent,
                            focusedBorderColor = GoldAccent
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isNotBlank()) {
                    onAdd(
                        nombre,
                        cantidad.toIntOrNull() ?: 1,
                        ve.toFloatOrNull() ?: 0f,
                        esMagico,
                        descripcion
                    )
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
