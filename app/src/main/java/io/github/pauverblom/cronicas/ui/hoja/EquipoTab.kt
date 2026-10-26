package io.github.pauverblom.cronicas.ui.hoja

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.pauverblom.cronicas.data.Compania
import io.github.pauverblom.cronicas.data.Conjuro
import io.github.pauverblom.cronicas.data.Objeto
import io.github.pauverblom.cronicas.data.Tesoro
import io.github.pauverblom.cronicas.rules.Atributo
import io.github.pauverblom.cronicas.rules.Carga
import io.github.pauverblom.cronicas.rules.Monedas
import io.github.pauverblom.cronicas.rules.data.ArticuloDef
import io.github.pauverblom.cronicas.rules.data.Catalogo
import io.github.pauverblom.cronicas.rules.data.Categoria
import io.github.pauverblom.cronicas.rules.data.TipoArma
import io.github.pauverblom.cronicas.ui.common.CampoNumero
import io.github.pauverblom.cronicas.ui.common.Carrusel
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
import io.github.pauverblom.cronicas.ui.common.Desplegable
import io.github.pauverblom.cronicas.ui.common.DesplegableEditable
import io.github.pauverblom.cronicas.ui.common.FormularioDialog
import io.github.pauverblom.cronicas.ui.common.Ilustracion
import io.github.pauverblom.cronicas.ui.common.Ilustraciones
import io.github.pauverblom.cronicas.ui.common.formatoVe
import io.github.pauverblom.cronicas.ui.common.signo
import io.github.pauverblom.cronicas.ui.hud.Aviso
import io.github.pauverblom.cronicas.ui.hud.BandaCarga
import io.github.pauverblom.cronicas.ui.hud.BarraCarga
import io.github.pauverblom.cronicas.ui.hud.Bolsa
import io.github.pauverblom.cronicas.ui.hud.Boton
import io.github.pauverblom.cronicas.ui.hud.BotonIcono
import io.github.pauverblom.cronicas.ui.hud.Campo
import io.github.pauverblom.cronicas.ui.hud.Casilla
import io.github.pauverblom.cronicas.ui.hud.DescripcionTarjeta
import io.github.pauverblom.cronicas.ui.hud.Etiqueta
import io.github.pauverblom.cronicas.ui.hud.Filete
import io.github.pauverblom.cronicas.ui.hud.Glifo
import io.github.pauverblom.cronicas.ui.hud.IconoGlifo
import io.github.pauverblom.cronicas.ui.hud.Moneda
import io.github.pauverblom.cronicas.ui.hud.NombreTarjeta
import io.github.pauverblom.cronicas.ui.hud.Separador
import io.github.pauverblom.cronicas.ui.hud.Subtitulo
import io.github.pauverblom.cronicas.ui.hud.Tono
import io.github.pauverblom.cronicas.ui.hud.Variante
import io.github.pauverblom.cronicas.ui.hud.Ventana
import io.github.pauverblom.cronicas.ui.hud.fondoNoche
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos

/** Dinero que se tiene: «0» si no hay nada (el «—» de [Monedas.formato] es para lo que es gratis). */
private fun dinero(mc: Int): String = if (mc == 0) "0" else Monedas.formato(mc)

private val EQUIPABLES = setOf(Categoria.ARMA, Categoria.ARMADURA, Categoria.ESCUDO, Categoria.CASCO, Categoria.EQUIPO)

/** Habilidad de clase que explica cómo se leen los pergaminos (la del Armónico de Hueso). */
private const val HABILIDAD_PERGAMINOS = "Usar pergaminos de magia"

private val Objeto.esPergamino: Boolean
    get() = Categoria.of(categoria) == Categoria.EQUIPO &&
        (conjuroId != null || catalogId == "pergamino" || nombre.contains("pergamino", ignoreCase = true))

