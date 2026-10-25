package com.example.cronicasmarca.ui.dados

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cronicasmarca.ui.DiceRollResult
import com.example.cronicasmarca.ui.MainViewModel
import com.example.cronicasmarca.ui.theme.GoldAccent
import com.example.cronicasmarca.ui.theme.GoldDim
import com.example.cronicasmarca.ui.theme.GoldLight
import com.example.cronicasmarca.ui.theme.ParchmentSurface
import com.example.cronicasmarca.ui.theme.ParchmentSurfaceVariant
import com.example.cronicasmarca.ui.theme.SilverDim
import com.example.cronicasmarca.ui.theme.SilverText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DadosSheet(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var formula by remember { mutableStateOf("") }
    var history by remember { mutableStateOf<List<DiceRollResult>>(emptyList()) }
    val lastResult by viewModel.diceResult.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = ParchmentSurface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "DADOS",
                style = MaterialTheme.typography.labelMedium,
                color = GoldDim,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(16.dp))

            // Botones de dados rápidos (etiqueta → fórmula)
            val diceTypes = listOf(
                listOf("d4" to "d4", "d6" to "d6", "d8" to "d8", "d10" to "d10"),
                listOf("d12" to "d12", "d20" to "d20", "d100" to "d%")
            )
            diceTypes.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (label, dieFormula) ->
                        OutlinedButton(
                            onClick = {
                                val result = viewModel.rollDice(dieFormula)
                                if (result != null) {
                                    history = (listOf(result) + history).take(50)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                            border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.4f))
                        ) {
                            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Fórmula libre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formula,
                    onValueChange = { formula = it },
                    label = { Text("Fórmula (ej: 2d6+1)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = GoldAccent,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = GoldDim.copy(alpha = 0.3f),
                        focusedLabelColor = GoldAccent
                    )
                )
                Button(
                    onClick = {
                        val result = viewModel.rollDice(formula)
                        if (result != null) {
                            history = (listOf(result) + history).take(50)
                        }
                    },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent.copy(alpha = 0.2f),
                        contentColor = GoldLight
                    )
                ) {
                    Text("Tirar", fontWeight = FontWeight.Bold)
                }
            }

            // Último resultado
            lastResult?.let { result ->
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ParchmentSurfaceVariant),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            result.formula,
                            style = MaterialTheme.typography.labelMedium,
                            color = SilverDim
                        )
                        Text(
                            result.breakdown,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SilverText
                        )
                        Text(
                            "${result.total}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }

            // Historial (sin la tirada más reciente, que ya se muestra arriba)
            if (history.size > 1) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "HISTORIAL",
                    style = MaterialTheme.typography.labelMedium,
                    color = SilverDim,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(history.drop(1)) { roll ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                roll.formula,
                                modifier = Modifier.width(80.dp),
                                fontSize = 13.sp,
                                color = SilverDim
                            )
                            Text(
                                roll.breakdown,
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                color = SilverText.copy(alpha = 0.6f)
                            )
                            Text(
                                "${roll.total}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
