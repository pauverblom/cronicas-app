package io.github.pauverblom.cronicas.ui.hoja

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pauverblom.cronicas.data.Compania
import io.github.pauverblom.cronicas.data.Objeto
import io.github.pauverblom.cronicas.data.Tesoro
import io.github.pauverblom.cronicas.rules.Carga
import io.github.pauverblom.cronicas.rules.Monedas
import io.github.pauverblom.cronicas.rules.data.ArticuloDef
import io.github.pauverblom.cronicas.rules.data.Catalogo
import io.github.pauverblom.cronicas.rules.data.Categoria
import io.github.pauverblom.cronicas.rules.data.TipoArma
import io.github.pauverblom.cronicas.ui.common.CampoNumero
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
import io.github.pauverblom.cronicas.ui.common.Desplegable
import io.github.pauverblom.cronicas.ui.common.DesplegableEditable
import io.github.pauverblom.cronicas.ui.common.FormularioDialog
import io.github.pauverblom.cronicas.ui.common.SeccionCard
import io.github.pauverblom.cronicas.ui.common.formatoVe
import io.github.pauverblom.cronicas.ui.common.signo
import io.github.pauverblom.cronicas.ui.theme.LocalColoresHoja

private val EQUIPABLES = setOf(Categoria.ARMA, Categoria.ARMADURA, Categoria.ESCUDO, Categoria.CASCO, Categoria.EQUIPO)

@Composable
fun EquipoTab(ui: HojaUi, vm: HojaViewModel) {
    var catalogo by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Objeto?>(null) }
    var compania by remember { mutableStateOf<Compania?>(null) }

    ColumnasHoja(
        izquierda = {
            BloqueCarga(ui)
            BloqueMonedas(ui.tesoro, vm::guardarTesoro)
            BloqueCompania(ui, onEditar = { compania = it })
        },
        derecha = {
            BloqueInventario(ui, onAnadir = { catalogo = true }, onEditar = { editando = it }, onEquipar = vm::alternarEquipado)
        }
    )

    if (catalogo) {
        CatalogoSheet(
            tesoro = ui.tesoro,
            onDismiss = { catalogo = false },
            onAnadir = { art, lotes, comprar, fin -> vm.anadirDeCatalogo(art, lotes, comprar, fin) },
            onPersonalizado = { catalogo = false; editando = Objeto(personajeId = ui.personaje.id, nombre = "") }
        )
    }
    editando?.let { o ->
        ObjetoDialog(o, ui, onDismiss = { editando = null }, onGuardar = { vm.guardarObjeto(it); editando = null }, onEliminar = { vm.eliminarObjeto(o); editando = null })
    }
    compania?.let { c ->
        CompaniaDialog(c, onDismiss = { compania = null }, onGuardar = { vm.guardarCompania(it); compania = null }, onEliminar = { vm.eliminarCompania(c); compania = null })
    }
}

@Composable
private fun BloqueCarga(ui: HojaUi) {
    val c = ui.hoja.carga
    val colores = LocalColoresHoja.current
    val color = when (c.banda) {
        Carga.Banda.LIGERA -> colores.pgBien
        Carga.Banda.MEDIA -> colores.pgMedio
        Carga.Banda.SOBRECARGADO -> colores.pgMal
    }
    SeccionCard("Carga") {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatoVe(c.total), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(" / ${c.limite} VE", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(Modifier.weight(1f))
            Text(c.banda.etiqueta.uppercase(), color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        // La barra llega hasta 3 × LC, el límite de la carga media; la marca indica LC
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((c.total / (c.limite * 3f).coerceAtLeast(1f)).coerceIn(0f, 1f)).background(color))
            Box(Modifier.fillMaxHeight().fillMaxWidth(1f / 3f)) {
                Box(Modifier.align(Alignment.CenterEnd).width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
            }
        }
        Text(
            "Límite de carga = FUE${if (ui.hoja.car(io.github.pauverblom.cronicas.rules.Atributo.FUE).primaria) " +3" else ""}" +
                "${if (ui.hoja.car(io.github.pauverblom.cronicas.rules.Atributo.CON).primaria) " +3 (CON)" else ""}. Media hasta ${c.limite * 3} VE.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (c.veMonedas > 0) Text("Monedas: ${c.veMonedas} VE (150 monedas = 1 VE)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Carga.efectos(c.banda)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = color) }
    }
}

