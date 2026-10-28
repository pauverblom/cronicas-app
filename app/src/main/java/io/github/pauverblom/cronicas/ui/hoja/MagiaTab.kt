package io.github.pauverblom.cronicas.ui.hoja

import io.github.pauverblom.cronicas.ui.plataforma.rememberAviso
import io.github.pauverblom.cronicas.ui.plataforma.rememberAbrirArchivo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pauverblom.cronicas.data.Conjuro
import io.github.pauverblom.cronicas.data.ConjuroConDetalle
import io.github.pauverblom.cronicas.rules.MagiaHoja
import io.github.pauverblom.cronicas.ui.common.CampoNumero
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
import io.github.pauverblom.cronicas.ui.common.FormularioDialog
import io.github.pauverblom.cronicas.ui.common.NumeroDialog
import io.github.pauverblom.cronicas.ui.hud.Boton
import io.github.pauverblom.cronicas.ui.hud.BotonIcono
import io.github.pauverblom.cronicas.ui.hud.Campo
import io.github.pauverblom.cronicas.ui.hud.Casilla
import io.github.pauverblom.cronicas.ui.hud.Contador
import io.github.pauverblom.cronicas.ui.hud.EntradaConjuro
import io.github.pauverblom.cronicas.ui.hud.EspacioNivel
import io.github.pauverblom.cronicas.ui.hud.EspaciosConjuro
import io.github.pauverblom.cronicas.ui.hud.Etiqueta
import io.github.pauverblom.cronicas.ui.hud.Filete
import io.github.pauverblom.cronicas.ui.hud.Glifo
import io.github.pauverblom.cronicas.ui.hud.IconoGlifo
import io.github.pauverblom.cronicas.ui.hud.Tono
import io.github.pauverblom.cronicas.ui.hud.Variante
import io.github.pauverblom.cronicas.ui.hud.Ventana
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos

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
    val buscador = rememberBuscador(ui, vm)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "espacios") {
            Ventana("Espacios de conjuro") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ui.hoja.magia.forEach { m -> FilaEspacios(m, ui.conjuros, onForzar = { nivel -> forzar = m to nivel }) }
                }
                Boton("Descansar", { descanso = true }, Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp), variante = Variante.DORADO)
            }
        }
        item(key = "vistas") {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                Vista.entries.forEach { v -> Etiqueta(v.titulo, tono = if (vista == v) Tono.ORO else Tono.LISO, onClick = { vista = v }) }
            }
        }
        when (vista) {
            Vista.PREPARADOS -> listaPreparados(ui, vm)
            Vista.GRIMORIO -> listaGrimorio(ui, vm)
            Vista.CATALOGO -> listaCatalogo(ui, vm, catalogo, buscador)
        }
        item(key = "fin") { Spacer(Modifier.navigationBarsPadding()) }
    }

    if (descanso) {
        ConfirmarDialog(
            "Descanso",
            "Tras descansar y estudiar o rezar, todos los conjuros preparados vuelven a estar disponibles.",
            confirmar = "Descansar",
            destructivo = false,
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
            explicacion = "Incluye los conjuros adicionales por ${m.clase.atributoConjuros?.nombre ?: "característica"} alta.",
            onDismiss = { forzar = null }
        ) { v -> vm.forzarEspacios(nivel, v); forzar = null }
    }
}

@Composable
private fun FilaEspacios(m: MagiaHoja, conjuros: List<ConjuroConDetalle>, onForzar: (Int) -> Unit) {
    val preparados = conjuros.groupBy { it.cp.nivel }.mapValues { (_, l) -> l.sumOf { it.cp.preparadoVeces } }
    val niveles = (0..9).filter { m.espacios[it] > 0 || (preparados[it] ?: 0) > 0 }
    EspaciosConjuro(
        m.clase.nombre, m.nivelLanzador,
        niveles.map { n -> EspacioNivel(n, preparados[n] ?: 0, m.espacios[n], n in m.forzados) },
        onNivel = onForzar
    )
}

@Composable
fun DetallesConjuro(c: Conjuro) {
    val p = Marca.colores
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val niveles = c.nivelesPorClase.entries.joinToString { "${NOMBRE_LISTA[it.key] ?: it.key} ${it.value}" }
        if (niveles.isNotBlank()) Text(niveles.uppercase(), style = Tipos.etiquetaCaps, color = p.azulArcano)
        Text(
            listOf("TL" to c.tl, "Alcance" to c.alcance, "Comp." to c.componentes, "Duración" to c.duracion, "TS" to c.ts, "RC" to c.rc)
                .filter { it.second.isNotBlank() }.joinToString(" · ") { "${it.first}: ${it.second}" },
            style = Tipos.caption,
            color = p.tintaSuave
        )
        if (c.descripcion.isNotBlank()) Text(c.descripcion, style = Tipos.cuerpo, color = p.tinta)
    }
}

