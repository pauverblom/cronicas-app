package io.github.pauverblom.cronicas.ui.hoja

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import io.github.pauverblom.cronicas.ui.hud.MenuHud
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.github.pauverblom.cronicas.data.Habilidad
import io.github.pauverblom.cronicas.data.PERSONALIZADA
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.rules.Atributo
import io.github.pauverblom.cronicas.rules.CaracteristicaHoja
import io.github.pauverblom.cronicas.rules.Modifiers
import io.github.pauverblom.cronicas.rules.Progresion
import io.github.pauverblom.cronicas.rules.data.Alineamientos
import io.github.pauverblom.cronicas.rules.data.Clases
import io.github.pauverblom.cronicas.rules.data.Especies
import io.github.pauverblom.cronicas.ui.common.CampoNumero
import io.github.pauverblom.cronicas.ui.common.Desplegable
import io.github.pauverblom.cronicas.ui.common.DesplegableEditable
import io.github.pauverblom.cronicas.ui.common.FormularioDialog
import io.github.pauverblom.cronicas.ui.common.signo
import io.github.pauverblom.cronicas.ui.foto.RetratoAvatar
import io.github.pauverblom.cronicas.ui.foto.rememberSelectorRetrato
import io.github.pauverblom.cronicas.ui.hud.BarraPx
import io.github.pauverblom.cronicas.ui.hud.Boton
import io.github.pauverblom.cronicas.ui.hud.BotonIcono
import io.github.pauverblom.cronicas.ui.hud.Campo
import io.github.pauverblom.cronicas.ui.hud.Casilla
import io.github.pauverblom.cronicas.ui.hud.EscudoCaracteristica
import io.github.pauverblom.cronicas.ui.hud.Filete
import io.github.pauverblom.cronicas.ui.hud.Glifo
import io.github.pauverblom.cronicas.ui.hud.IconoGlifo
import io.github.pauverblom.cronicas.ui.hud.Separador
import io.github.pauverblom.cronicas.ui.hud.Subtitulo
import io.github.pauverblom.cronicas.ui.hud.Variante
import io.github.pauverblom.cronicas.ui.hud.Ventana
import io.github.pauverblom.cronicas.ui.hud.miles
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos

