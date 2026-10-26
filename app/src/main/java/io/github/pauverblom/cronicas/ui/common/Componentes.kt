package io.github.pauverblom.cronicas.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.pauverblom.cronicas.ui.hud.Boton
import io.github.pauverblom.cronicas.ui.hud.Campo
import io.github.pauverblom.cronicas.ui.hud.Dialogo
import io.github.pauverblom.cronicas.ui.hud.Etiqueta
import io.github.pauverblom.cronicas.ui.hud.Glifo
import io.github.pauverblom.cronicas.ui.hud.IconoGlifo
import io.github.pauverblom.cronicas.ui.hud.Variante
import io.github.pauverblom.cronicas.ui.hud.conSigno
import io.github.pauverblom.cronicas.ui.hud.tecladoNumerico
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos

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
    maxLength: Int = 8,
    error: String? = null,
    sufijo: String? = null
) {
    Campo(
        valor,
        { nuevo ->
            val ok = nuevo.isEmpty() || (if (negativos) nuevo == "-" || nuevo.removePrefix("-").all(Char::isDigit) else if (decimales) nuevo.count { it == '.' || it == ',' } <= 1 && nuevo.all { it.isDigit() || it == '.' || it == ',' } else nuevo.all(Char::isDigit))
            if (ok && nuevo.length <= maxLength) onValor(nuevo)
        },
        etiqueta,
        modifier,
        soporte = soporte,
        error = error,
        sufijo = sufijo,
        teclado = tecladoNumerico(decimales)
    )
}

/**
 * Diálogo para fijar un número a mano. [calculado] es el valor automático: «Automático» quita el valor forzado.
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
    val c = Marca.colores
    Dialogo(
        titulo,
        onDismiss = onDismiss,
        extra = if (calculado != null && actual != null) {
            { Boton("Automático", { onConfirm(null) }, variante = Variante.DISCRETO) }
        } else null,
        acciones = {
            Boton("Cancelar", onDismiss, variante = Variante.DISCRETO)
            Boton("Fijar", { texto.toIntOrNull()?.let(onConfirm) ?: onDismiss() }, variante = Variante.DORADO)
        }
    ) {
        explicacion?.let { Text(it, style = Tipos.cuerpoSm, color = c.tintaSuave) }
        CampoNumero(texto, { texto = it }, "Valor", negativos = negativos, modifier = Modifier.fillMaxWidth())
        if (calculado != null) Text("Calculado: $calculado", style = Tipos.cifraMd, color = c.tintaSuave)
    }
}

@Composable
fun ConfirmarDialog(
    titulo: String,
    texto: String,
    confirmar: String = "Eliminar",
    destructivo: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialogo(
        titulo,
        onDismiss = onDismiss,
        acciones = {
            Boton("Cancelar", onDismiss, variante = Variante.DISCRETO)
            Boton(confirmar, { onConfirm(); onDismiss() }, variante = if (destructivo) Variante.PELIGRO else Variante.DORADO)
        }
    ) { Text(texto) }
}

/** Desplegable de solo lectura con [opciones]; [etiquetaDe] da el texto de cada una. */
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
    val c = Marca.colores
    Box(modifier) {
        Campo(
            seleccionado?.let(etiquetaDe) ?: "",
            {},
            etiqueta,
            Modifier.fillMaxWidth(),
            soporte = soporte,
            marcador = "Elegir…",
            onClick = { abierto = true },
            final = { IconoGlifo(Glifo.ROMBO, tamano = 12.dp, color = c.oro) }
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }, offset = DpOffset(16.dp, 0.dp)) {
            opciones.forEach { op ->
                val elegido = op == seleccionado
                DropdownMenuItem(
                    text = { Text(etiquetaDe(op), style = Tipos.ui, color = if (elegido) c.oro else c.tinta) },
                    onClick = { onSeleccion(op); abierto = false }
                )
            }
        }
    }
}

/**
 * Desplegable editable: un campo libre con las sugerencias debajo como etiquetas, filtradas por lo escrito.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesplegableEditable(
    etiqueta: String,
    valor: String,
    sugerencias: List<String>,
    onValor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val texto = valor.trim()
    val visibles = sugerencias
        .filter { texto.isBlank() || (it.contains(texto, ignoreCase = true) && !it.equals(texto, ignoreCase = true)) }
        .take(10)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Campo(valor, onValor, etiqueta, Modifier.fillMaxWidth())
        if (visibles.isNotEmpty() && !sugerencias.any { it.equals(texto, ignoreCase = true) }) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                visibles.forEach { s -> Etiqueta(s, onClick = { onValor(s) }) }
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
    Dialogo(
        titulo,
        onDismiss = onDismiss,
        extra = extraBoton,
        acciones = {
            Boton("Cancelar", onDismiss, variante = Variante.DISCRETO)
            Boton(guardar, onGuardar, variante = Variante.DORADO, activo = guardarActivo)
        },
        contenido = content
    )
}

fun signo(n: Int): String = conSigno(n)

fun formatoVe(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v).replace('.', ',')