private fun tituloNivel(n: Int) = if (n == 0) "Trucos (nivel 0)" else "Nivel $n"

@Composable
private fun Vacio(texto: String) {
    Text(texto, Modifier.fillMaxWidth().padding(24.dp), style = Tipos.lore, color = Marca.colores.tintaSuave, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}

/** Conjuros preparados agrupados por nivel, una ventana por nivel con una casilla de llama por copia. */
private fun androidx.compose.foundation.lazy.LazyListScope.listaPreparados(ui: HojaUi, vm: HojaViewModel) {
    val preparados = ui.conjuros.filter { it.cp.preparadoVeces > 0 }
    if (preparados.isEmpty()) {
        item(key = "vacio") { Vacio("No hay conjuros preparados. Prepáralos desde «Conocidos».") }
        return
    }
    preparados.groupBy { it.cp.nivel }.toSortedMap().forEach { (nivel, lista) ->
        item(key = "p$nivel") {
            Ventana(tituloNivel(nivel), Modifier.padding(top = 14.dp)) {
                lista.forEach { d ->
                    var abierto by remember(d.cp.conjuroId) { mutableStateOf(false) }
                    val c = d.conjuro.toConjuro()
                    EntradaConjuro(
                        c.nombre, d.cp.preparadoVeces, d.cp.usadoVeces,
                        onLanzadas = { vm.setUsados(d.cp, it) },
                        onNombre = { abierto = !abierto }
                    )
                    if (abierto) Column(Modifier.padding(start = 8.dp, top = 8.dp, bottom = 12.dp)) { DetallesConjuro(c) }
                }
            }
        }
    }
}

/** Fila de un conjuro con sus acciones a la derecha; tocar el nombre despliega los detalles. */
@Composable
private fun FilaConjuro(nombre: String, sub: String?, acciones: @Composable () -> Unit, detalle: @Composable () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val c = Marca.colores
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClickLabel = if (abierto) "Plegar" else "Ver detalles") { abierto = !abierto }.padding(vertical = 6.dp)) {
                Text(nombre, style = Tipos.ui, color = c.tinta)
                sub?.let { Text(it, style = Tipos.caption, color = c.azulArcano) }
            }
            acciones()
        }
        if (abierto) Column(Modifier.padding(bottom = 12.dp)) { detalle() }
        Filete()
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.listaGrimorio(ui: HojaUi, vm: HojaViewModel) {
    if (ui.conjuros.isEmpty()) {
        item(key = "vacio") { Vacio("Aún no conoces conjuros. Añádelos desde el «Catálogo».") }
        return
    }
    ui.conjuros.groupBy { it.cp.nivel }.toSortedMap().forEach { (nivel, lista) ->
        item(key = "g$nivel") {
            var olvidar by remember { mutableStateOf<ConjuroConDetalle?>(null) }
            Ventana(tituloNivel(nivel), Modifier.padding(top = 14.dp)) {
                lista.forEach { d ->
                    val c = d.conjuro.toConjuro()
                    FilaConjuro(
                        c.nombre,
                        sub = if (d.cp.preparadoVeces > 0) "Preparado ×${d.cp.preparadoVeces}" else null,
                        acciones = {
                            Contador(d.cp.preparadoVeces, { n -> vm.preparar(d.cp, n - d.cp.preparadoVeces) }, "Preparar ${c.nombre}", min = 0)
                            BotonIcono({ olvidar = d }, "Quitar ${c.nombre} de conocidos") { IconoGlifo(Glifo.CERRAR, color = Marca.colores.tintaSuave) }
                        },
                        detalle = { DetallesConjuro(c) }
                    )
                }
            }
            olvidar?.let { d ->
                ConfirmarDialog("Quitar conjuro", "¿Quitar ${d.conjuro.nombre} de tus conjuros conocidos?", confirmar = "Quitar", onDismiss = { olvidar = null }) {
                    vm.olvidar(d.cp.conjuroId)
                }
            }
        }
    }
}

/** Filtros del catálogo: viven fuera de la lista para que las filas sean perezosas. */
private class Buscador(val listas: List<String>) {
    var busqueda by mutableStateOf("")
    var nivel by mutableStateOf<Int?>(null)
    var todas by mutableStateOf(listas.isEmpty())
    var nuevo by mutableStateOf(false)
    lateinit var importarCsv: () -> Unit

    /** Nivel del conjuro para este personaje: el de su lista de clase, o el primero que tenga. */
    fun nivelPara(c: Conjuro): Int? {
        val m = c.nivelesPorClase
        return listas.firstNotNullOfOrNull { m[it] } ?: if (todas || listas.isEmpty()) m.values.minOrNull() ?: 0 else null
    }

