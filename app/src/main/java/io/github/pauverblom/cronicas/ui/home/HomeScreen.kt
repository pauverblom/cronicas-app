package io.github.pauverblom.cronicas.ui.home

import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.BoxWithConstraints
import io.github.pauverblom.cronicas.ui.plataforma.modeloRetrato
import io.github.pauverblom.cronicas.ui.plataforma.rememberAbrirArchivo
import io.github.pauverblom.cronicas.ui.plataforma.rememberAviso
import io.github.pauverblom.cronicas.ui.plataforma.rememberGuardarArchivo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ExpandLess
import androidx.compose.material.icons.sharp.ExpandMore
import androidx.compose.material.icons.sharp.FileOpen
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material.icons.sharp.Sync
import io.github.pauverblom.cronicas.ui.hud.MenuHud
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pauverblom.cronicas.data.Campana
import io.github.pauverblom.cronicas.data.DatosCampanas
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.ResumenPersonaje
import io.github.pauverblom.cronicas.data.sync.Codigos
import io.github.pauverblom.cronicas.rules.data.Clases
import io.github.pauverblom.cronicas.rules.data.Especies
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
import io.github.pauverblom.cronicas.ui.common.FormularioDialog
import io.github.pauverblom.cronicas.ui.common.llamaDe
import io.github.pauverblom.cronicas.ui.hud.Boton
import io.github.pauverblom.cronicas.ui.hud.BotonIcono
import io.github.pauverblom.cronicas.ui.hud.Cabecera
import io.github.pauverblom.cronicas.ui.hud.Campo
import io.github.pauverblom.cronicas.ui.hud.Cargando
import io.github.pauverblom.cronicas.ui.hud.Dialogo
import io.github.pauverblom.cronicas.ui.hud.EntradaLista
import io.github.pauverblom.cronicas.ui.hud.EstadoVacio
import io.github.pauverblom.cronicas.ui.hud.Glifo
import io.github.pauverblom.cronicas.ui.hud.Opcion
import io.github.pauverblom.cronicas.ui.hud.PlacaTitulo
import io.github.pauverblom.cronicas.ui.hud.TituloPantalla
import io.github.pauverblom.cronicas.ui.hud.Variante
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos

