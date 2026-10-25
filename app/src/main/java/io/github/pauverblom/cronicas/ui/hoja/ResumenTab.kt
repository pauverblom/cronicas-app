package io.github.pauverblom.cronicas.ui.hoja

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pauverblom.cronicas.data.Condicion
import io.github.pauverblom.cronicas.rules.AtaqueHoja
import io.github.pauverblom.cronicas.rules.Atributo
import io.github.pauverblom.cronicas.rules.CaracteristicaHoja
import io.github.pauverblom.cronicas.rules.Salvaciones
import io.github.pauverblom.cronicas.ui.common.CampoNumero
import io.github.pauverblom.cronicas.ui.common.FormularioDialog
import io.github.pauverblom.cronicas.ui.common.NumeroDialog
import io.github.pauverblom.cronicas.ui.common.SeccionCard
import io.github.pauverblom.cronicas.ui.common.StatBox
import io.github.pauverblom.cronicas.ui.common.signo
import io.github.pauverblom.cronicas.ui.theme.LocalColoresHoja

/** Columna desplazable en móvil; dos columnas en pantallas anchas. */
@Composable
fun ColumnasHoja(izquierda: @Composable ColumnScope.() -> Unit, derecha: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth > 700.dp) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    izquierda(); Spacer(Modifier.height(80.dp))
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    derecha(); Spacer(Modifier.height(80.dp))
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                izquierda(); derecha(); Spacer(Modifier.height(80.dp))
            }
        }
    }
}

private enum class Fijar { BA, MOV, PG_MAX }

@Composable
fun ResumenTab(ui: HojaUi, vm: HojaViewModel) {
    var fijar by remember { mutableStateOf<Fijar?>(null) }
    var verCa by remember { mutableStateOf(false) }
    var editarDg by remember { mutableStateOf(false) }
    val h = ui.hoja

    ColumnasHoja(
        izquierda = {
            BloquePg(ui, onMenos = { vm.cambiarPg(-1) }, onMas = { vm.cambiarPg(1) }, onEditarMax = { fijar = Fijar.PG_MAX })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("CA", "${h.ca.total}", Modifier.weight(1f), MaterialTheme.colorScheme.secondary, forzado = h.ca.forzada, onClick = { verCa = true })
                StatBox("BA", signo(h.ba), Modifier.weight(1f), MaterialTheme.colorScheme.tertiary, forzado = h.baForzado, onClick = { fijar = Fijar.BA })
                StatBox("DG", h.dado.substringBefore(' '), Modifier.weight(1f), MaterialTheme.colorScheme.primary, forzado = h.dadoForzado, onClick = { editarDg = true })
            }
            StatBox(
                "MOVIMIENTO",
                "${h.movimiento} m",
                Modifier.fillMaxWidth(),
                LocalColoresHoja.current.pgBien,
                detalle = "Marchar ${h.movimiento * 2} m · Correr ${h.movimiento * 4} m" +
                    if (h.movimiento != h.movimientoBase) " · Base ${h.movimientoBase} m" else "",
                forzado = h.movimientoForzado,
                onClick = { fijar = Fijar.MOV }
            )
            if (h.avisos.isNotEmpty()) BloqueAvisos(h.avisos)
            BloqueCondiciones(ui.condiciones, onGuardar = vm::guardarCondicion, onEliminar = vm::eliminarCondicion)
        },
        derecha = {
            BloqueAtaques(h.ataques)
            BloqueSalvaciones(h.caracteristicas)
        }
    )

    when (fijar) {
        Fijar.BA -> NumeroDialog(
            "Bonificador de ataque", ui.estado.baOverride,
            h.clase?.let { io.github.pauverblom.cronicas.rules.Progresion.ba(it, h.secundaria, h.nivel) },
            explicacion = "Se suma FUE en cuerpo a cuerpo y DES a distancia.",
            onDismiss = { fijar = null }
        ) { v -> vm.actualizarEstado { it.copy(baOverride = v) }; fijar = null }
        Fijar.MOV -> NumeroDialog(
            "Movimiento base (m)", ui.estado.movOverride,
            h.especie?.movimiento ?: 10,
            negativos = false,
            explicacion = "La carga media resta 3 m; sobrecargado deja el movimiento en 3 m.",
            onDismiss = { fijar = null }
        ) { v -> vm.actualizarEstado { it.copy(movOverride = v) }; fijar = null }
        Fijar.PG_MAX -> NumeroDialog(
            "PG máximos", ui.estado.pgMaximos, null, negativos = false,
            explicacion = "Para sumar los PG de un nivel nuevo, usa «Subir de nivel» en la pestaña Personaje.",
            onDismiss = { fijar = null }
        ) { v -> v?.let(vm::setPgMax); fijar = null }
        null -> {}
    }

    if (verCa) CaDialog(ui, onDismiss = { verCa = false }, vm = vm)

    if (editarDg) {
        var texto by remember { mutableStateOf(ui.estado.dgOverride ?: h.dado) }
        FormularioDialog(
            "Dado de golpe", onDismiss = { editarDg = false },
            onGuardar = { vm.actualizarEstado { it.copy(dgOverride = texto.trim().ifBlank { null }) }; editarDg = false },
            extraBoton = if (ui.estado.dgOverride != null) {
                { TextButton(onClick = { vm.actualizarEstado { it.copy(dgOverride = null) }; editarDg = false }) { Text("Automático") } }
            } else null
        ) {
            Text(h.dado, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(texto, { texto = it }, label = { Text("Dado") }, singleLine = true)
        }
    }
}