@Composable
fun EquipoTab(ui: HojaUi, vm: HojaViewModel) {
    var catalogo by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Objeto?>(null) }
    var compania by remember { mutableStateOf<Compania?>(null) }
    val conjuros by vm.catalogoConjuros.collectAsState()

    ColumnasHoja(
        izquierda = {
            BloqueCarga(ui)
            BloqueMonedas(ui.tesoro, vm::guardarTesoro)
            BloqueCompania(ui, onEditar = { compania = it })
            BloqueMagicos(ui, conjuros, onEditar = { editando = it })
        },
        derecha = {
            BloqueInventario(ui, conjuros, onAnadir = { catalogo = true }, onEditar = { editando = it }, onEquipar = vm::alternarEquipado)
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
        ObjetoDialog(
            o, ui, conjuros,
            onDismiss = { editando = null },
            onGuardar = { vm.guardarObjeto(it); editando = null },
            onEliminar = { vm.eliminarObjeto(o); editando = null },
            onGastar = { vm.gastarPergamino(o); editando = null },
            onCrearConjuro = vm::crearConjuroPropio
        )
    }
    compania?.let { c ->
        CompaniaDialog(c, onDismiss = { compania = null }, onGuardar = { vm.guardarCompania(it); compania = null }, onEliminar = { vm.eliminarCompania(c); compania = null })
    }
}

@Composable
private fun BloqueCarga(ui: HojaUi) {
    val c = ui.hoja.carga
    val banda = when (c.banda) {
        Carga.Banda.LIGERA -> BandaCarga.LIGERA
        Carga.Banda.MEDIA -> BandaCarga.MEDIA
        Carga.Banda.SOBRECARGADO -> BandaCarga.SOBRECARGADO
    }
    Ventana("Carga") {
        // La barra llega hasta 3 × LC, el límite de la carga media; el rombo marca LC
        BarraCarga(
            formatoVe(c.total), "${c.limite}",
            c.total / (c.limite * 3f).coerceAtLeast(1f),
            banda,
            monedas = if (c.veMonedas > 0) "Monedas: ${formatoVe(c.veMonedas.toFloat())} VE (150 monedas = 1 VE)" else null
        )
        Text(
            "Límite de carga = Fuerza${if (ui.hoja.car(Atributo.FUE).primaria) " +3" else ""}" +
                "${if (ui.hoja.car(Atributo.CON).primaria) " +3 (Constitución)" else ""}. Media hasta ${c.limite * 3} VE.",
            style = Tipos.cuerpoSm,
            color = Marca.colores.tintaSuave,
            modifier = Modifier.padding(top = 8.dp)
        )
        Carga.efectos(c.banda)?.let {
            Aviso(it, Modifier.padding(top = 12.dp), titulo = "Carga ${c.banda.etiqueta.lowercase()}", peligro = c.banda == Carga.Banda.SOBRECARGADO)
        }
    }
}

@Composable
private fun BloqueMonedas(t: Tesoro, onGuardar: (Tesoro) -> Unit) {
    var editando by remember { mutableStateOf<Moneda?>(null) }
    var gemas by remember { mutableStateOf(false) }
    val cantidades = mapOf(
        Moneda.PLATINO to t.platino, Moneda.ORO to t.oro, Moneda.ELECTRO to t.electro, Moneda.PLATA to t.plata, Moneda.COBRE to t.cobre
    )
    fun con(m: Moneda, v: Int): Tesoro = when (m) {
        Moneda.PLATINO -> t.copy(platino = v)
        Moneda.ORO -> t.copy(oro = v)
        Moneda.ELECTRO -> t.copy(electro = v)
        Moneda.PLATA -> t.copy(plata = v)
        Moneda.COBRE -> t.copy(cobre = v)
    }
    val c = Marca.colores
    Ventana("Tesoro") {
        Bolsa(
            cantidades,
            onCambio = { m, v -> onGuardar(con(m, v)) },
            onExacta = { editando = it },
            total = dinero(Monedas.Bolsa(t.cobre, t.plata, t.electro, t.oro, t.platino).totalMc)
        )
        Separador()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Subtitulo("Gemas y joyas")
                Text(t.gemas.ifBlank { "—" }, style = Tipos.cuerpo, color = c.tinta)
            }
            BotonIcono({ gemas = true }, "Editar gemas y joyas") { IconoGlifo(Glifo.LAPIZ) }
        }
    }
    editando?.let { m ->
        val actual = cantidades.getValue(m)
        var texto by remember(m) { mutableStateOf(actual.toString()) }
        FormularioDialog(m.nombre, onDismiss = { editando = null }, onGuardar = {
            onGuardar(con(m, texto.toIntOrNull() ?: actual)); editando = null
        }) { CampoNumero(texto, { texto = it }, "Cantidad", sufijo = m.abrev, enfocarAlAbrir = true) }
    }
    if (gemas) {
        var texto by remember { mutableStateOf(t.gemas) }
        FormularioDialog("Gemas y joyas", onDismiss = { gemas = false }, onGuardar = { onGuardar(t.copy(gemas = texto)); gemas = false }) {
            Campo(texto, { texto = it }, "Descripción y valor", unaLinea = false, minLineas = 3)
        }
    }
}