@Composable
fun HomeScreen(
    onAbrir: (Long) -> Unit,
    onNuevo: () -> Unit,
    onAjustes: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val personajes by vm.personajes.collectAsState()
    val vinculos by vm.vinculos.collectAsState()
    val campanas by vm.campanas.collectAsState()
    val aviso = rememberAviso()
    var exportando by remember { mutableStateOf<Personaje?>(null) }
    var borrando by remember { mutableStateOf<Personaje?>(null) }
    var sincronizando by remember { mutableStateOf<Personaje?>(null) }
    var vinculando by remember { mutableStateOf(false) }
    var moviendo by remember { mutableStateOf<Personaje?>(null) }
    var renombrando by remember { mutableStateOf<Campana?>(null) }
    var disolviendo by remember { mutableStateOf<Campana?>(null) }

    val exportar = rememberGuardarArchivo("application/json") { archivo ->
        val p = exportando ?: return@rememberGuardarArchivo
        exportando = null
        vm.exportar(p.id, archivo) { ok -> aviso(if (ok) "Personaje exportado" else "No se pudo exportar") }
    }
    val importar = rememberAbrirArchivo(arrayOf("application/json", "application/octet-stream", "*/*")) { archivo ->
        vm.importar(archivo) { id ->
            if (id != null) onAbrir(id) else aviso("El archivo no es un personaje válido", largo = true)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Cabecera(
            acciones = {
                BotonIcono(importar, "Importar personaje") { Icon(Icons.Sharp.FileOpen, null, Modifier.size(24.dp)) }
                BotonIcono(onAjustes, "Ajustes") { Icon(Icons.Sharp.Settings, null, Modifier.size(24.dp)) }
            }
        ) { TituloPantalla("Crónicas de la Marca") }
        val lista = personajes
        val datos = campanas
        when {
            lista == null || datos == null -> Cargando()
            lista.isEmpty() -> SinPersonajes(onNuevo, importar, if (vm.syncDisponible) ({ vinculando = true }) else null)
            else -> {
                // Crear un personaje o traer uno que otro dispositivo comparte con su código. Uno al lado del otro;
                // en pantallas muy estrechas, uno debajo del otro para que no se corte el texto
                BoxWithConstraints(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).widthIn(max = 560.dp).align(Alignment.CenterHorizontally)) {
                    val nuevo = @Composable { m: Modifier, margen: Dp ->
                        Boton("Nuevo personaje", onNuevo, m, variante = Variante.DORADO, icono = Glifo.MAS, bloque = true, margen = margen)
                    }
                    val vincular = @Composable { m: Modifier, margen: Dp ->
                        Boton("Vincular", { vinculando = true }, m, descripcion = "Vincular un personaje con su código", margen = margen)
                    }
                    when {
                        !vm.syncDisponible -> nuevo(Modifier, 36.dp)
                        maxWidth >= 368.dp -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            nuevo(Modifier.weight(1f), 24.dp)
                            vincular(Modifier, 24.dp)
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            nuevo(Modifier, 36.dp)
                            vincular(Modifier.fillMaxWidth(), 36.dp)
                        }
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(340.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val grupos = agrupar(lista, datos)
                    val conCabeceras = grupos.any { it.campana != null }
                    for (g in grupos) {
                        val plegada = g.campana != null && g.campana.id in datos.plegadas
                        if (conCabeceras) {
                            item(key = "campana_${g.campana?.id ?: 0}", span = { GridItemSpan(maxLineSpan) }) {
                                CabeceraCampana(
                                    titulo = g.campana?.nombre ?: "Sin campaña",
                                    cuantos = g.personajes.size,
                                    plegada = plegada,
                                    onPlegar = g.campana?.let { c -> { vm.plegarCampana(c.id, !plegada) } },
                                    onRenombrar = g.campana?.let { c -> { renombrando = c } },
                                    onDisolver = g.campana?.let { c -> { disolviendo = c } }
                                )
                            }
                        }
                        if (!plegada) items(g.personajes, key = { it.personaje.id }) { r ->
                            FilaPersonaje(
                                r,
                                onClick = { onAbrir(r.personaje.id) },
                                onExportar = {
                                    exportando = r.personaje
                                    exportar("${r.personaje.nombre.ifBlank { "personaje" }}.cronicas.json")
                                },
                                onEliminar = { borrando = r.personaje },
                                onSincronizar = if (vm.syncDisponible) ({ sincronizando = r.personaje }) else null,
                                onMover = { moviendo = r.personaje }
                            )
                        }
                    }
                    item { Box(Modifier.navigationBarsPadding()) }
                }
            }
        }
    }

    borrando?.let { p ->
        ConfirmarDialog(
            titulo = "Eliminar personaje",
            texto = if (p.id in vinculos) {
                "¿Eliminar a ${p.nombre} de este dispositivo? Se borrará su ficha aquí y dejará de sincronizarse. " +
                    "En los demás dispositivos vinculados seguirá estando."
            } else {
                "¿Eliminar a ${p.nombre}? Se borrará toda su ficha. Esta acción no se puede deshacer."
            },
            onDismiss = { borrando = null },
            onConfirm = { vm.eliminar(p) }
        )
    }

    moviendo?.let { p ->
        val actuales = personajes.orEmpty()
        val datos = campanas ?: DatosCampanas()
        MoverCampanaDialog(
            personaje = p,
            actual = datos.campanaDe(p.id),
            campanas = agrupar(actuales, datos).mapNotNull { it.campana },
            onElegir = { vm.moverACampana(p.id, it) },
            onCrear = { vm.crearCampana(p.id, it) },
            onDismiss = { moviendo = null }
        )
    }

    renombrando?.let { c ->
        RenombrarCampanaDialog(c, onGuardar = { vm.renombrarCampana(c.id, it) }, onDismiss = { renombrando = null })
    }

    disolviendo?.let { c ->
        ConfirmarDialog(
            titulo = "Disolver campaña",
            texto = "Los personajes de «${c.nombre}» quedarán sin campaña. No se borra ningún personaje.",
            confirmar = "Disolver",
            onDismiss = { disolviendo = null },
            onConfirm = { vm.disolverCampana(c.id) }
        )
    }

    sincronizando?.let { p ->
        SincronizarDialog(
            personaje = p,
            codigo = vinculos[p.id],
            onActivar = { onFin ->
                vm.activarSync(p.id) { codigo ->
                    onFin()
                    if (codigo == null) {
                        aviso("No se pudo activar. ¿Hay conexión a internet?", largo = true)
                    }
                }
            },
            onDesvincular = { vm.desvincular(p.id) },
            onDismiss = { sincronizando = null }
        )
    }

    if (vinculando) {
        VincularDialog(
            onVincular = { codigo, onError ->
                vm.vincular(codigo) { r ->
                    r.onSuccess { id ->
                        vinculando = false
                        onAbrir(id)
                    }.onFailure { e ->
                        onError(
                            when (e) {
                                is IllegalArgumentException, is IllegalStateException -> e.message ?: "No se pudo vincular"
                                else -> "No se pudo vincular. ¿Hay conexión a internet?"
                            }
                        )
                    }
                }
            },
            onDismiss = { vinculando = false }
        )
    }
}

/** Personajes de una campaña, o los que no tienen ([campana] null). */
private data class Grupo(val campana: Campana?, val personajes: List<ResumenPersonaje>)

/** Una sección por campaña con personajes, en orden de creación, y al final los que no tienen campaña. */
private fun agrupar(lista: List<ResumenPersonaje>, datos: DatosCampanas): List<Grupo> {
    val porCampana = lista.groupBy { datos.campanaDe(it.personaje.id) }
    val conCampana = datos.campanas.mapNotNull { c -> porCampana[c]?.let { Grupo(c, it) } }
    val sinCampana = porCampana[null].orEmpty()
    return if (conCampana.isEmpty()) listOf(Grupo(null, lista))
    else conCampana + listOfNotNull(sinCampana.takeIf { it.isNotEmpty() }?.let { Grupo(null, it) })
}

/** Cabecera de sección: nombre y número de personajes; se pliega al tocarla. «Sin campaña» no se pliega ni tiene menú. */
@Composable
private fun CabeceraCampana(
    titulo: String,
    cuantos: Int,
    plegada: Boolean,
    onPlegar: (() -> Unit)?,
    onRenombrar: (() -> Unit)?,
    onDisolver: (() -> Unit)?
) {
    val c = Marca.colores
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onPlegar != null) Modifier.clickable(onClick = onPlegar) else Modifier)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // El título se lleva todo el espacio libre que necesite; los botones quedan a la derecha
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlacaTitulo(titulo, Modifier.weight(1f, fill = false))
            Text("$cuantos", style = Tipos.uiSm, color = c.tintaSuave)
        }
        if (onPlegar != null) {
            Icon(
                if (plegada) Icons.Sharp.ExpandMore else Icons.Sharp.ExpandLess,
                contentDescription = if (plegada) "Desplegar" else "Plegar",
                tint = c.oro
            )
        }
        if (onRenombrar != null && onDisolver != null) {
            Box {
                BotonIcono({ menu = true }, "Opciones de la campaña") { Icon(Icons.Sharp.MoreVert, null, Modifier.size(22.dp)) }
                MenuHud(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Renombrar", style = Tipos.ui, color = c.tinta) }, onClick = { menu = false; onRenombrar() })
                    DropdownMenuItem(text = { Text("Disolver", style = Tipos.ui, color = c.sangre) }, onClick = { menu = false; onDisolver() })
                }
            }
        }
    }
}