@Composable
fun PersonajeTab(ui: HojaUi, vm: HojaViewModel) {
    var editarIdentidad by remember { mutableStateOf(false) }
    var editarBio by remember { mutableStateOf(false) }
    var caract by remember { mutableStateOf<CaracteristicaHoja?>(null) }
    var subir by remember { mutableStateOf(false) }
    var anadirXp by remember { mutableStateOf(false) }
    var habilidad by remember { mutableStateOf<Habilidad?>(null) }

    ColumnasHoja(
        izquierda = {
            BloqueIdentidad(ui, vm, onEditar = { editarIdentidad = true })
            BloqueExperiencia(ui, onAnadir = { anadirXp = true }, onSubir = { subir = true })
            BloqueCaracteristicas(ui, onClick = { caract = it })
        },
        derecha = {
            BloqueHabilidades(ui.habilidades, onEditar = { habilidad = it })
            BloqueBio(ui.personaje, onEditar = { editarBio = true })
        }
    )

    if (editarIdentidad) IdentidadDialog(ui.personaje, onDismiss = { editarIdentidad = false }) { p -> vm.actualizarPersonaje { p }; editarIdentidad = false }
    if (editarBio) BioDialog(ui.personaje, onDismiss = { editarBio = false }) { p -> vm.actualizarPersonaje { p }; editarBio = false }
    caract?.let { c -> CaracteristicaDialog(c, ui, vm, onDismiss = { caract = null }) }
    if (subir) SubirNivelDialog(ui, onDismiss = { subir = false }) { nivel, pg -> vm.subirNivel(nivel, pg); subir = false }
    if (anadirXp) {
        var texto by remember { mutableStateOf("") }
        FormularioDialog("Añadir experiencia", onDismiss = { anadirXp = false }, onGuardar = {
            val n = texto.toIntOrNull() ?: 0
            vm.actualizarPersonaje { it.copy(experiencia = (it.experiencia + n).coerceAtLeast(0)) }
            anadirXp = false
        }, guardar = "Añadir") {
            Text("Total actual: ${miles(ui.personaje.experiencia)} PX")
            CampoNumero(texto, { texto = it }, "PX ganados", negativos = true, soporte = "Usa un número negativo para restar", enfocarAlAbrir = true)
        }
    }
    habilidad?.let { h -> HabilidadDialog(h, onDismiss = { habilidad = null }, onGuardar = { vm.guardarHabilidad(it); habilidad = null }, onEliminar = { vm.eliminarHabilidad(h); habilidad = null }) }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    if (valor.isBlank()) return
    val c = Marca.colores
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(etiqueta, style = Tipos.uiSm, color = c.tintaSuave, modifier = Modifier.width(110.dp))
        Text(valor, style = Tipos.ui, color = c.tinta, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BloqueIdentidad(ui: HojaUi, vm: HojaViewModel, onEditar: () -> Unit) {
    val p = ui.personaje
    val h = ui.hoja
    val c = Marca.colores
    var menuRetrato by remember { mutableStateOf(false) }
    val selector = rememberSelectorRetrato(vm::setRetrato)
    Ventana("Personaje", accion = { BotonIcono(onEditar, "Editar personaje") { IconoGlifo(Glifo.LAPIZ) } }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                RetratoAvatar(
                    p.fotoPath, p.nombre, 128.dp,
                    Modifier.clickable(onClickLabel = "Cambiar retrato") { menuRetrato = true },
                    claseId = p.claseId, anillo = true
                )
                MenuHud(expanded = menuRetrato, onDismissRequest = { menuRetrato = false }) {
                    DropdownMenuItem(text = { Text("Elegir de la galería", style = Tipos.ui, color = c.tinta) }, onClick = { menuRetrato = false; selector(false) })
                    DropdownMenuItem(text = { Text("Hacer una foto", style = Tipos.ui, color = c.tinta) }, onClick = { menuRetrato = false; selector(true) })
                    if (p.fotoPath != null) DropdownMenuItem(text = { Text("Quitar retrato", style = Tipos.ui, color = c.sangre) }, onClick = { menuRetrato = false; vm.quitarRetrato() })
                }
            }
            Text(p.nombre, style = Tipos.tituloLg, color = c.tinta, modifier = Modifier.padding(top = 16.dp))
            Text(
                h.nombreClase + (h.secundaria?.let { " / ${it.nombre} ${h.nivelSecundario}" } ?: "") + " · Nivel ${h.nivel}",
                style = Tipos.ui, color = c.tinta
            )
            Text(h.nombreEspecie, style = Tipos.uiSm, color = c.tintaSuave)
        }
        Separador()
        FilaDato("Alineamiento", p.alineamiento)
        FilaDato("Deidad", p.deidad)
        FilaDato("Jugador", p.jugador)
        FilaDato("Origen", p.lugarOrigen)
        FilaDato("Idiomas", p.idiomas)
        FilaDato("Dado de golpe", h.dado)
    }
}

@Composable
private fun BloqueExperiencia(ui: HojaUi, onAnadir: () -> Unit, onSubir: () -> Unit) {
    val p = ui.personaje
    val h = ui.hoja
    Ventana("Experiencia") {
        val desde = h.clase?.let { Progresion.xpParaNivel(it, h.secundaria, h.nivel) } ?: 0
        BarraPx(p.experiencia, desde, h.xpSiguiente, h.nivel)
        Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Cuando hay PX suficientes, «Subir de nivel» es la llamada a la acción y late
            Boton("Subir de nivel", onSubir, variante = if (h.puedeSubir) Variante.DORADO else Variante.FANTASMA, brillo = h.puedeSubir, bloque = true)
            Boton("PX", onAnadir, icono = Glifo.MAS, descripcion = "Añadir PX", bloque = true)
        }
    }
}

