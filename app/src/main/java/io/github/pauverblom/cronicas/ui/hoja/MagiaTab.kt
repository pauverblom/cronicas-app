package io.github.pauverblom.cronicas.ui.hoja

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pauverblom.cronicas.data.Conjuro
import io.github.pauverblom.cronicas.data.ConjuroConDetalle
import io.github.pauverblom.cronicas.rules.MagiaHoja
import io.github.pauverblom.cronicas.ui.common.CampoNumero
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
import io.github.pauverblom.cronicas.ui.common.FormularioDialog
import io.github.pauverblom.cronicas.ui.common.NumeroDialog

/** [LISTA_PROPIA] guarda el nivel de los conjuros creados sin lista de clase. */
private val NOMBRE_LISTA = mapOf("clerigo" to "Clérigo", "druida" to "Druida", "ilusionista" to "Ilusionista", "mago" to "Mago", LISTA_PROPIA to "Nivel")
private const val LISTA_PROPIA = "propio"

private enum class Vista(val titulo: String) { PREPARADOS("Preparados"), GRIMORIO("Conocidos"), CATALOGO("Catálogo") }

@Composable
fun MagiaTab(ui: HojaUi, vm: HojaViewModel) {
    var vista by rememberSaveable { mutableStateOf(Vista.PREPARADOS) }
    var descanso by remember { mutableStateOf(false) }
    var forzar by remember { mutableStateOf<Pair<MagiaHoja, Int>?>(null) }
    val catalogo by vm.catalogoConjuros.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ui.hoja.magia.forEach { m -> FilaEspacios(m, ui.conjuros, onForzar = { nivel -> forzar = m to nivel }) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    Vista.entries.forEachIndexed { i, v ->
                        SegmentedButton(selected = vista == v, onClick = { vista = v }, shape = SegmentedButtonDefaults.itemShape(i, Vista.entries.size)) {
                            Text(v.titulo, fontSize = 12.sp)
                        }
                    }
                }
                IconButton(onClick = { descanso = true }) { Icon(Icons.Default.Bedtime, "Descanso") }
            }
        }
        when (vista) {
            Vista.PREPARADOS -> ListaPreparados(ui, vm)
            Vista.GRIMORIO -> ListaGrimorio(ui, vm)
            Vista.CATALOGO -> ListaCatalogo(ui, vm, catalogo)
        }
    }

    if (descanso) {
        ConfirmarDialog(
            "Descanso",
            "Tras descansar y estudiar o rezar, todos los conjuros preparados vuelven a estar disponibles.",
            confirmar = "Descansar",
            onDismiss = { descanso = false },
            onConfirm = vm::descansar
        )
    }
    forzar?.let { (m, nivel) ->
        NumeroDialog(
            "Espacios de nivel $nivel",
            actual = if (nivel in m.forzados) m.espacios[nivel] else null,
            calculado = m.espaciosCalculados[nivel],
            negativos = false,
            explicacion = "Incluye los conjuros adicionales por ${m.clase.atributoConjuros?.abrev ?: "característica"} alta.",
            onDismiss = { forzar = null }
        ) { v -> vm.forzarEspacios(nivel, v); forzar = null }
    }
}