/** Elige la campaña de un personaje, o crea una nueva para él. */
@Composable
private fun MoverCampanaDialog(
    personaje: Personaje,
    actual: Campana?,
    campanas: List<Campana>,
    onElegir: (Long?) -> Unit,
    onCrear: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nueva by remember { mutableStateOf("") }
    FormularioDialog(
        titulo = "Campaña de ${personaje.nombre}",
        onDismiss = onDismiss,
        guardar = "Crear",
        guardarActivo = nueva.isNotBlank(),
        onGuardar = { onCrear(nueva); onDismiss() }
    ) {
        for (c in campanas) {
            Opcion(actual?.id == c.id, { onElegir(c.id); onDismiss() }) { Text(c.nombre, style = Tipos.ui) }
        }
        Opcion(actual == null, { onElegir(null); onDismiss() }) { Text("Sin campaña", style = Tipos.ui) }
        Campo(nueva, { nueva = it }, "Nueva campaña", marcador = "Nombre de la campaña")
    }
}

@Composable
private fun RenombrarCampanaDialog(campana: Campana, onGuardar: (String) -> Unit, onDismiss: () -> Unit) {
    var nombre by remember { mutableStateOf(campana.nombre) }
    FormularioDialog(
        titulo = "Renombrar campaña",
        onDismiss = onDismiss,
        guardarActivo = nombre.isNotBlank(),
        onGuardar = { onGuardar(nombre); onDismiss() }
    ) {
        Campo(nombre, { nombre = it }, "Nombre", enfocarAlAbrir = true)
    }
}