@Composable
private fun BloqueMonedas(t: Tesoro, onGuardar: (Tesoro) -> Unit) {
    var editando by remember { mutableStateOf<String?>(null) }
    var gemas by remember { mutableStateOf(false) }
    val filas = listOf(
        Triple("Platino", "mpt", t.platino),
        Triple("Oro", "mo", t.oro),
        Triple("Electro", "me", t.electro),
        Triple("Plata", "mp", t.plata),
        Triple("Cobre", "mc", t.cobre)
    )
    fun con(nombre: String, v: Int): Tesoro = when (nombre) {
        "Platino" -> t.copy(platino = v)
        "Oro" -> t.copy(oro = v)
        "Electro" -> t.copy(electro = v)
        "Plata" -> t.copy(plata = v)
        else -> t.copy(cobre = v)
    }
    SeccionCard("Tesoro") {
        filas.forEach { (nombre, abrev, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nombre, Modifier.weight(1f))
                IconButton(onClick = { onGuardar(con(nombre, (v - 1).coerceAtLeast(0))) }, enabled = v > 0) { Icon(Icons.Default.Remove, "Quitar 1") }
                Text(
                    "$v $abrev",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(80.dp).clickable { editando = nombre },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(onClick = { onGuardar(con(nombre, v + 1)) }) { Icon(Icons.Default.Add, "Añadir 1") }
            }
        }
        Text(
            "Total: ${Monedas.formato(Monedas.Bolsa(t.cobre, t.plata, t.electro, t.oro, t.platino).totalMc)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gemas = true }) {
            Column(Modifier.weight(1f)) {
                Text("Gemas y joyas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(t.gemas.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Default.Edit, "Editar gemas", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
    editando?.let { nombre ->
        val actual = filas.first { it.first == nombre }.third
        var texto by remember(nombre) { mutableStateOf(actual.toString()) }
        FormularioDialog(nombre, onDismiss = { editando = null }, onGuardar = {
            onGuardar(con(nombre, texto.toIntOrNull() ?: actual)); editando = null
        }) { CampoNumero(texto, { texto = it }, "Cantidad") }
    }
    if (gemas) {
        var texto by remember { mutableStateOf(t.gemas) }
        FormularioDialog("Gemas y joyas", onDismiss = { gemas = false }, onGuardar = { onGuardar(t.copy(gemas = texto)); gemas = false }) {
            OutlinedTextField(texto, { texto = it }, minLines = 3, label = { Text("Descripción y valor") })
        }
    }
}

@Composable
private fun BloqueInventario(ui: HojaUi, onAnadir: () -> Unit, onEditar: (Objeto) -> Unit, onEquipar: (Objeto) -> Unit) {
    val objetos = ui.objetos
    val companias = ui.companias.associateBy { it.id }
    SeccionCard("Inventario", accion = {
        IconButton(onClick = onAnadir, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, "Añadir objeto", tint = MaterialTheme.colorScheme.primary) }
    }) {
        if (objetos.isEmpty()) {
            Text("Inventario vacío. Pulsa + para añadir del catálogo o un objeto propio.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val sueltos = objetos.filter { it.contenedorId == null }
        val grupos = listOf(
            "Armas" to setOf(Categoria.ARMA),
            "Protección" to setOf(Categoria.ARMADURA, Categoria.ESCUDO, Categoria.CASCO),
            "Munición" to setOf(Categoria.MUNICION),
            "Equipo" to setOf(Categoria.EQUIPO),
            "Contenedores" to setOf(Categoria.CONTENEDOR)
        )
        grupos.forEach { (titulo, cats) ->
            val lista = sueltos.filter { Categoria.of(it.categoria) in cats }
            if (lista.isEmpty()) return@forEach
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            lista.forEach { o ->
                FilaObjeto(o, companias[o.companiaId]?.nombre, onEditar, onEquipar)
                if (Categoria.of(o.categoria) == Categoria.CONTENEDOR) {
                    val uso = ui.hoja.carga.usoContenedores[o.id] ?: 0f
                    o.capacidad?.let { cap ->
                        Text(
                            "Contenido: ${formatoVe(uso)} / $cap VE",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uso > cap) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                    objetos.filter { it.contenedorId == o.id }.forEach { dentro ->
                        Row(Modifier.padding(start = 16.dp)) { FilaObjeto(dentro, null, onEditar, onEquipar) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaObjeto(o: Objeto, montura: String?, onEditar: (Objeto) -> Unit, onEquipar: (Objeto) -> Unit) {
    val cat = Categoria.of(o.categoria)
    Row(Modifier.fillMaxWidth().clickable { onEditar(o) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                (if (o.cantidad > 1) "${o.cantidad} × " else "") + o.nombre + if (o.bonoMagico != 0) " ${signo(o.bonoMagico)}" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (o.equipado) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val detalle = listOfNotNull(
                o.formulaDano.takeIf { it.isNotBlank() }?.let { d -> if (o.formulaDanoDosManos.isNotBlank()) "$d / ${o.formulaDanoDosManos}" else d },
                o.ca.takeIf { it != 0 && cat != Categoria.ARMA }?.let { "CA ${signo(it + o.bonoMagico)}" },
                "VE ${formatoVe(if (cat == Categoria.CASCO && o.equipado) 0f else o.ve * o.cantidad)}",
                montura?.let { "En $it" },
                "Mágico".takeIf { o.esMagico }
            ).joinToString(" · ")
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (cat in EQUIPABLES && o.contenedorId == null && o.companiaId == null) {
            val etiqueta = when (cat) {
                Categoria.ARMA -> "En mano"
                Categoria.EQUIPO -> "Usando"
                else -> "Puesto"
            }
            FilterChipPequeno(etiqueta, o.equipado) { onEquipar(o) }
        }
    }
}

@Composable
private fun FilterChipPequeno(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(selected = seleccionado, onClick = onClick, label = { Text(texto, fontSize = 11.sp) })
}

@Composable
private fun BloqueCompania(ui: HojaUi, onEditar: (Compania) -> Unit) {
    SeccionCard("Montura y compañeros", accion = {
        IconButton(onClick = { onEditar(Compania(personajeId = ui.personaje.id)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, "Añadir", tint = MaterialTheme.colorScheme.primary)
        }
    }) {
        if (ui.companias.isEmpty()) {
            Text("Monturas, compañeros animales y familiares.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ui.companias.forEach { c ->
            Column(Modifier.fillMaxWidth().clickable { onEditar(c) }.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.nombre.ifBlank { c.especie.ifBlank { c.tipo } }, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(c.tipo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                }
                Text(
                    listOfNotNull(
                        c.especie.takeIf { it.isNotBlank() && c.nombre.isNotBlank() },
                        "PG ${c.pgActuales}/${c.pgMaximos}",
                        "CA ${c.ca}",
                        c.movimiento.takeIf { it.isNotBlank() }?.let { "Mov. $it" },
                        c.ataque.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (c.capacidadVe > 0) {
                    val carga = ui.hoja.carga.usoCompanias[c.id] ?: 0f
                    Text(
                        "Carga: ${formatoVe(carga)} / ${c.capacidadVe} VE",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (carga > c.capacidadVe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Catálogo ──

private enum class PestanaCatalogo(val titulo: String, val lista: List<ArticuloDef>) {
    ARMAS("Armas", Catalogo.armas),
    MUNICION("Munición", Catalogo.municion),
    ARMADURAS("Armaduras", Catalogo.armaduras),
    ESCUDOS("Escudos", Catalogo.escudos),
    CASCOS("Cascos", Catalogo.cascos),
    EQUIPO("Equipo", Catalogo.equipo)
}

fun detalleArticulo(a: ArticuloDef): String = listOfNotNull(
    a.dano?.let { d -> a.danoDosManos?.let { "$d / $it (2 manos)" } ?: d },
    a.ca.takeIf { it > 0 }?.let { "CA +$it" },
    a.alcance?.let { "${it} m" },
    "2 manos".takeIf { a.dosManos },
    a.capacidad?.let { "CAP $it" },
    "VE ${formatoVe(a.ve)}",
    a.unidades.takeIf { it > 1 }?.let { "lote de $it" }
).joinToString(" · ")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogoSheet(
    tesoro: Tesoro,
    onDismiss: () -> Unit,
    onAnadir: (ArticuloDef, Int, Boolean, (Boolean) -> Unit) -> Unit,
    onPersonalizado: () -> Unit
) {
    val context = LocalContext.current
    var pestana by remember { mutableStateOf(PestanaCatalogo.ARMAS) }
    var busqueda by remember { mutableStateOf("") }
    var elegido by remember { mutableStateOf<ArticuloDef?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Añadir al inventario", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onPersonalizado) { Text("Personalizado") }
            }
            OutlinedTextField(
                busqueda, { busqueda = it },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Buscar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PestanaCatalogo.entries) { p ->
                FilterChip(selected = pestana == p && busqueda.isBlank(), onClick = { pestana = p; busqueda = "" }, label = { Text(p.titulo) })
            }
        }
        val lista = if (busqueda.isBlank()) pestana.lista else Catalogo.todos.filter { it.nombre.contains(busqueda.trim(), ignoreCase = true) }
        LazyColumn(Modifier.fillMaxWidth().height(460.dp)) {
            items(lista, key = { it.id }) { a ->
                Row(
                    Modifier.fillMaxWidth().clickable { elegido = a }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(a.nombre, style = MaterialTheme.typography.bodyLarge)
                        Text(detalleArticulo(a), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (a.notas.isNotBlank()) Text(a.notas, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(Monedas.formato(a.precioMc), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
    elegido?.let { a ->
        var lotes by remember(a) { mutableStateOf("1") }
        var comprar by remember(a) { mutableStateOf(a.precioMc > 0) }
        val n = (lotes.toIntOrNull() ?: 1).coerceAtLeast(1)
        val total = a.precioMc * n
        val disponible = Monedas.Bolsa(tesoro.cobre, tesoro.plata, tesoro.electro, tesoro.oro, tesoro.platino).totalMc
        FormularioDialog(a.nombre, onDismiss = { elegido = null }, guardar = "Añadir", onGuardar = {
            onAnadir(a, n, comprar) { ok ->
                Toast.makeText(context, if (ok) "${a.nombre} añadido" else "No tienes suficiente dinero", Toast.LENGTH_SHORT).show()
            }
            elegido = null
        }) {
            Text(detalleArticulo(a), style = MaterialTheme.typography.bodySmall)
            CampoNumero(lotes, { lotes = it }, if (a.unidades > 1) "Lotes de ${a.unidades}" else "Cantidad")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Comprar (${Monedas.formato(total)})")
                    Text(
                        "Tienes ${Monedas.formato(disponible)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (disponible < total && comprar) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(comprar, { comprar = it }, enabled = a.precioMc > 0)
            }
        }
    }
}

// ── Diálogos de edición ──

@Composable
private fun ObjetoDialog(o: Objeto, ui: HojaUi, onDismiss: () -> Unit, onGuardar: (Objeto) -> Unit, onEliminar: () -> Unit) {
    var e by remember { mutableStateOf(o) }
    var cantidad by remember { mutableStateOf(o.cantidad.toString()) }
    var ve by remember { mutableStateOf(formatoVe(o.ve)) }
    var ca by remember { mutableStateOf(o.ca.toString()) }
    var bono by remember { mutableStateOf(o.bonoMagico.toString()) }
    var alcance by remember { mutableStateOf(o.alcance?.toString() ?: "") }
    var capacidad by remember { mutableStateOf(o.capacidad?.toString() ?: "") }
    var borrar by remember { mutableStateOf(false) }
    val cat = Categoria.of(e.categoria)

    // Ubicación: encima, dentro de un contenedor o cargado en una montura
    data class Lugar(val etiqueta: String, val contenedorId: Long?, val companiaId: Long?)
    val lugares = buildList {
        add(Lugar("Encima", null, null))
        if (cat != Categoria.CONTENEDOR) {
            ui.objetos.filter { Categoria.of(it.categoria) == Categoria.CONTENEDOR && it.id != o.id }
                .forEach { add(Lugar("Dentro de: ${it.nombre}", it.id, it.companiaId)) }
        }
        ui.companias.filter { it.capacidadVe > 0 }.forEach { add(Lugar("Cargado en: ${it.nombre.ifBlank { it.tipo }}", null, it.id)) }
    }
    val lugarActual = lugares.firstOrNull { it.contenedorId == e.contenedorId && (it.contenedorId != null || it.companiaId == e.companiaId) } ?: lugares.first()

    FormularioDialog(
        if (o.id == 0L) "Nuevo objeto" else "Editar objeto",
        onDismiss = onDismiss,
        onGuardar = {
            onGuardar(
                e.copy(
                    nombre = e.nombre.trim(),
                    cantidad = (cantidad.toIntOrNull() ?: 1).coerceAtLeast(0),
                    ve = ve.replace(',', '.').toFloatOrNull() ?: 0f,
                    ca = ca.toIntOrNull() ?: 0,
                    bonoMagico = bono.toIntOrNull() ?: 0,
                    alcance = alcance.toIntOrNull(),
                    capacidad = capacidad.toIntOrNull(),
                    equipado = e.equipado && e.contenedorId == null && e.companiaId == null
                )
            )
        },
        guardarActivo = e.nombre.isNotBlank(),
        extraBoton = if (o.id != 0L) {
            { TextButton(onClick = { borrar = true }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } }
        } else null
    ) {
        OutlinedTextField(e.nombre, { e = e.copy(nombre = it) }, label = { Text("Nombre") }, singleLine = true)
        Desplegable("Tipo", Categoria.entries, cat, { it.etiqueta }, { e = e.copy(categoria = it.name) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoNumero(cantidad, { cantidad = it }, "Cantidad", Modifier.weight(1f))
            CampoNumero(ve, { ve = it }, "VE por unidad", Modifier.weight(1f), decimales = true)
        }
        Desplegable("Dónde está", lugares, lugarActual, { it.etiqueta }, { l -> e = e.copy(contenedorId = l.contenedorId, companiaId = l.companiaId) })
        if (cat in EQUIPABLES && e.contenedorId == null && e.companiaId == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (cat == Categoria.ARMA) "En mano" else "Puesto / en uso", Modifier.weight(1f))
                Switch(e.equipado, { e = e.copy(equipado = it) })
            }
        }
        if (cat == Categoria.ARMA) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(e.formulaDano, { e = e.copy(formulaDano = it) }, label = { Text("Daño") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(e.formulaDanoDosManos, { e = e.copy(formulaDanoDosManos = it) }, label = { Text("A 2 manos") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Desplegable(
                "Uso", TipoArma.entries, TipoArma.of(e.tipoArma),
                { when (it) { TipoArma.CUERPO -> "Cuerpo a cuerpo"; TipoArma.ARROJADIZA -> "Cuerpo a cuerpo o arrojadiza"; TipoArma.DISTANCIA -> "A distancia" } },
                { e = e.copy(tipoArma = it.name) }
            )
            if (TipoArma.of(e.tipoArma) != TipoArma.CUERPO) CampoNumero(alcance, { alcance = it }, "Alcance base (m)")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(e.dosManos, { e = e.copy(dosManos = it) }); Text("Siempre a dos manos")
            }
        }
        if (cat in setOf(Categoria.ARMADURA, Categoria.ESCUDO, Categoria.CASCO, Categoria.EQUIPO)) {
            CampoNumero(
                ca, { ca = it }, if (cat == Categoria.EQUIPO) "Bonificador a la CA (anillo, capa...)" else "CA",
                negativos = true,
                soporte = if (cat == Categoria.EQUIPO) "De varios objetos así solo cuenta el mayor" else null
            )
        }
        if (cat == Categoria.CONTENEDOR) CampoNumero(capacidad, { capacidad = it }, "Capacidad (CAP, en VE)")
        if (cat in setOf(Categoria.ARMA, Categoria.ARMADURA, Categoria.ESCUDO, Categoria.CASCO, Categoria.MUNICION)) {
            CampoNumero(bono, { bono = it }, if (cat == Categoria.ARMA) "Bono mágico (ataque y daño)" else "Bono mágico", negativos = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(e.esMagico, { e = e.copy(esMagico = it) }); Text("Objeto mágico")
        }
        OutlinedTextField(e.notas, { e = e.copy(notas = it) }, label = { Text("Notas") })
    }
    if (borrar) {
        ConfirmarDialog("Eliminar objeto", "¿Eliminar ${o.nombre}?" + if (cat == Categoria.CONTENEDOR) " Su contenido pasará a llevarse encima." else "", onDismiss = { borrar = false }, onConfirm = onEliminar)
    }
}

@Composable
private fun CompaniaDialog(c: Compania, onDismiss: () -> Unit, onGuardar: (Compania) -> Unit, onEliminar: () -> Unit) {
    var e by remember { mutableStateOf(c) }
    var pg by remember { mutableStateOf(c.pgActuales.toString()) }
    var pgMax by remember { mutableStateOf(c.pgMaximos.toString()) }
    var ca by remember { mutableStateOf(c.ca.toString()) }
    var capacidad by remember { mutableStateOf(c.capacidadVe.toString()) }
    FormularioDialog(
        if (c.id == 0L) "Nuevo compañero" else c.nombre.ifBlank { c.tipo },
        onDismiss = onDismiss,
        onGuardar = {
            onGuardar(
                e.copy(
                    pgActuales = pg.toIntOrNull() ?: 0, pgMaximos = pgMax.toIntOrNull() ?: 0,
                    ca = ca.toIntOrNull() ?: 10, capacidadVe = capacidad.toIntOrNull() ?: 0
                )
            )
        },
        extraBoton = if (c.id != 0L) {
            { TextButton(onClick = onEliminar) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } }
        } else null
    ) {
        Desplegable("Tipo", listOf("Montura", "Compañero animal", "Familiar"), e.tipo, { it }, { e = e.copy(tipo = it) })
        OutlinedTextField(e.nombre, { e = e.copy(nombre = it) }, label = { Text("Nombre") }, singleLine = true)
        DesplegableEditable("Especie", e.especie, Catalogo.monturas.map { it.nombre }, onValor = { v ->
            val m = Catalogo.monturas.firstOrNull { it.nombre == v }
            e = e.copy(especie = v)
            if (m != null && (capacidad.toIntOrNull() ?: 0) == 0) capacidad = m.capacidadVe.toString()
        })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoNumero(pg, { pg = it }, "PG", Modifier.weight(1f))
            CampoNumero(pgMax, { pgMax = it }, "PG máx.", Modifier.weight(1f))
            CampoNumero(ca, { ca = it }, "CA", Modifier.weight(1f))
        }
        OutlinedTextField(e.movimiento, { e = e.copy(movimiento = it) }, label = { Text("Movimiento") }, singleLine = true)
        OutlinedTextField(e.ataque, { e = e.copy(ataque = it) }, label = { Text("Ataque / daño") }, singleLine = true)
        CampoNumero(capacidad, { capacidad = it }, "Carga que puede llevar (VE)", soporte = "0 si no lleva carga")
        OutlinedTextField(e.habilidades, { e = e.copy(habilidades = it) }, label = { Text("Habilidades") }, minLines = 2)
    }
}