    fun filtrar(catalogo: List<Conjuro>) = catalogo.filter { c ->
        val n = nivelPara(c) ?: return@filter false
        (nivel == null || n == nivel) && (busqueda.isBlank() || c.nombre.contains(busqueda.trim(), ignoreCase = true))
    }
}

@Composable
private fun rememberBuscador(ui: HojaUi, vm: HojaViewModel): Buscador {
    val aviso = rememberAviso()
    val listas = ui.hoja.magia.mapNotNull { it.clase.listaConjuros }
    val b = remember(listas) { Buscador(listas) }
    val csv = rememberAbrirArchivo(arrayOf("text/*", "*/*")) { archivo ->
        vm.importarCsv(archivo) { n -> aviso("$n conjuros importados") }
    }
    b.importarCsv = csv
    return b
}

private fun androidx.compose.foundation.lazy.LazyListScope.listaCatalogo(ui: HojaUi, vm: HojaViewModel, catalogo: List<Conjuro>, b: Buscador) {
    val conocidos = ui.conjuros.map { it.cp.conjuroId }.toSet()
    val filtrados = b.filtrar(catalogo)
    item(key = "buscador") {
        Ventana("Catálogo", Modifier.padding(top = 14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Campo(b.busqueda, { b.busqueda = it }, null, marcador = "Buscar conjuro")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    item { Etiqueta("Todos", tono = if (b.nivel == null) Tono.ORO else Tono.LISO, onClick = { b.nivel = null }) }
                    items((0..9).toList()) { n -> Etiqueta("$n", tono = if (b.nivel == n) Tono.ORO else Tono.LISO, onClick = { b.nivel = if (b.nivel == n) null else n }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (b.listas.isNotEmpty()) {
                        Casilla(b.todas, { b.todas = it }, Modifier.weight(1f), etiqueta = "Incluir otras clases")
                    } else Spacer(Modifier.weight(1f))
                    Boton("Nuevo", { b.nuevo = true }, variante = Variante.DISCRETO)
                    Boton("CSV", { b.importarCsv() }, variante = Variante.DISCRETO, descripcion = "Importar conjuros de un CSV")
                }
            }
        }
        if (b.nuevo) NuevoConjuroDialog(b.listas.firstOrNull(), onDismiss = { b.nuevo = false }) { c, n -> vm.crearConjuro(c, n); b.nuevo = false }
    }
    if (filtrados.isEmpty()) item(key = "ninguno") { Vacio("Ningún conjuro coincide.") }
    items(filtrados, key = { "c${it.id}" }) { c ->
        val n = b.nivelPara(c) ?: 0
        FilaConjuro(
            c.nombre,
            sub = c.nivelesPorClase.entries.joinToString { "${NOMBRE_LISTA[it.key] ?: it.key} ${it.value}" }.ifBlank { "Nivel $n" } +
                if (c.esPropio) " · propio" else "",
            acciones = {
                if (c.id in conocidos) {
                    IconoGlifo(Glifo.VISTO, Modifier.padding(14.dp), color = Marca.colores.oro, descripcion = "Conocido")
                } else {
                    BotonIcono({ vm.aprender(c, n) }, "Añadir ${c.nombre} a conocidos") { IconoGlifo(Glifo.MAS) }
                }
            },
            detalle = { DetallesConjuro(c) }
        )
    }
}

@Composable
fun NuevoConjuroDialog(lista: String?, onDismiss: () -> Unit, nombre: String = "", onGuardar: (Conjuro, Int) -> Unit) {
    var c by remember { mutableStateOf(Conjuro(nombre = nombre)) }
    var nivel by remember { mutableStateOf("1") }
    FormularioDialog("Nuevo conjuro", onDismiss = onDismiss, guardarActivo = c.nombre.isNotBlank(), onGuardar = {
        val n = (nivel.toIntOrNull() ?: 0).coerceIn(0, 9)
        onGuardar(c.copy(nombre = c.nombre.trim(), niveles = Conjuro.formatNiveles(mapOf((lista ?: LISTA_PROPIA) to n))), n)
    }) {
        Campo(c.nombre, { c = c.copy(nombre = it) }, "Nombre")
        CampoNumero(nivel, { nivel = it }, "Nivel (0-9)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo(c.tl, { c = c.copy(tl = it) }, "TL", Modifier.weight(1f))
            Campo(c.componentes, { c = c.copy(componentes = it) }, "Comp.", Modifier.weight(1f))
        }
        Campo(c.alcance, { c = c.copy(alcance = it) }, "Alcance")
        Campo(c.duracion, { c = c.copy(duracion = it) }, "Duración")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo(c.ts, { c = c.copy(ts = it) }, "TS", Modifier.weight(1f))
            Campo(c.rc, { c = c.copy(rc = it) }, "RC", Modifier.weight(1f))
        }
        Campo(c.descripcion, { c = c.copy(descripcion = it) }, "Descripción", unaLinea = false, minLineas = 3)
    }
}
