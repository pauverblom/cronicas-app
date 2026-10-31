package io.github.pauverblom.cronicas.ui.hoja

import androidx.compose.foundation.layout.widthIn
import io.github.pauverblom.cronicas.ui.plataforma.anchoVentanaDp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import io.github.pauverblom.cronicas.ui.common.signo
import io.github.pauverblom.cronicas.ui.hud.Aviso
import io.github.pauverblom.cronicas.ui.hud.Boton
import io.github.pauverblom.cronicas.ui.hud.BotonIcono
import io.github.pauverblom.cronicas.ui.hud.Campo
import io.github.pauverblom.cronicas.ui.hud.Dialogo
import io.github.pauverblom.cronicas.ui.hud.Etiqueta
import io.github.pauverblom.cronicas.ui.hud.FilaSalvacion
import io.github.pauverblom.cronicas.ui.hud.Filete
import io.github.pauverblom.cronicas.ui.hud.Glifo
import io.github.pauverblom.cronicas.ui.hud.IconoGlifo
import io.github.pauverblom.cronicas.ui.hud.Insignia
import io.github.pauverblom.cronicas.ui.hud.ModoAtaque
import io.github.pauverblom.cronicas.ui.hud.OrbePg
import io.github.pauverblom.cronicas.ui.hud.PlacaAtaque
import io.github.pauverblom.cronicas.ui.hud.Separador
import io.github.pauverblom.cronicas.ui.hud.Subtitulo
import io.github.pauverblom.cronicas.ui.hud.Variante
import io.github.pauverblom.cronicas.ui.hud.Ventana
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos

/** Separación entre ventanas: deja sitio a la clave que cuelga de una y a la placa del título de la siguiente. */
private val EntreVentanas = 30.dp

/** Columna desplazable en móvil; dos columnas a partir de 700 dp. */
@Composable
fun ColumnasHoja(izquierda: @Composable ColumnScope.() -> Unit, derecha: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (maxWidth >= 700.dp) {
            // En monitores muy anchos las columnas no se estiran más allá de 1400 dp entre las dos
            Row(Modifier.widthIn(max = 1400.dp).fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 22.dp, bottom = 48.dp).navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(EntreVentanas)
                ) { izquierda() }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 22.dp, bottom = 48.dp).navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(EntreVentanas)
                ) { derecha() }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 48.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(EntreVentanas)
            ) {
                izquierda(); derecha()
            }
        }
    }
}

/** Ancho a partir del cual la ficha va a dos columnas. */
@Composable
private fun esAncha(): Boolean = anchoVentanaDp() >= 700

private enum class Fijar { BA, MOV, PG_MAX }

@Composable
fun ResumenTab(ui: HojaUi, vm: HojaViewModel) {
    var fijar by remember { mutableStateOf<Fijar?>(null) }
    var verCa by remember { mutableStateOf(false) }
    var editarDg by remember { mutableStateOf(false) }
    val h = ui.hoja
    val ancha = esAncha()

    // Móvil: Avisos → PG → CA/BA/DG/MOV → Ataques → Salvaciones → Condiciones.
    // Tablet: PG, cifras y ataques a la izquierda; salvaciones, condiciones y avisos a la derecha.
    ColumnasHoja(
        izquierda = {
            if (!ancha) BloqueAvisos(h.avisos)
            BloquePg(
                ui, onMenos = { vm.cambiarPg(-1) }, onMas = { vm.cambiarPg(1) }, onEditarMax = { fijar = Fijar.PG_MAX },
                onAtenuado = vm::golpeAtenuado, onRecuperar = vm::recuperarAtenuado
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Insignia("CA", "${h.ca.total}", Modifier.weight(1f), fijado = h.ca.forzada, onClick = { verCa = true })
                Insignia("BA", signo(h.ba), Modifier.weight(1f), fijado = h.baForzado, onClick = { fijar = Fijar.BA })
                Insignia("DG", h.dado.substringBefore(' '), Modifier.weight(1f), fijado = h.dadoForzado, onClick = { editarDg = true })
                Insignia(
                    "MOV", "${h.movimiento} m", Modifier.weight(1f),
                    sub = if (h.movimiento != h.movimientoBase) "Base ${h.movimientoBase} m" else "Correr ${h.movimiento * 4} m",
                    fijado = h.movimientoForzado,
                    onClick = { fijar = Fijar.MOV }
                )
            }
            BloqueAtaques(h.ataques, ui.estado.otrosAtaques) { texto -> vm.actualizarEstado { it.copy(otrosAtaques = texto) } }
            if (!ancha) {
                BloqueSalvaciones(h.caracteristicas)
                BloqueCondiciones(ui.condiciones, onGuardar = vm::guardarCondicion, onEliminar = vm::eliminarCondicion)
            }
        },
        derecha = {
            if (ancha) {
                BloqueSalvaciones(h.caracteristicas)
                BloqueCondiciones(ui.condiciones, onGuardar = vm::guardarCondicion, onEliminar = vm::eliminarCondicion)
                BloqueAvisos(h.avisos)
            }
        }
    )

    when (fijar) {
        Fijar.BA -> NumeroDialog(
            "Bonificador de ataque", ui.estado.baOverride,
            h.clase?.let { io.github.pauverblom.cronicas.rules.Progresion.ba(it, h.secundaria, h.nivel) },
            explicacion = "Se suma la Fuerza en cuerpo a cuerpo y la Destreza a distancia.",
            onDismiss = { fijar = null }
        ) { v -> vm.actualizarEstado { it.copy(baOverride = v) }; fijar = null }
        Fijar.MOV -> NumeroDialog(
            "Movimiento base (m)", ui.estado.movOverride,
            h.especie?.movimiento ?: 10,
            negativos = false,
            explicacion = "Marchar ${h.movimiento * 2} m · Correr ${h.movimiento * 4} m. La carga media resta 3 m; sobrecargado deja el movimiento en 3 m.",
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
                { Boton("Automático", { vm.actualizarEstado { it.copy(dgOverride = null) }; editarDg = false }, variante = Variante.DISCRETO) }
            } else null
        ) {
            Text(h.dado, style = Tipos.cuerpoSm, color = Marca.colores.tintaSuave)
            Campo(texto, { texto = it }, "Dado")
        }
    }
}