/** Activa la sincronización de un personaje o muestra su código para vincularlo en otro dispositivo. */
@Composable
private fun SincronizarDialog(
    personaje: Personaje,
    codigo: String?,
    onActivar: (onFin: () -> Unit) -> Unit,
    onDesvincular: () -> Unit,
    onDismiss: () -> Unit
) {
    var activando by remember { mutableStateOf(false) }
    val portapapeles = LocalClipboardManager.current
    val aviso = rememberAviso()
    val c = Marca.colores
    if (codigo == null) {
        Dialogo(
            "Sincronizar",
            onDismiss = onDismiss,
            acciones = {
                Boton("Cancelar", onDismiss, variante = Variante.DISCRETO)
                Boton(if (activando) "Activando…" else "Activar", {
                    activando = true
                    onActivar { activando = false }
                }, variante = Variante.DORADO, activo = !activando)
            }
        ) {
            Text("Sincroniza la ficha de ${personaje.nombre} con tus otros dispositivos: lo que cambies en uno aparecerá en los demás.")
            Text("Al activarla tendrás un código para vincular el personaje en el otro dispositivo.", style = Tipos.uiSm, color = c.tintaSuave)
        }
    } else {
        val visible = Codigos.formatear(codigo)
        Dialogo(
            "Sincronizado",
            onDismiss = onDismiss,
            extra = {
                Boton("Dejar de sincronizar", { onDesvincular(); onDismiss() }, variante = Variante.PELIGRO_DISCRETO)
            },
            acciones = {
                Boton("Copiar", {
                    portapapeles.setText(AnnotatedString(visible))
                    aviso("Código copiado")
                })
                Boton("Cerrar", onDismiss, variante = Variante.DORADO)
            }
        ) {
            Text("Para tener a ${personaje.nombre} en otro dispositivo, pulsa allí «Vincular personaje» y escribe este código:")
            Text(visible, style = Tipos.tituloLg, color = c.oro, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally))
            Text("Comparte el código solo con quien quieras que pueda ver y cambiar la ficha.", style = Tipos.uiSm, color = c.tintaSuave)
        }
    }
}

/** Pide un código de sincronización y descarga ese personaje. */
@Composable
private fun VincularDialog(onVincular: (codigo: String, onError: (String) -> Unit) -> Unit, onDismiss: () -> Unit) {
    var codigo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var vinculando by remember { mutableStateOf(false) }
    val completo = Codigos.normalizar(codigo).length == Codigos.LARGO
    FormularioDialog(
        titulo = "Vincular personaje",
        onDismiss = onDismiss,
        guardar = if (vinculando) "Vinculando…" else "Vincular",
        guardarActivo = completo && !vinculando,
        onGuardar = {
            vinculando = true
            error = null
            onVincular(codigo) { mensaje ->
                vinculando = false
                error = mensaje
            }
        }
    ) {
        Text("Escribe el código que aparece en el otro dispositivo, en «Sincronizar» del personaje.")
        Campo(
            codigo,
            { codigo = it; error = null },
            "Código",
            marcador = "XXXX-XXXX-XXXX",
            error = error,
            enfocarAlAbrir = true
        )
    }
}

@Composable
private fun SinPersonajes(onNuevo: () -> Unit, onImportar: () -> Unit, onVincular: (() -> Unit)?) {
    Box(Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        EstadoVacio(
            "Bienvenido a la Marca del Este",
            "Crea tu primer aventurero para empezar su ficha."
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Boton("Crear personaje", onNuevo, variante = Variante.DORADO)
                Boton("Importar personaje", onImportar)
                if (onVincular != null) Boton("Vincular personaje", onVincular)
            }
        }
    }
}

@Composable
private fun FilaPersonaje(
    r: ResumenPersonaje,
    onClick: () -> Unit,
    onExportar: () -> Unit,
    onEliminar: () -> Unit,
    onSincronizar: (() -> Unit)?,
    onMover: () -> Unit
) {
    val p = r.personaje
    var menu by remember { mutableStateOf(false) }
    val c = Marca.colores
    val clase = Clases.porId(p.claseId)?.nombre ?: p.claseNombre.ifBlank { null }
    val especie = Especies.porId(p.especieId)?.nombre ?: p.especieNombre
    // Igual que en la cabecera de la ficha: «Druida · Semiorco · NV 1»
    val linea = listOfNotNull(clase ?: "Sin clase", especie.ifBlank { null }, "NV ${p.nivel}").joinToString(" · ")
    Box {
        EntradaLista(
            nombre = p.nombre,
            retrato = remember(p.fotoPath) { modeloRetrato(p.fotoPath) },
            linea = linea,
            pg = r.estado?.pgActuales,
            pgMax = r.estado?.pgMaximos,
            llama = llamaDe(p.claseId),
            onClick = onClick,
            onClickLargo = { menu = true },
            onMenu = { menu = true }
        )
        // El menú sale junto a los tres puntos, en la esquina de la tarjeta
        Box(Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp)) {
            MenuHud(expanded = menu, onDismissRequest = { menu = false }) {
                if (onSincronizar != null) {
                    DropdownMenuItem(text = { Text("Sincronizar", style = Tipos.ui, color = c.tinta) }, onClick = { menu = false; onSincronizar() })
                }
                DropdownMenuItem(text = { Text("Mover a campaña…", style = Tipos.ui, color = c.tinta) }, onClick = { menu = false; onMover() })
                DropdownMenuItem(text = { Text("Exportar", style = Tipos.ui, color = c.tinta) }, onClick = { menu = false; onExportar() })
                DropdownMenuItem(text = { Text("Eliminar", style = Tipos.ui, color = c.sangre) }, onClick = { menu = false; onEliminar() })
            }
        }
    }
}