@Composable
private fun BloquePg(ui: HojaUi, onMenos: () -> Unit, onMas: () -> Unit, onEditarMax: () -> Unit) {
    val e = ui.estado
    val objetivo = colorPg(e.pgActuales, e.pgMaximos)
    val color by animateColorAsState(objetivo, animationSpec = tween(400), label = "pg")
    val ratio = if (e.pgMaximos > 0) e.pgActuales.toFloat() / e.pgMaximos else 0f
    SeccionCard("Puntos de golpe", accion = {
        IconButton(onClick = onEditarMax, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, "Editar PG máximos", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("-1", LocalColoresHoja.current.pgMal, e.pgActuales > 0, onMenos)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                Text("${e.pgActuales}", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = color, lineHeight = 64.sp)
                Text(" / ${e.pgMaximos}", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            }
            RoundButton("+1", LocalColoresHoja.current.pgBien, e.pgActuales < e.pgMaximos, onMas)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(ratio.coerceIn(0f, 1f)).clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)))
            )
        }
        if (e.pgActuales == 0) {
            Text(
                "Inconsciente. De -1 a -9 pierde 1 PG por asalto; a -10 muere.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun RoundButton(texto: String, color: androidx.compose.ui.graphics.Color, activo: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = activo,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = if (activo) 0.6f else 0.2f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(texto, color = color.copy(alpha = if (activo) 1f else 0.35f), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun BloqueAvisos(avisos: List<String>) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            avisos.forEach { a ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(6.dp))
                    Text(a, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
private fun BloqueAtaques(ataques: List<AtaqueHoja>) {
    SeccionCard("Ataques") {
        ataques.forEachIndexed { i, a ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.nombre, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (a.equipado) Text("EN MANO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
            }
            a.lineas.forEach { l ->
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(l.modo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("BA ${signo(l.ba)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(12.dp))
                    Text(l.dano, fontWeight = FontWeight.Bold, modifier = Modifier.width(76.dp))
                }
            }
            if (a.notas.isNotBlank()) {
                Text(a.notas, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            a.aviso?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
        if (ataques.size == 1) Text(
            "Añade armas desde la pestaña Equipo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloqueSalvaciones(caracts: List<CaracteristicaHoja>) {
    var detalle by remember { mutableStateOf<CaracteristicaHoja?>(null) }
    val colores = LocalColoresHoja.current
    SeccionCard("Salvaciones y tiradas") {
        Text(
            "1d20 + bonificador ≥ ND (+ dificultad del Narrador)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        caracts.chunked(3).forEach { fila ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { c ->
                    val color = colores.atributos[c.atributo] ?: MaterialTheme.colorScheme.primary
                    Surface(
                        modifier = Modifier.weight(1f).clickable { detalle = c },
                        shape = RoundedCornerShape(12.dp),
                        color = color.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
                    ) {
                        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c.atributo.abrev, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                                if (c.primaria) Icon(Icons.Default.Star, "Primaria", Modifier.size(12.dp), tint = color)
                            }
                            Text(signo(c.bonusTirada), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("ND ${c.nd}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    detalle?.let { c ->
        AlertDialog(
            onDismissRequest = { detalle = null },
            title = { Text("${c.atributo.nombre} (${c.atributo.abrev})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("1d20 ${signo(c.mod)} (mod.) ${signo(c.bonusTirada - c.mod)} (nivel) contra ND ${c.nd}" + if (c.primaria) " (primaria)" else " (secundaria)")
                    Text("Salvaciones contra:", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Salvaciones.amenazas[c.atributo].orEmpty().forEach { t ->
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(t, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { detalle = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun CaDialog(ui: HojaUi, vm: HojaViewModel, onDismiss: () -> Unit) {
    val ca = ui.hoja.ca
    var otros by remember { mutableStateOf(ui.estado.caOtros.toString()) }
    var forzada by remember { mutableStateOf(ui.estado.caOverride?.toString() ?: "") }
    FormularioDialog(
        "Clase de armadura",
        onDismiss = onDismiss,
        onGuardar = {
            vm.actualizarEstado { it.copy(caOtros = otros.toIntOrNull() ?: 0, caOverride = forzada.toIntOrNull()) }
            onDismiss()
        }
    ) {
        @Composable
        fun fila(etiqueta: String, valor: Int, signoMas: Boolean = true) = Row(Modifier.fillMaxWidth()) {
            Text(etiqueta, Modifier.weight(1f))
            Text(if (signoMas) signo(valor) else "$valor", fontWeight = FontWeight.Bold)
        }
        fila(if (ui.hoja.clase?.id == "mistico") "Base (místico)" else "Base", ca.base, false)
        fila("Armadura", ca.armadura)
        fila("DES", ca.des)
        fila("Escudo", ca.escudo)
        if (ca.magia != 0) fila("Objetos mágicos (el mayor)", ca.magia)
        fila("Otros", ca.otros)
        HorizontalDivider()
        fila("Total calculado", ca.base + ca.armadura + ca.des + ca.escudo + ca.magia + ca.otros, false)
        if (ca.casco > 0) {
            Text("Casco: ${signo(ca.casco)} solo contra ataques a la cabeza", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Equipa armadura, escudo y casco desde la pestaña Equipo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        CampoNumero(otros, { otros = it }, "Otros bonificadores", negativos = true)
        CampoNumero(forzada, { forzada = it }, "CA fijada a mano", soporte = "Déjalo vacío para calcularla")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloqueCondiciones(condiciones: List<Condicion>, onGuardar: (Condicion) -> Unit, onEliminar: (Condicion) -> Unit) {
    var editando by remember { mutableStateOf<Condicion?>(null) }
    SeccionCard("Condiciones", accion = {
        IconButton(onClick = { editando = Condicion(personajeId = 0, nombre = "") }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, "Añadir condición", tint = MaterialTheme.colorScheme.primary)
        }
    }) {
        if (condiciones.isEmpty()) {
            Text("Sin condiciones activas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            condiciones.forEach { c ->
                InputChip(
                    selected = false,
                    onClick = { editando = c },
                    label = { Text(if (c.nota.isBlank()) c.nombre else "${c.nombre}: ${c.nota}", maxLines = 1) }
                )
            }
        }
    }
    editando?.let { c ->
        var nombre by remember(c) { mutableStateOf(c.nombre) }
        var nota by remember(c) { mutableStateOf(c.nota) }
        FormularioDialog(
            if (c.id == 0L) "Nueva condición" else "Condición",
            onDismiss = { editando = null },
            onGuardar = { onGuardar(c.copy(nombre = nombre.trim(), nota = nota.trim())); editando = null },
            guardarActivo = nombre.isNotBlank(),
            extraBoton = if (c.id != 0L) {
                { TextButton(onClick = { onEliminar(c); editando = null }) { Text("Quitar", color = MaterialTheme.colorScheme.error) } }
            } else null
        ) {
            OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre (bendecido, envenenado...)") }, singleLine = true)
            OutlinedTextField(nota, { nota = it }, label = { Text("Nota (opcional)") })
        }
    }
}

/** Para pestañas que muestran el color de cada característica. */
@Composable
fun colorAtributo(a: Atributo) = LocalColoresHoja.current.atributos[a] ?: MaterialTheme.colorScheme.primary