@Composable
private fun BloquePg(
    ui: HojaUi,
    onMenos: () -> Unit,
    onMas: () -> Unit,
    onEditarMax: () -> Unit,
    onAtenuado: () -> Unit,
    onRecuperar: () -> Unit
) {
    val e = ui.estado
    Ventana("Puntos de golpe") {
        // Los PG máximos siempre se anotan a mano: no hay valor calculado que marcar como fijado
        OrbePg(e.pgActuales, e.pgMaximos, e.danoAtenuado, maxFijado = false, onMenos, onMas)
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Boton("−1 atenuado", onAtenuado, activo = e.pgActuales > 0, bloque = true)
            Boton("1 minuto", onRecuperar, activo = e.danoAtenuado > 0, bloque = true)
            Boton("PG máximos", onEditarMax, Modifier.align(Alignment.CenterHorizontally), variante = Variante.DISCRETO, icono = Glifo.LAPIZ)
        }
    }
}

@Composable
private fun BloqueAvisos(avisos: List<String>) {
    if (avisos.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        avisos.forEach { a ->
            // «Carga media: -2 a las tiradas de DES…» lleva su propio título; el resto, «Aviso»
            val (titulo, texto) = if (a.startsWith("Carga ") && ": " in a) a.substringBefore(": ") to a.substringAfter(": ") else "Aviso" to a
            Aviso(texto, titulo = titulo, peligro = a.startsWith("Carga sobrecargad"))
        }
    }
}