@Composable
private fun FilaEspacios(m: MagiaHoja, conjuros: List<ConjuroConDetalle>, onForzar: (Int) -> Unit) {
    val preparados = conjuros.groupBy { it.cp.nivel }.mapValues { (_, l) -> l.sumOf { it.cp.preparadoVeces } }
    val niveles = (0..9).filter { m.espacios[it] > 0 || (preparados[it] ?: 0) > 0 }
    Text(
        "${m.clase.nombre} · nivel de lanzador ${m.nivelLanzador}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(niveles) { n ->
            val prep = preparados[n] ?: 0
            val exceso = prep > m.espacios[n]
            val color = if (exceso) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            Surface(
                modifier = Modifier.clickable { onForzar(n) },
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (n == 0) "Trucos" else "Nv $n", fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                    Text("$prep/${m.espacios[n]}" + if (n in m.forzados) "*" else "", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetallesConjuro(c: Conjuro) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val niveles = c.nivelesPorClase.entries.joinToString { "${NOMBRE_LISTA[it.key] ?: it.key} ${it.value}" }
        if (niveles.isNotBlank()) Text(niveles, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            listOf("TL" to c.tl, "Alcance" to c.alcance, "Comp." to c.componentes, "Duración" to c.duracion, "TS" to c.ts, "RC" to c.rc)
                .filter { it.second.isNotBlank() }.joinToString(" · ") { "${it.first}: ${it.second}" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (c.descripcion.isNotBlank()) Text(c.descripcion, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TarjetaConjuro(nombre: String, sub: String? = null, acciones: @Composable () -> Unit, detalle: @Composable () -> Unit, extra: (@Composable () -> Unit)? = null) {
    var abierto by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { abierto = !abierto },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(nombre, style = MaterialTheme.typography.titleSmall)
                    sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                acciones()
            }
            extra?.invoke()
            if (abierto) {
                Spacer(Modifier.height(6.dp))
                detalle()
            }
        }
    }
}

private fun tituloNivel(n: Int) = if (n == 0) "Trucos (nivel 0)" else "Nivel $n"

@Composable
private fun Vacio(texto: String) {
    Text(texto, Modifier.padding(24.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListaPreparados(ui: HojaUi, vm: HojaViewModel) {
    val preparados = ui.conjuros.filter { it.cp.preparadoVeces > 0 }
    if (preparados.isEmpty()) {
        Vacio("No hay conjuros preparados. Prepáralos desde «Conocidos».")
        return
    }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        preparados.groupBy { it.cp.nivel }.toSortedMap().forEach { (nivel, lista) ->
            item(key = "h$nivel") { Text(tituloNivel(nivel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
            items(lista, key = { "p${it.cp.conjuroId}" }) { d ->
                val c = d.conjuro.toConjuro()
                val restantes = d.cp.preparadoVeces - d.cp.usadoVeces
                TarjetaConjuro(
                    c.nombre,
                    sub = if (restantes == 0) "Gastado" else "$restantes de ${d.cp.preparadoVeces} disponibles",
                    acciones = {},
                    detalle = { DetallesConjuro(c) },
                    extra = {
                        // Una casilla por cada vez que está preparado; marcarla = lanzado
                        FlowRow {
                            repeat(d.cp.preparadoVeces) { i ->
                                Checkbox(
                                    checked = i < d.cp.usadoVeces,
                                    onCheckedChange = { marcar -> vm.setUsados(d.cp, if (marcar) i + 1 else i) }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ListaGrimorio(ui: HojaUi, vm: HojaViewModel) {
    var olvidar by remember { mutableStateOf<ConjuroConDetalle?>(null) }
    if (ui.conjuros.isEmpty()) {
        Vacio("Aún no conoces conjuros. Añádelos desde el «Catálogo».")
        return
    }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ui.conjuros.groupBy { it.cp.nivel }.toSortedMap().forEach { (nivel, lista) ->
            item(key = "h$nivel") { Text(tituloNivel(nivel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
            items(lista, key = { "g${it.cp.conjuroId}" }) { d ->
                val c = d.conjuro.toConjuro()
                TarjetaConjuro(
                    c.nombre,
                    sub = if (d.cp.preparadoVeces > 0) "Preparado ×${d.cp.preparadoVeces}" else null,
                    acciones = {
                        IconButton(onClick = { vm.preparar(d.cp, -1) }, enabled = d.cp.preparadoVeces > 0) { Icon(Icons.Default.Remove, "Preparar uno menos") }
                        Text("${d.cp.preparadoVeces}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { vm.preparar(d.cp, 1) }) { Icon(Icons.Default.Add, "Preparar") }
                        IconButton(onClick = { olvidar = d }) { Icon(Icons.Default.Close, "Quitar de conocidos", tint = MaterialTheme.colorScheme.outline) }
                    },
                    detalle = { DetallesConjuro(c) }
                )
            }
        }
    }
    olvidar?.let { d ->
        ConfirmarDialog("Quitar conjuro", "¿Quitar ${d.conjuro.nombre} de tus conjuros conocidos?", confirmar = "Quitar", onDismiss = { olvidar = null }) {
            vm.olvidar(d.cp.conjuroId)
        }
    }
}

@Composable
private fun ListaCatalogo(ui: HojaUi, vm: HojaViewModel, catalogo: List<Conjuro>) {
    val context = LocalContext.current
    val listas = ui.hoja.magia.mapNotNull { it.clase.listaConjuros }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var nivel by rememberSaveable { mutableStateOf<Int?>(null) }
    var todas by rememberSaveable { mutableStateOf(listas.isEmpty()) }
    var nuevo by remember { mutableStateOf(false) }
    val conocidos = ui.conjuros.map { it.cp.conjuroId }.toSet()
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importarCsv(uri) { n -> Toast.makeText(context, "$n conjuros importados", Toast.LENGTH_SHORT).show() }
    }

    /** Nivel del conjuro para este personaje: el de su lista de clase, o el primero que tenga. */
    fun nivelPara(c: Conjuro): Int? {
        val m = c.nivelesPorClase
        return listas.firstNotNullOfOrNull { m[it] } ?: if (todas || listas.isEmpty()) m.values.minOrNull() ?: 0 else null
    }

    val filtrados = catalogo.filter { c ->
        val n = nivelPara(c) ?: return@filter false
        (nivel == null || n == nivel) && (busqueda.isBlank() || c.nombre.contains(busqueda.trim(), ignoreCase = true))
    }

    Column {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(busqueda, { busqueda = it }, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Buscar conjuro") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { FilterChip(selected = nivel == null, onClick = { nivel = null }, label = { Text("Todos") }) }
                items((0..9).toList()) { n -> FilterChip(selected = nivel == n, onClick = { nivel = if (nivel == n) null else n }, label = { Text("$n") }) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (listas.isNotEmpty()) {
                    Checkbox(todas, { todas = it })
                    Text("Incluir otras clases", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                } else Spacer(Modifier.weight(1f))
                TextButton(onClick = { nuevo = true }) { Text("Nuevo") }
                TextButton(onClick = { csv.launch(arrayOf("text/*", "*/*")) }) { Text("CSV") }
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtrados, key = { it.id }) { c ->
                val n = nivelPara(c) ?: 0
                val sabido = c.id in conocidos
                TarjetaConjuro(
                    c.nombre,
                    sub = c.nivelesPorClase.entries.joinToString { "${NOMBRE_LISTA[it.key] ?: it.key} ${it.value}" }.ifBlank { "Nivel $n" } +
                        if (c.esPropio) " · propio" else "",
                    acciones = {
                        if (sabido) Icon(Icons.Default.Check, "Conocido", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
                        else IconButton(onClick = { vm.aprender(c, n) }) { Icon(Icons.Default.Add, "Añadir a conocidos") }
                    },
                    detalle = { DetallesConjuro(c) }
                )
            }
        }
    }

    if (nuevo) NuevoConjuroDialog(listas.firstOrNull(), onDismiss = { nuevo = false }) { c, n -> vm.crearConjuro(c, n); nuevo = false }
}

@Composable
fun NuevoConjuroDialog(lista: String?, onDismiss: () -> Unit, nombre: String = "", onGuardar: (Conjuro, Int) -> Unit) {
    var c by remember { mutableStateOf(Conjuro(nombre = nombre)) }
    var nivel by remember { mutableStateOf("1") }
    FormularioDialog("Nuevo conjuro", onDismiss = onDismiss, guardarActivo = c.nombre.isNotBlank(), onGuardar = {
        val n = (nivel.toIntOrNull() ?: 0).coerceIn(0, 9)
        onGuardar(c.copy(nombre = c.nombre.trim(), niveles = Conjuro.formatNiveles(mapOf((lista ?: LISTA_PROPIA) to n))), n)
    }) {
        OutlinedTextField(c.nombre, { c = c.copy(nombre = it) }, label = { Text("Nombre") }, singleLine = true)
        CampoNumero(nivel, { nivel = it }, "Nivel (0-9)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(c.tl, { c = c.copy(tl = it) }, label = { Text("TL") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(c.componentes, { c = c.copy(componentes = it) }, label = { Text("Comp.") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(c.alcance, { c = c.copy(alcance = it) }, label = { Text("Alcance") }, singleLine = true)
        OutlinedTextField(c.duracion, { c = c.copy(duracion = it) }, label = { Text("Duración") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(c.ts, { c = c.copy(ts = it) }, label = { Text("TS") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(c.rc, { c = c.copy(rc = it) }, label = { Text("RC") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(c.descripcion, { c = c.copy(descripcion = it) }, label = { Text("Descripción") }, minLines = 3)
    }
}
