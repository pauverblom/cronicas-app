package com.example.cronicasmarca.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cronicasmarca.ui.theme.GoldAccent
import com.example.cronicasmarca.ui.theme.GoldDim
import com.example.cronicasmarca.ui.theme.Parchment
import com.example.cronicasmarca.ui.theme.ParchmentSurface
import com.example.cronicasmarca.ui.theme.ParchmentSurfaceVariant
import com.example.cronicasmarca.ui.theme.SilverDim

@Composable
fun NewCharacterDialog(
    onDismiss: () -> Unit,
    onCreate: (
        nombre: String,
        clase: String,
        especie: String,
        nivel: Int,
        fue: Int,
        des: Int,
        con: Int,
        int_: Int,
        sab: Int,
        car: Int,
        pgMax: Int,
        dadoGolpe: String
    ) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var clase by remember { mutableStateOf("") }
    var especie by remember { mutableStateOf("") }
    var nivel by remember { mutableStateOf("1") }
    var fue by remember { mutableStateOf("10") }
    var des by remember { mutableStateOf("10") }
    var con by remember { mutableStateOf("10") }
    var int_ by remember { mutableStateOf("10") }
    var sab by remember { mutableStateOf("10") }
    var car by remember { mutableStateOf("10") }
    var pgMax by remember { mutableStateOf("4") }
    var dadoGolpe by remember { mutableStateOf("d4") }
    var dadoExpanded by remember { mutableStateOf(false) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        cursorColor = GoldAccent,
        focusedBorderColor = GoldAccent,
        unfocusedBorderColor = GoldDim.copy(alpha = 0.4f),
        focusedLabelColor = GoldAccent,
        unfocusedLabelColor = SilverDim
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = ParchmentSurface,
        title = {
            Text("Nuevo personaje", color = GoldAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre *") },
                    singleLine = true,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clase,
                        onValueChange = { clase = it },
                        label = { Text("Clase") },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = especie,
                        onValueChange = { especie = it },
                        label = { Text("Especie") },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nivel,
                        onValueChange = { nivel = it.filter { c -> c.isDigit() } },
                        label = { Text("Nivel") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pgMax,
                        onValueChange = { pgMax = it.filter { c -> c.isDigit() } },
                        label = { Text("PG Máx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dadoGolpe,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("DG") },
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = dadoExpanded,
                            onDismissRequest = { dadoExpanded = false },
                            containerColor = ParchmentSurfaceVariant
                        ) {
                            listOf("d4", "d6", "d8", "d10", "d12").forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d, color = Parchment) },
                                    onClick = {
                                        dadoGolpe = d
                                        dadoExpanded = false
                                    }
                                )
                            }
                        }
                        // Capa transparente para abrir el menú al tocar el campo de solo lectura.
                        Surface(
                            onClick = { dadoExpanded = true },
                            modifier = Modifier.matchParentSize(),
                            color = Color.Transparent
                        ) {}
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "CARACTERÍSTICAS",
                    color = GoldDim,
                    letterSpacing = 3.sp,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(2.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatField("FUE", fue, { fue = it }, Modifier.weight(1f), tfColors)
                    StatField("DES", des, { des = it }, Modifier.weight(1f), tfColors)
                    StatField("CON", con, { con = it }, Modifier.weight(1f), tfColors)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatField("INT", int_, { int_ = it }, Modifier.weight(1f), tfColors)
                    StatField("SAB", sab, { sab = it }, Modifier.weight(1f), tfColors)
                    StatField("CAR", car, { car = it }, Modifier.weight(1f), tfColors)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isNotBlank()) {
                    onCreate(
                        nombre.trim(),
                        clase.trim(),
                        especie.trim(),
                        nivel.toIntOrNull() ?: 1,
                        fue.toIntOrNull() ?: 10,
                        des.toIntOrNull() ?: 10,
                        con.toIntOrNull() ?: 10,
                        int_.toIntOrNull() ?: 10,
                        sab.toIntOrNull() ?: 10,
                        car.toIntOrNull() ?: 10,
                        pgMax.toIntOrNull() ?: 4,
                        dadoGolpe
                    )
                }
            }) {
                Text("Crear", color = GoldAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SilverDim)
            }
        }
    )
}

@Composable
fun StatField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    colors: TextFieldColors
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
        label = { Text(label, fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = colors,
        modifier = modifier
    )
}