@Composable
private fun BloqueCaracteristicas(ui: HojaUi, onClick: (CaracteristicaHoja) -> Unit) {
    val c = Marca.colores
    Ventana("Características") {
        // 3 × 2 en móvil, 6 × 1 cuando cabe; siempre FUE, DES, CON, INT, SAB, CAR
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val porFila = if (maxWidth >= 560.dp) 6 else 3
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.hoja.caracteristicas.chunked(porFila).forEach { fila ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                        fila.forEach { car ->
                            EscudoCaracteristica(
                                car.atributo, car.puntuacion, car.base, car.mod, car.primaria, car.modForzado,
                                Modifier.weight(1f), onClick = { onClick(car) }
                            )
                        }
                    }
                }
            }
        }
        val extra = ui.hoja.especie?.mods?.takeIf { it.isNotEmpty() }
        if (extra != null) {
            Text(
                "Incluye los modificadores de ${ui.hoja.nombreEspecie.lowercase()}: " + extra.entries.joinToString { "${it.key.nombre} ${signo(it.value)}" },
                style = Tipos.cuerpoSm,
                color = c.tintaSuave,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun BloqueHabilidades(habilidades: List<Habilidad>, onEditar: (Habilidad) -> Unit) {
    val c = Marca.colores
    Ventana("Habilidades y rasgos", accion = {
        BotonIcono({ onEditar(Habilidad(personajeId = 0, nombre = "")) }, "Añadir habilidad") { IconoGlifo(Glifo.MAS) }
    }) {
        val grupos = listOf(
            Habilidad.ORIGEN_CLASE to "De clase",
            Habilidad.ORIGEN_ESPECIE to "De especie",
            Habilidad.ORIGEN_PROPIA to "Otras"
        )
        if (habilidades.isEmpty()) Text("Sin habilidades", style = Tipos.lore, color = c.tintaSuave)
        grupos.filter { (origen, _) -> habilidades.any { it.origen == origen } }.forEachIndexed { g, (origen, titulo) ->
            if (g > 0) Separador()
            Subtitulo(titulo, Modifier.padding(bottom = 4.dp))
            habilidades.filter { it.origen == origen }.forEachIndexed { i, h ->
                if (i > 0) Filete()
                var abierta by remember(h.id) { mutableStateOf(false) }
                Column(Modifier.fillMaxWidth().clickable(onClickLabel = if (abierta) "Plegar" else "Desplegar") { abierta = !abierta }.padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(h.nombre, style = Tipos.ui, color = c.tinta, modifier = Modifier.weight(1f).padding(end = 8.dp))
                        h.atributo?.let { abrev ->
                            val a = Atributo.fromAbrev(abrev)
                            Text(a?.nombre?.uppercase() ?: abrev, style = Tipos.etiquetaCaps.copy(letterSpacing = 0.08.em), color = a?.let { colorAtributo(it) } ?: c.tintaSuave)
                        }
                        BotonIcono({ onEditar(h) }, "Editar ${h.nombre}") { IconoGlifo(Glifo.LAPIZ, tamano = 16.dp) }
                    }
                    if (h.descripcion.isNotBlank()) {
                        Text(
                            h.descripcion,
                            style = Tipos.cuerpoSm,
                            color = c.tintaSuave,
                            maxLines = if (abierta) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BloqueBio(p: Personaje, onEditar: () -> Unit) {
    val c = Marca.colores
    Ventana("Descripción", accion = { BotonIcono(onEditar, "Editar descripción") { IconoGlifo(Glifo.LAPIZ) } }) {
        val apariencia = listOf("Edad" to p.edad, "Género" to p.genero, "Altura" to p.altura, "Peso" to p.peso, "Ojos" to p.ojos, "Cabello" to p.cabello)
        apariencia.forEach { (k, v) -> FilaDato(k, v) }
        listOf("Descripción" to p.descripcion, "Personalidad" to p.personalidad, "Trasfondo" to p.trasfondo, "Cicatrices" to p.cicatrices).forEach { (k, v) ->
            if (v.isNotBlank()) {
                Subtitulo(k, Modifier.padding(top = 12.dp, bottom = 2.dp))
                Text(v, style = Tipos.cuerpo, color = c.tinta)
            }
        }
        if (apariencia.all { it.second.isBlank() } && p.descripcion.isBlank() && p.personalidad.isBlank() && p.trasfondo.isBlank() && p.cicatrices.isBlank()) {
            Text("Pulsa el lápiz para describir a tu personaje.", style = Tipos.lore, color = c.tintaSuave)
        }
    }
}

@Composable
private fun IdentidadDialog(p: Personaje, onDismiss: () -> Unit, onGuardar: (Personaje) -> Unit) {
    var e by remember { mutableStateOf(p) }
    var nivelTxt by remember { mutableStateOf(p.nivel.toString()) }
    data class Opcion(val id: String, val nombre: String)
    val clases = Clases.todas.map { Opcion(it.id, it.nombre) } + Opcion(PERSONALIZADA, "Personalizada")
    val especies = Especies.todas.map { Opcion(it.id, it.nombre) } + Opcion(PERSONALIZADA, "Personalizada")
    FormularioDialog(
        "Editar personaje",
        onDismiss = onDismiss,
        onGuardar = { onGuardar(e.copy(nombre = e.nombre.trim(), nivel = (nivelTxt.toIntOrNull() ?: e.nivel).coerceIn(1, Progresion.NIVEL_MAXIMO))) },
        guardarActivo = e.nombre.isNotBlank()
    ) {
        Campo(e.nombre, { e = e.copy(nombre = it) }, "Nombre")
        Desplegable("Clase", clases, clases.firstOrNull { it.id == e.claseId }, { it.nombre }, { e = e.copy(claseId = it.id) })
        if (e.claseId == PERSONALIZADA) {
            Campo(e.claseNombre, { e = e.copy(claseNombre = it) }, "Nombre de la clase")
        } else {
            Casilla(e.claseSecundariaId != null, { on ->
                e = e.copy(claseSecundariaId = if (on) Clases.todas.first { it.id != e.claseId }.id else null)
            }, etiqueta = "Multiclase")
            if (e.claseSecundariaId != null) {
                val sec = clases.filter { it.id != e.claseId && it.id != PERSONALIZADA }
                Desplegable("Clase secundaria", sec, sec.firstOrNull { it.id == e.claseSecundariaId }, { it.nombre }, { e = e.copy(claseSecundariaId = it.id) })
            }
        }
        Desplegable("Especie", especies, especies.firstOrNull { it.id == e.especieId }, { it.nombre }, { e = e.copy(especieId = it.id) })
        if (e.especieId == PERSONALIZADA) {
            Campo(e.especieNombre, { e = e.copy(especieNombre = it) }, "Nombre de la especie")
        }
        CampoNumero(nivelTxt, { nivelTxt = it }, "Nivel", soporte = "Cambiarlo aquí no suma PG; usa «Subir de nivel»")
        Desplegable(
            "Alineamiento", Alineamientos.todos, e.alineamiento.ifBlank { null }, { it }, { e = e.copy(alineamiento = it) },
            soporte = Alineamientos.aviso(Clases.porId(e.claseId), e.alineamiento)
        )
        DesplegableEditable("Deidad", e.deidad, Alineamientos.deidades, { e = e.copy(deidad = it) })
        Campo(e.idiomas, { e = e.copy(idiomas = it) }, "Idiomas", unaLinea = false)
        Campo(e.jugador, { e = e.copy(jugador = it) }, "Jugador")
        Campo(e.lugarOrigen, { e = e.copy(lugarOrigen = it) }, "Lugar de origen")
    }
}

@Composable
private fun BioDialog(p: Personaje, onDismiss: () -> Unit, onGuardar: (Personaje) -> Unit) {
    var e by remember { mutableStateOf(p) }
    FormularioDialog("Descripción", onDismiss = onDismiss, onGuardar = { onGuardar(e) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo(e.edad, { e = e.copy(edad = it) }, "Edad", Modifier.weight(1f))
            Campo(e.genero, { e = e.copy(genero = it) }, "Género", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo(e.altura, { e = e.copy(altura = it) }, "Altura", Modifier.weight(1f))
            Campo(e.peso, { e = e.copy(peso = it) }, "Peso", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo(e.ojos, { e = e.copy(ojos = it) }, "Ojos", Modifier.weight(1f))
            Campo(e.cabello, { e = e.copy(cabello = it) }, "Cabello", Modifier.weight(1f))
        }
        Campo(e.descripcion, { e = e.copy(descripcion = it) }, "Descripción", unaLinea = false, minLineas = 2)
        Campo(e.personalidad, { e = e.copy(personalidad = it) }, "Personalidad", unaLinea = false, minLineas = 2)
        Campo(e.trasfondo, { e = e.copy(trasfondo = it) }, "Trasfondo", unaLinea = false, minLineas = 2)
        Campo(e.cicatrices, { e = e.copy(cicatrices = it) }, "Cicatrices", unaLinea = false)
    }
}

@Composable
private fun CaracteristicaDialog(c: CaracteristicaHoja, ui: HojaUi, vm: HojaViewModel, onDismiss: () -> Unit) {
    var base by remember { mutableStateOf(c.base.toString()) }
    var primaria by remember { mutableStateOf(c.primaria) }
    var mod by remember { mutableStateOf(if (c.modForzado) c.mod.toString() else "") }
    val modEsp = ui.hoja.especie?.mods?.get(c.atributo) ?: 0
    val ajustada = (base.toIntOrNull() ?: c.base) + modEsp
    FormularioDialog(c.atributo.nombre, onDismiss = onDismiss, onGuardar = {
        vm.actualizarCaracteristicas {
            it.conBase(c.atributo, (base.toIntOrNull() ?: c.base).coerceIn(1, 25))
                .conPrimaria(c.atributo, primaria)
                .conOverrideMod(c.atributo, mod.toIntOrNull())
        }
        onDismiss()
    }) {
        CampoNumero(base, { base = it }, "Puntuación tirada", soporte = if (modEsp != 0) "Con especie: $ajustada (${signo(modEsp)})" else null)
        Casilla(primaria, { primaria = it }, etiqueta = "Primaria (ND 12 en vez de 18)")
        if (ui.hoja.clase?.principal == c.atributo && !primaria) {
            Text("Es la característica principal de tu clase.", style = Tipos.caption, color = Marca.colores.sangre)
        }
        CampoNumero(mod, { mod = it }, "Modificador fijado a mano", negativos = true, soporte = "Vacío = según la tabla (${signo(Modifiers.modifier(ajustada))})")
    }
}

@Composable
private fun SubirNivelDialog(ui: HojaUi, onDismiss: () -> Unit, onConfirmar: (nivel: Int, pg: Int) -> Unit) {
    val h = ui.hoja
    val c = Marca.colores
    val nuevo = h.nivel + 1
    var tirada by remember { mutableStateOf("") }
    val clase = h.clase
    val (dado, fijos) = clase?.let { Progresion.dado(it, h.secundaria) } ?: (0 to 0)
    val modCon = h.mod(Atributo.CON)
    val pg = when {
        clase == null -> tirada.toIntOrNull() ?: 0
        else -> Progresion.pgAlSubir(nuevo, tirada.toIntOrNull() ?: 0, modCon, fijos)
    }
    val necesitaTirada = clase == null || nuevo <= 10
    FormularioDialog(
        "Subir a nivel $nuevo",
        onDismiss = onDismiss,
        onGuardar = { onConfirmar(nuevo, pg) },
        guardar = "Subir",
        guardarActivo = nuevo <= Progresion.NIVEL_MAXIMO &&
            (!necesitaTirada || tirada.toIntOrNull()?.let { clase == null || it in 1..dado } == true)
    ) {
        when {
            clase == null -> {
                Text("Introduce los PG que ganas en este nivel.")
                CampoNumero(tirada, { tirada = it }, "PG ganados")
            }
            nuevo <= 10 -> {
                Text("Tira 1d$dado y escribe el resultado. Se te sumará ${signo(modCon)} (tu modificador de Constitución); si el total queda por debajo de 1, ganas 1 PG.")
                CampoNumero(
                    tirada, { tirada = it }, "Resultado del d$dado",
                    maxLength = 2,
                    soporte = "Entre 1 y $dado",
                    error = tirada.toIntOrNull()?.takeIf { it !in 1..dado }?.let { "Un d$dado da entre 1 y $dado" }
                )
            }
            nuevo <= 12 -> Text("A partir del nivel 11 ya no se tira dado: ganas $fijos PG fijos.")
            else -> Text("Después del nivel 12 no se ganan más PG ni BA, pero el nivel sigue contando para tiradas y salvaciones.")
        }
        Separador(compacto = true)
        Text("PG: ${ui.estado.pgMaximos} → ${ui.estado.pgMaximos + pg} (+$pg)", style = Tipos.cifraMd, color = c.tinta)
        if (clase != null) {
            val baNuevo = Progresion.ba(clase, h.secundaria, nuevo)
            Text("BA: ${signo(Progresion.ba(clase, h.secundaria, h.nivel))} → ${signo(baNuevo)}", style = Tipos.cifraMd, color = c.oro)
            val nuevas = (clase.habilidades.filter { it.nivel == nuevo }).map { it.nombre }
            if (nuevas.isNotEmpty()) Text("Nuevas habilidades: ${nuevas.joinToString()}")
            if (h.esLanzador) Text("Los espacios de conjuro se actualizan solos.", style = Tipos.cuerpoSm, color = c.tintaSuave)
        }
    }
}

@Composable
private fun HabilidadDialog(h: Habilidad, onDismiss: () -> Unit, onGuardar: (Habilidad) -> Unit, onEliminar: () -> Unit) {
    var e by remember { mutableStateOf(h) }
    val atributos = listOf<Atributo?>(null) + Atributo.entries
    FormularioDialog(
        if (h.id == 0L) "Nueva habilidad" else "Habilidad",
        onDismiss = onDismiss,
        onGuardar = { onGuardar(e.copy(nombre = e.nombre.trim())) },
        guardarActivo = e.nombre.isNotBlank(),
        extraBoton = if (h.id != 0L) {
            { Boton("Eliminar", onEliminar, variante = Variante.PELIGRO_DISCRETO) }
        } else null
    ) {
        Campo(e.nombre, { e = e.copy(nombre = it) }, "Nombre")
        Desplegable(
            "Característica de la tirada", atributos, Atributo.fromAbrev(e.atributo),
            { it?.nombre ?: "Ninguna" },
            { e = e.copy(atributo = it?.abrev) }
        )
        Campo(e.descripcion, { e = e.copy(descripcion = it) }, "Descripción", unaLinea = false, minLineas = 3)
    }
}
