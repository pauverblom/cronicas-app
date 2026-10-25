package io.github.pauverblom.cronicas.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tarjeta de sección de la ficha con su título en versalitas. */
@Composable
fun SeccionCard(
    titulo: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titulo.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 3.sp,
                    modifier = Modifier.weight(1f)
                )
                accion?.invoke()
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/** Caja con un valor grande. Si [forzado], muestra un lápiz: el valor se ha fijado a mano. */
@Composable
fun StatBox(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    detalle: String? = null,
    forzado: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(etiqueta, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                if (forzado) {
                    Spacer(Modifier.width(3.dp))
                    Icon(Icons.Default.Edit, contentDescription = "Fijado a mano", modifier = Modifier.size(10.dp), tint = color)
                }
            }
            Text(valor, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            if (detalle != null) {
                Text(detalle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Campo numérico (admite signo negativo si [negativos]). */
@Composable
fun CampoNumero(
    valor: String,
    onValor: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    negativos: Boolean = false,
    decimales: Boolean = false,
    soporte: String? = null,
    maxLength: Int = 8
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo ->
            val ok = nuevo.isEmpty() || (if (negativos) nuevo == "-" || nuevo.removePrefix("-").all(Char::isDigit) else if (decimales) nuevo.count { it == '.' || it == ',' } <= 1 && nuevo.all { it.isDigit() || it == '.' || it == ',' } else nuevo.all(Char::isDigit))
            if (ok && nuevo.length <= maxLength) onValor(nuevo)
        },
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimales) KeyboardType.Decimal else KeyboardType.Number),
        supportingText = soporte?.let { { Text(it) } },
        modifier = modifier
    )
}

/**
 * Diálogo para fijar un número a mano. [calculado] es el valor automático: "Automático" quita el valor forzado.
 */
@Composable
fun NumeroDialog(
    titulo: String,
    actual: Int?,
    calculado: Int?,
    negativos: Boolean = true,
    explicacion: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var texto by remember { mutableStateOf((actual ?: calculado)?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column {
                explicacion?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
                CampoNumero(texto, { texto = it }, "Valor", negativos = negativos, modifier = Modifier.fillMaxWidth())
                if (calculado != null) {
                    Text("Calculado: $calculado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { texto.toIntOrNull()?.let(onConfirm) ?: onDismiss() }) { Text("Fijar") }
        },
        dismissButton = {
            Row {
                if (calculado != null && actual != null) {
                    TextButton(onClick = { onConfirm(null) }) { Text("Automático") }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
fun ConfirmarDialog(
    titulo: String,
    texto: String,
    confirmar: String = "Eliminar",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmar, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Desplegable de solo lectura con [opciones]; [etiquetaDe] da el texto de cada una. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Desplegable(
    etiqueta: String,
    opciones: List<T>,
    seleccionado: T?,
    etiquetaDe: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    soporte: String? = null
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }, modifier = modifier) {
        OutlinedTextField(
            value = seleccionado?.let(etiquetaDe) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            supportingText = soporte?.let { { Text(it) } },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { op ->
                DropdownMenuItem(text = { Text(etiquetaDe(op)) }, onClick = { onSeleccion(op); abierto = false })
            }
        }
    }
}

/** Desplegable editable: se puede elegir una sugerencia o escribir libremente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesplegableEditable(
    etiqueta: String,
    valor: String,
    sugerencias: List<String>,
    onValor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }, modifier = modifier) {
        OutlinedTextField(
            value = valor,
            onValueChange = onValor,
            label = { Text(etiqueta) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            sugerencias.forEach { s ->
                DropdownMenuItem(text = { Text(s) }, onClick = { onValor(s); abierto = false })
            }
        }
    }
}

/** Diálogo con un formulario desplazable. */
@Composable
fun FormularioDialog(
    titulo: String,
    onDismiss: () -> Unit,
    onGuardar: () -> Unit,
    guardar: String = "Guardar",
    guardarActivo: Boolean = true,
    extraBoton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        },
        confirmButton = { TextButton(onClick = onGuardar, enabled = guardarActivo) { Text(guardar) } },
        dismissButton = {
            Row {
                extraBoton?.invoke()
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

fun signo(n: Int): String = if (n >= 0) "+$n" else "$n"

fun formatoVe(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v).replace('.', ',')