@Composable
private fun BloqueInventario(ui: HojaUi, conjuros: List<Conjuro>, onAnadir: () -> Unit, onEditar: (Objeto) -> Unit, onEquipar: (Objeto) -> Unit) {
    val objetos = ui.objetos
    val companias = ui.companias.associateBy { it.id }
    val nombresConjuros = conjuros.associate { it.id to it.nombre }
    val c = Marca.colores
    Ventana("Inventario", accion = { BotonIcono(onAnadir, "Añadir objeto") { IconoGlifo(Glifo.MAS) } }) {
        if (objetos.isEmpty()) {
            Text("Inventario vacío. Pulsa + para añadir del catálogo o un objeto propio.", style = Tipos.lore, color = c.tintaSuave)
        }
        val sueltos = objetos.filter { it.contenedorId == null }
        val grupos = listOf(
            "Armas" to setOf(Categoria.ARMA),
            "Protección" to setOf(Categoria.ARMADURA, Categoria.ESCUDO, Categoria.CASCO),
            "Munición" to setOf(Categoria.MUNICION),
            "Equipo" to setOf(Categoria.EQUIPO),
            "Contenedores" to setOf(Categoria.CONTENEDOR)
        ).filter { (_, cats) -> sueltos.any { Categoria.of(it.categoria) in cats } }
        grupos.forEachIndexed { g, (titulo, cats) ->
            if (g > 0) Separador()
            Subtitulo(titulo, Modifier.padding(bottom = 2.dp))
            sueltos.filter { Categoria.of(it.categoria) in cats }.forEachIndexed { i, o ->
                if (i > 0) Filete()
                FilaObjeto(o, companias[o.companiaId]?.nombre, nombresConjuros[o.conjuroId], onEditar, onEquipar)
                if (Categoria.of(o.categoria) == Categoria.CONTENEDOR) {
                    val uso = ui.hoja.carga.usoContenedores[o.id] ?: 0f
                    o.capacidad?.let { cap ->
                        Text(
                            "Contenido: ${formatoVe(uso)} / $cap VE",
                            style = Tipos.caption,
                            color = if (uso > cap) c.sangre else c.tintaSuave,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                    objetos.filter { it.contenedorId == o.id }.forEach { dentro ->
                        Row(Modifier.padding(start = 16.dp)) { FilaObjeto(dentro, null, nombresConjuros[dentro.conjuroId], onEditar, onEquipar) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaObjeto(o: Objeto, montura: String?, conjuro: String?, onEditar: (Objeto) -> Unit, onEquipar: (Objeto) -> Unit) {
    val cat = Categoria.of(o.categoria)
    val c = Marca.colores
    Row(Modifier.fillMaxWidth().clickable(onClickLabel = "Editar") { onEditar(o) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                (if (o.cantidad > 1) "${o.cantidad} × " else "") + o.nombre + if (o.bonoMagico != 0) " ${signo(o.bonoMagico)}" else "",
                style = if (o.equipado) Tipos.ui.copy(fontWeight = FontWeight.Bold) else Tipos.ui,
                color = c.tinta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val detalle = listOfNotNull(
                conjuro?.let { "Conjuro: $it" },
                o.formulaDano.takeIf { it.isNotBlank() }?.let { d -> if (o.formulaDanoDosManos.isNotBlank()) "$d / ${o.formulaDanoDosManos}" else d },
                o.ca.takeIf { it != 0 && cat != Categoria.ARMA }?.let { "CA ${signo(it + o.bonoMagico)}" },
                "VE ${formatoVe(if (cat == Categoria.CASCO && o.equipado) 0f else o.ve * o.cantidad)}",
                montura?.let { "En $it" },
                "Mágico".takeIf { o.esMagico }
            ).joinToString(" · ")
            Text(detalle, style = Tipos.caption, color = if (o.esMagico) c.azulArcano else c.tintaSuave)
        }
        if (cat in EQUIPABLES && o.contenedorId == null && o.companiaId == null) {
            val etiqueta = when (cat) {
                Categoria.ARMA -> "En mano"
                Categoria.EQUIPO -> "Usando"
                else -> "Puesto"
            }
            // Oro cuando está en uso, hierro cuando no: la misma placa cambia de metal
            Etiqueta(etiqueta, tono = if (o.equipado) Tono.ORO else Tono.LISO, onClick = { onEquipar(o) })
        }
    }
}

/** Los objetos marcados como mágicos, como el recuadro «Objetos mágicos» de la ficha. */
@Composable
private fun BloqueMagicos(ui: HojaUi, conjuros: List<Conjuro>, onEditar: (Objeto) -> Unit) {
    val magicos = ui.objetos.filter { it.esMagico || it.conjuroId != null }
    val nombresConjuros = conjuros.associate { it.id to it.nombre }
    val c = Marca.colores
    Ventana("Objetos mágicos") {
        if (magicos.isEmpty()) {
            Text("Marca «Objeto mágico» al editar un objeto del inventario para tenerlo a mano aquí.", style = Tipos.lore, color = c.tintaSuave)
        }
        magicos.forEachIndexed { i, o ->
            if (i > 0) Filete()
            Column(Modifier.fillMaxWidth().clickable(onClickLabel = "Editar") { onEditar(o) }.padding(vertical = 6.dp)) {
                Text(
                    (if (o.cantidad > 1) "${o.cantidad} × " else "") + o.nombre + if (o.bonoMagico != 0) " ${signo(o.bonoMagico)}" else "",
                    style = Tipos.ui, color = c.tinta
                )
                val detalle = listOfNotNull(
                    nombresConjuros[o.conjuroId]?.let { "Conjuro: $it" },
                    o.ca.takeIf { it != 0 && Categoria.of(o.categoria) == Categoria.EQUIPO }?.let { "CA ${signo(it)}" },
                    "Puesto".takeIf { o.equipado },
                    o.notas.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (detalle.isNotBlank()) Text(detalle, style = Tipos.caption, color = c.azulArcano)
            }
        }
    }
}

@Composable
private fun BloqueCompania(ui: HojaUi, onEditar: (Compania) -> Unit) {
    val c = Marca.colores
    Ventana("Montura y compañeros", accion = {
        BotonIcono({ onEditar(Compania(personajeId = ui.personaje.id)) }, "Añadir compañero") { IconoGlifo(Glifo.MAS) }
    }) {
        if (ui.companias.isEmpty()) {
            Text("Monturas, compañeros animales y familiares.", style = Tipos.lore, color = c.tintaSuave)
        }
        ui.companias.forEachIndexed { i, comp ->
            if (i > 0) Filete()
            Column(Modifier.fillMaxWidth().clickable(onClickLabel = "Editar") { onEditar(comp) }.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comp.nombre.ifBlank { comp.especie.ifBlank { comp.tipo } }, style = Tipos.ui, color = c.tinta, modifier = Modifier.weight(1f))
                    Etiqueta(comp.tipo.uppercase(), tono = if (comp.tipo == "Familiar") Tono.ARCANO else Tono.ORO, insignia = true)
                }
                Text(
                    listOfNotNull(
                        comp.especie.takeIf { it.isNotBlank() && comp.nombre.isNotBlank() },
                        "PG ${comp.pgActuales}/${comp.pgMaximos}",
                        "CA ${comp.ca}",
                        comp.movimiento.takeIf { it.isNotBlank() }?.let { "Mov. $it" },
                        comp.ataque.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = Tipos.caption,
                    color = c.tintaSuave
                )
                if (comp.capacidadVe > 0) {
                    val carga = ui.hoja.carga.usoCompanias[comp.id] ?: 0f
                    Text(
                        "Carga: ${formatoVe(carga)} / ${comp.capacidadVe} VE",
                        style = Tipos.caption,
                        color = if (carga > comp.capacidadVe) c.sangre else c.tintaSuave
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
    val c = Marca.colores
    var pestana by remember { mutableStateOf(PestanaCatalogo.ARMAS) }
    var busqueda by remember { mutableStateOf("") }
    var elegido by remember { mutableStateOf<ArticuloDef?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.noche,
        dragHandle = null
    ) {
        Column(Modifier.fondoNoche(c).navigationBarsPadding().padding(top = 16.dp)) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Añadir al inventario", style = Tipos.tituloLg, color = c.tinta, modifier = Modifier.weight(1f))
                    Boton("Personalizado", onPersonalizado, variante = Variante.DISCRETO)
                }
                Campo(busqueda, { busqueda = it }, null, marcador = "Buscar")
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(PestanaCatalogo.entries) { p ->
                    val sel = pestana == p && busqueda.isBlank()
                    Etiqueta(p.titulo, tono = if (sel) Tono.ORO else Tono.LISO, onClick = { pestana = p; busqueda = "" })
                }
            }
            val lista = if (busqueda.isBlank()) pestana.lista else Catalogo.todos.filter { it.nombre.contains(busqueda.trim(), ignoreCase = true) }
            if (lista.isEmpty()) {
                Text(
                    "Nada coincide con «${busqueda.trim()}».",
                    style = Tipos.lore,
                    color = c.tintaSuave,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }
            // Un carrusel nuevo por cada categoría o búsqueda, empezando por la primera tarjeta
            key(pestana, busqueda.trim()) {
                if (lista.isNotEmpty()) {
                    val pager = rememberPagerState { lista.size }
                    Carrusel(
                        pager, lista, seleccionada = { false }, onElegir = { elegido = it },
                        clave = { it.id },
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) { a ->
                        Ilustracion(Ilustraciones.articulo(a), a.nombre, 168.dp, archivoFaltante = Ilustraciones.archivoArticulo(a))
                        NombreTarjeta(a.nombre, Monedas.formato(a.precioMc))
                        Text(detalleArticulo(a), style = Tipos.uiSm, color = c.tinta, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        DescripcionTarjeta(a.notas.ifBlank { a.categoria.etiqueta })
                        Boton("Añadir", { elegido = a }, bloque = true, icono = Glifo.MAS)
                    }
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
            Text(detalleArticulo(a), style = Tipos.cuerpoSm, color = c.tintaSuave)
            CampoNumero(lotes, { lotes = it }, if (a.unidades > 1) "Lotes de ${a.unidades}" else "Cantidad")
            Casilla(comprar, { comprar = it }, activa = a.precioMc > 0) {
                Column(Modifier.padding(start = 4.dp)) {
                    Text("Comprar (${Monedas.formato(total)})", style = Tipos.ui, color = c.tinta)
                    Text(
                        "Tienes ${dinero(disponible)}",
                        style = Tipos.caption,
                        color = if (disponible < total && comprar) c.sangre else c.tintaSuave
                    )
                }
            }
        }
    }
}

// ── Diálogos de edición ──

@Composable
private fun ObjetoDialog(
    o: Objeto,
    ui: HojaUi,
    conjuros: List<Conjuro>,
    onDismiss: () -> Unit,
    onGuardar: (Objeto) -> Unit,
    onEliminar: () -> Unit,
    onGastar: () -> Unit,
    onCrearConjuro: (Conjuro, (Long) -> Unit) -> Unit
) {
    var e by remember { mutableStateOf(o) }
    var cantidad by remember { mutableStateOf(o.cantidad.toString()) }
    var ve by remember { mutableStateOf(formatoVe(o.ve)) }
    var ca by remember { mutableStateOf(o.ca.toString()) }
    var bono by remember { mutableStateOf(o.bonoMagico.toString()) }
    var alcance by remember { mutableStateOf(o.alcance?.toString() ?: "") }
    var capacidad by remember { mutableStateOf(o.capacidad?.toString() ?: "") }
    var borrar by remember { mutableStateOf(false) }
    var gastar by remember { mutableStateOf(false) }
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
            { Boton("Eliminar", { borrar = true }, variante = Variante.PELIGRO_DISCRETO) }
        } else null
    ) {
        Campo(e.nombre, { e = e.copy(nombre = it) }, "Nombre")
        Desplegable("Tipo", Categoria.entries, cat, { it.etiqueta }, { e = e.copy(categoria = it.name) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoNumero(cantidad, { cantidad = it }, "Cantidad", Modifier.weight(1f))
            CampoNumero(ve, { ve = it }, "VE por unidad", Modifier.weight(1f), decimales = true, sufijo = "VE")
        }
        Desplegable("Dónde está", lugares, lugarActual, { it.etiqueta }, { l -> e = e.copy(contenedorId = l.contenedorId, companiaId = l.companiaId) })
        if (cat in EQUIPABLES && e.contenedorId == null && e.companiaId == null) {
            Casilla(e.equipado, { e = e.copy(equipado = it) }, etiqueta = if (cat == Categoria.ARMA) "En mano" else "Puesto / en uso")
        }
        if (cat == Categoria.ARMA) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo(e.formulaDano, { e = e.copy(formulaDano = it) }, "Daño", Modifier.weight(1f))
                Campo(e.formulaDanoDosManos, { e = e.copy(formulaDanoDosManos = it) }, "A 2 manos", Modifier.weight(1f))
            }
            Desplegable(
                "Uso", TipoArma.entries, TipoArma.of(e.tipoArma),
                { when (it) { TipoArma.CUERPO -> "Cuerpo a cuerpo"; TipoArma.ARROJADIZA -> "Cuerpo a cuerpo o arrojadiza"; TipoArma.DISTANCIA -> "A distancia" } },
                { e = e.copy(tipoArma = it.name) }
            )
            if (TipoArma.of(e.tipoArma) != TipoArma.CUERPO) CampoNumero(alcance, { alcance = it }, "Alcance base", sufijo = "m")
            Casilla(e.dosManos, { e = e.copy(dosManos = it) }, etiqueta = "Siempre a dos manos")
        }
        if (cat in setOf(Categoria.ARMADURA, Categoria.ESCUDO, Categoria.CASCO, Categoria.EQUIPO)) {
            CampoNumero(
                ca, { ca = it }, if (cat == Categoria.EQUIPO) "Bonificador a la CA (anillo, capa...)" else "CA",
                negativos = true,
                soporte = if (cat == Categoria.EQUIPO) "De varios objetos así solo cuenta el mayor" else null
            )
        }
        if (cat == Categoria.CONTENEDOR) CampoNumero(capacidad, { capacidad = it }, "Capacidad (CAP)", sufijo = "VE")
        if (cat in setOf(Categoria.ARMA, Categoria.ARMADURA, Categoria.ESCUDO, Categoria.CASCO, Categoria.MUNICION)) {
            CampoNumero(bono, { bono = it }, if (cat == Categoria.ARMA) "Bono mágico (ataque y daño)" else "Bono mágico", negativos = true)
        }
        Casilla(e.esMagico, { e = e.copy(esMagico = it) }, etiqueta = "Objeto mágico")
        if (e.esPergamino) {
            SeccionPergamino(e, o.id != 0L, ui, conjuros, onConjuro = { e = e.copy(conjuroId = it) }, onCrearConjuro, onGastar = { gastar = true })
        }
        Campo(e.notas, { e = e.copy(notas = it) }, "Notas", unaLinea = false)
    }
    if (gastar) {
        ConfirmarDialog(
            "Gastar pergamino",
            "El pergamino se consume al leerlo, tanto si sale bien como si se hace polvo." + if (o.cantidad > 1) " Te quedarán ${o.cantidad - 1}." else "",
            confirmar = "Gastar",
            onDismiss = { gastar = false },
            onConfirm = onGastar
        )
    }
    if (borrar) {
        ConfirmarDialog("Eliminar objeto", "¿Eliminar ${o.nombre}?" + if (cat == Categoria.CONTENEDOR) " Su contenido pasará a llevarse encima." else "", onDismiss = { borrar = false }, onConfirm = onEliminar)
    }
}

/** Conjuro que guarda el pergamino, la regla para leerlo y el botón para gastarlo. */
@Composable
private fun SeccionPergamino(
    e: Objeto,
    guardado: Boolean,
    ui: HojaUi,
    conjuros: List<Conjuro>,
    onConjuro: (Long?) -> Unit,
    onCrearConjuro: (Conjuro, (Long) -> Unit) -> Unit,
    onGastar: () -> Unit
) {
    val actual = conjuros.firstOrNull { it.id == e.conjuroId }
    var texto by remember { mutableStateOf("") }
    // Muestra el nombre del conjuro enlazado (también cuando se acaba de crear y llega del catálogo)
    LaunchedEffect(actual?.nombre) { actual?.let { texto = it.nombre } }
    var crear by remember { mutableStateOf(false) }
    val busqueda = texto.trim()
    DesplegableEditable(
        "Conjuro del pergamino", texto,
        conjuros.filter { busqueda.isBlank() || it.nombre.contains(busqueda, ignoreCase = true) }.take(20).map { it.nombre },
        onValor = { v ->
            texto = v
            onConjuro(conjuros.firstOrNull { it.nombre.equals(v.trim(), ignoreCase = true) }?.id)
        }
    )
    if (actual != null) {
        DetallesConjuro(actual)
    } else if (busqueda.isNotBlank()) {
        Boton("Crear «$busqueda» como conjuro del Narrador", { crear = true }, variante = Variante.DISCRETO)
    }
    ui.habilidades.firstOrNull { it.nombre == HABILIDAD_PERGAMINOS }?.let {
        Text(it.descripcion, style = Tipos.cuerpoSm, color = Marca.colores.tintaSuave)
    }
    if (guardado) {
        Boton("Gastar pergamino", onGastar, bloque = true, icono = Glifo.LLAMA)
    }
    if (crear) {
        NuevoConjuroDialog(null, onDismiss = { crear = false }, nombre = busqueda) { c, _ ->
            onCrearConjuro(c, onConjuro)
            crear = false
        }
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
            { Boton("Eliminar", onEliminar, variante = Variante.PELIGRO_DISCRETO) }
        } else null
    ) {
        Desplegable("Tipo", listOf("Montura", "Compañero animal", "Familiar"), e.tipo, { it }, { e = e.copy(tipo = it) })
        Campo(e.nombre, { e = e.copy(nombre = it) }, "Nombre")
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
        Campo(e.movimiento, { e = e.copy(movimiento = it) }, "Movimiento")
        Campo(e.ataque, { e = e.copy(ataque = it) }, "Ataque / daño")
        CampoNumero(capacidad, { capacidad = it }, "Carga que puede llevar", soporte = "0 si no lleva carga", sufijo = "VE")
        Campo(e.habilidades, { e = e.copy(habilidades = it) }, "Habilidades", unaLinea = false, minLineas = 2)
    }
}