@Composable
private fun BloqueAtaques(ataques: List<AtaqueHoja>, otrosAtaques: String, onOtrosAtaques: (String) -> Unit) {
    var editando by remember { mutableStateOf(false) }
    val c = Marca.colores
    Ventana("Ataques") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ataques.forEach { a ->
                PlacaAtaque(
                    a.nombre, a.equipado,
                    a.lineas.map { ModoAtaque(it.modo, it.ba, it.dano) },
                    a.notas.ifBlank { null }, a.aviso
                )
            }
        }
        if (ataques.size == 1) {
            Text("Añade armas desde la pestaña Equipo.", style = Tipos.caption, color = c.tintaSuave, modifier = Modifier.padding(top = 8.dp))
        }
        Separador()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Subtitulo("Otros ataques")
                Text(
                    otrosAtaques.ifBlank { "Garras, aliento, ataques especiales…" },
                    style = if (otrosAtaques.isBlank()) Tipos.lore.copy(fontSize = 15.sp, lineHeight = 22.sp) else Tipos.cuerpo,
                    color = if (otrosAtaques.isBlank()) c.tintaSuave else c.tinta
                )
            }
            BotonIcono({ editando = true }, "Editar otros ataques") { IconoGlifo(Glifo.LAPIZ) }
        }
    }
    if (editando) {
        var texto by remember { mutableStateOf(otrosAtaques) }
        FormularioDialog("Otros ataques", onDismiss = { editando = false }, onGuardar = { onOtrosAtaques(texto.trim()); editando = false }) {
            Campo(texto, { texto = it }, "Ataque, BA, daño y notas", unaLinea = false, minLineas = 3)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloqueSalvaciones(caracts: List<CaracteristicaHoja>) {
    var detalle by remember { mutableStateOf<CaracteristicaHoja?>(null) }
    val c = Marca.colores
    Ventana("Salvaciones y tiradas") {
        Text(
            "1d20 + bonificador ≥ ND (+ dificultad del Narrador)",
            style = Tipos.uiSm,
            color = c.tintaSuave,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            caracts.forEach { car ->
                FilaSalvacion(
                    car.atributo, car.bonusTirada, car.nd, car.primaria,
                    penalizada = car.penalizacionCarga != 0, falla = car.falloAutomatico, onClick = { detalle = car }
                )
            }
        }
    }
    detalle?.let { car ->
        Dialogo(
            car.atributo.nombre,
            onDismiss = { detalle = null },
            acciones = { Boton("Cerrar", { detalle = null }, variante = Variante.DISCRETO) }
        ) {
            val nivel = car.bonusTirada - car.mod - car.penalizacionCarga
            val carga = if (car.penalizacionCarga != 0) " ${signo(car.penalizacionCarga)} (carga media)" else ""
            Text("1d20 ${signo(car.mod)} (mod.) ${signo(nivel)} (nivel)$carga contra ND ${car.nd}" + if (car.primaria) " (primaria)" else " (secundaria)")
            if (car.falloAutomatico) {
                Text("Sobrecargado: las tiradas de ${car.atributo.nombre} fallan automáticamente.", style = Tipos.cuerpoSm, color = Marca.colores.alerta)
            }
            Subtitulo("Salvaciones contra:")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Salvaciones.amenazas[car.atributo].orEmpty().forEach { t -> Etiqueta(t) }
            }
        }
    }
}

@Composable
private fun CaDialog(ui: HojaUi, vm: HojaViewModel, onDismiss: () -> Unit) {
    val ca = ui.hoja.ca
    val c = Marca.colores
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
        fun fila(etiqueta: String, valor: Int, signoMas: Boolean = true, total: Boolean = false) = Column {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(etiqueta, style = if (total) Tipos.ui else Tipos.uiSm, color = if (total) c.tinta else c.tintaSuave, modifier = Modifier.weight(1f))
                Text(if (signoMas) signo(valor) else "$valor", style = Tipos.cifraMd.copy(fontWeight = FontWeight.Bold), color = if (total) c.oro else c.tinta)
            }
            if (!total) Filete()
        }
        Column {
            fila(if (ui.hoja.clase?.id == "mistico") "Base (místico)" else "Base", ca.base, false)
            fila(ca.nombreArmadura?.let { "Armadura ($it)" } ?: "Armadura (ninguna)", ca.armadura)
            fila("Destreza", ca.des)
            fila(ca.nombreEscudo?.let { "Escudo ($it)" } ?: "Escudo", ca.escudo)
            if (ca.magia != 0) fila("Objetos mágicos (el mayor)", ca.magia)
            fila("Otros", ca.otros)
            Spacer(Modifier.height(4.dp))
            fila("Total calculado", ca.base + ca.armadura + ca.des + ca.escudo + ca.magia + ca.otros, false, total = true)
        }
        if (ca.casco > 0) {
            Text("${ca.nombreCasco ?: "Casco"}: ${signo(ca.casco)} solo contra ataques a la cabeza", style = Tipos.cuerpoSm, color = c.tintaSuave)
        }
        Text("Equipa armadura, escudo y casco desde la pestaña Equipo.", style = Tipos.cuerpoSm, color = c.tintaSuave)
        CampoNumero(otros, { otros = it }, "Otros bonificadores", negativos = true)
        CampoNumero(forzada, { forzada = it }, "CA fijada a mano", soporte = "Déjalo vacío para calcularla")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloqueCondiciones(condiciones: List<Condicion>, onGuardar: (Condicion) -> Unit, onEliminar: (Condicion) -> Unit) {
    var editando by remember { mutableStateOf<Condicion?>(null) }
    val c = Marca.colores
    Ventana("Condiciones") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (condiciones.isEmpty()) {
                Text("Sin condiciones activas", style = Tipos.lore.copy(fontSize = Tipos.cuerpo.fontSize), color = c.tintaSuave)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    condiciones.forEach { cond ->
                        Etiqueta(cond.nombre, nota = cond.nota.ifBlank { null }, onClick = { editando = cond })
                    }
                }
            }
            Boton(
                "Nueva condición", { editando = Condicion(personajeId = 0, nombre = "") },
                Modifier.align(Alignment.CenterHorizontally), icono = Glifo.MAS
            )
        }
    }
    editando?.let { cond ->
        var nombre by remember(cond) { mutableStateOf(cond.nombre) }
        var nota by remember(cond) { mutableStateOf(cond.nota) }
        FormularioDialog(
            if (cond.id == 0L) "Nueva condición" else "Condición",
            onDismiss = { editando = null },
            onGuardar = { onGuardar(cond.copy(nombre = nombre.trim(), nota = nota.trim())); editando = null },
            guardarActivo = nombre.isNotBlank(),
            extraBoton = if (cond.id != 0L) {
                { Boton("Quitar", { onEliminar(cond); editando = null }, variante = Variante.PELIGRO_DISCRETO) }
            } else null
        ) {
            Campo(nombre, { nombre = it }, "Nombre", marcador = "bendecido, envenenado…")
            Campo(nota, { nota = it }, "Nota (opcional)")
        }
    }
}

/** Para pestañas que muestran el color de cada característica. */
@Composable
fun colorAtributo(a: Atributo) = Marca.colores.atributo(a)
