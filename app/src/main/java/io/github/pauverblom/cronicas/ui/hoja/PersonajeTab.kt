package io.github.pauverblom.cronicas.ui.hoja

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.pauverblom.cronicas.ui.common.SeccionCard
import io.github.pauverblom.cronicas.ui.common.signo
import io.github.pauverblom.cronicas.ui.foto.RetratoAvatar
import io.github.pauverblom.cronicas.ui.foto.RetratoTarjeta
import io.github.pauverblom.cronicas.ui.foto.rememberSelectorRetrato

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
            Text("Total actual: ${ui.personaje.experiencia} PX")
            CampoNumero(texto, { texto = it }, "PX ganados", negativos = true, soporte = "Usa un número negativo para restar")
        }
    }
    habilidad?.let { h -> HabilidadDialog(h, onDismiss = { habilidad = null }, onGuardar = { vm.guardarHabilidad(it); habilidad = null }, onEliminar = { vm.eliminarHabilidad(h); habilidad = null }) }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    if (valor.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(110.dp))
        Text(valor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BloqueIdentidad(ui: HojaUi, vm: HojaViewModel, onEditar: () -> Unit) {
    val p = ui.personaje
    val h = ui.hoja
    var menuRetrato by remember { mutableStateOf(false) }
    val selector = rememberSelectorRetrato(vm::setRetrato)
    SeccionCard("Personaje", accion = {
        IconButton(onClick = onEditar, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, "Editar", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }) {
        Row {
            Box {
                RetratoAvatar(p.fotoPath, p.nombre, 96.dp, Modifier.clickable { menuRetrato = true }, shape = RetratoTarjeta)
                DropdownMenu(expanded = menuRetrato, onDismissRequest = { menuRetrato = false }) {
                    DropdownMenuItem(text = { Text("Elegir de la galería") }, onClick = { menuRetrato = false; selector(false) })
                    DropdownMenuItem(text = { Text("Hacer una foto") }, onClick = { menuRetrato = false; selector(true) })
                    if (p.fotoPath != null) DropdownMenuItem(text = { Text("Quitar retrato") }, onClick = { menuRetrato = false; vm.quitarRetrato() })
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.nombre, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    h.nombreClase + (h.secundaria?.let { " / ${it.nombre} ${h.nivelSecundario}" } ?: "") + " · Nivel ${h.nivel}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(h.nombreEspecie, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
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
    SeccionCard("Experiencia") {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${p.experiencia}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                h.xpSiguiente?.let { " / $it PX para nivel ${h.nivel + 1}" } ?: " PX",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }
        h.xpSiguiente?.let { sig ->
            val desde = h.clase?.let { Progresion.xpParaNivel(it, h.secundaria, h.nivel) } ?: 0
            val progreso = if (sig > desde) (p.experiencia - desde).toFloat() / (sig - desde) else 1f
            LinearProgressIndicator(progress = { progreso.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }
        if (h.puedeSubir) {
            Text("¡Tienes PX suficientes para subir de nivel!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(onClick = onAnadir) { Icon(Icons.Default.Add, null); Text("PX") }
            if (h.puedeSubir) {
                Button(onClick = onSubir) { Icon(Icons.Default.TrendingUp, null); Spacer(Modifier.width(6.dp)); Text("Subir de nivel") }
            } else {
                OutlinedButton(onClick = onSubir) { Text("Subir de nivel") }
            }
        }
    }
}

@Composable
private fun BloqueCaracteristicas(ui: HojaUi, onClick: (CaracteristicaHoja) -> Unit) {
    SeccionCard("Características") {
        ui.hoja.caracteristicas.forEachIndexed { i, c ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            val color = colorAtributo(c.atributo)
            Row(
                Modifier.fillMaxWidth().clickable { onClick(c) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(c.atributo.abrev, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                Text(c.atributo.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (c.primaria) Icon(Icons.Default.Star, "Primaria", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(48.dp)) {
                    Text("${c.puntuacion}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (c.puntuacion != c.base) Text("(${c.base})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Row(Modifier.width(52.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (c.modForzado) Icon(Icons.Default.Edit, "Modificador fijado a mano", Modifier.size(12.dp), tint = color)
                    Text(signo(c.mod), color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }
        val extra = ui.hoja.especie?.mods?.takeIf { it.isNotEmpty() }
        if (extra != null) {
            Text(
                "Incluye los modificadores de ${ui.hoja.nombreEspecie.lowercase()}: " + extra.entries.joinToString { "${it.key.abrev} ${signo(it.value)}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BloqueHabilidades(habilidades: List<Habilidad>, onEditar: (Habilidad) -> Unit) {
    SeccionCard("Habilidades y rasgos", accion = {
        IconButton(onClick = { onEditar(Habilidad(personajeId = 0, nombre = "")) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, "Añadir habilidad", tint = MaterialTheme.colorScheme.primary)
        }
    }) {
        val grupos = listOf(
            Habilidad.ORIGEN_CLASE to "De clase",
            Habilidad.ORIGEN_ESPECIE to "De especie",
            Habilidad.ORIGEN_PROPIA to "Otras"
        )
        if (habilidades.isEmpty()) Text("Sin habilidades", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        grupos.forEach { (origen, titulo) ->
            val lista = habilidades.filter { it.origen == origen }
            if (lista.isEmpty()) return@forEach
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
            lista.forEach { h ->
                var abierta by remember(h.id) { mutableStateOf(false) }
                Column(Modifier.fillMaxWidth().clickable { abierta = !abierta }.padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(h.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        h.atributo?.let { abrev ->
                            val a = Atributo.fromAbrev(abrev)
                            Text(abrev, color = a?.let { colorAtributo(it) } ?: MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        IconButton(onClick = { onEditar(h) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, "Editar", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    if (h.descripcion.isNotBlank()) {
                        Text(
                            h.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (abierta) Int.MAX_VALUE else 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BloqueBio(p: Personaje, onEditar: () -> Unit) {
    SeccionCard("Descripción", accion = {
        IconButton(onClick = onEditar, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, "Editar", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }) {
        val apariencia = listOf("Edad" to p.edad, "Género" to p.genero, "Altura" to p.altura, "Peso" to p.peso, "Ojos" to p.ojos, "Cabello" to p.cabello)
        apariencia.forEach { (k, v) -> FilaDato(k, v) }
        listOf("Descripción" to p.descripcion, "Personalidad" to p.personalidad, "Trasfondo" to p.trasfondo, "Cicatrices" to p.cicatrices).forEach { (k, v) ->
            if (v.isNotBlank()) {
                Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
                Text(v, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (apariencia.all { it.second.isBlank() } && p.descripcion.isBlank() && p.personalidad.isBlank() && p.trasfondo.isBlank() && p.cicatrices.isBlank()) {
            Text("Pulsa el lápiz para describir a tu personaje.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        OutlinedTextField(e.nombre, { e = e.copy(nombre = it) }, label = { Text("Nombre") }, singleLine = true)
        Desplegable("Clase", clases, clases.firstOrNull { it.id == e.claseId }, { it.nombre }, { e = e.copy(claseId = it.id) })
        if (e.claseId == PERSONALIZADA) {
            OutlinedTextField(e.claseNombre, { e = e.copy(claseNombre = it) }, label = { Text("Nombre de la clase") }, singleLine = true)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Multiclase", Modifier.weight(1f))
                Switch(e.claseSecundariaId != null, { on ->
                    e = e.copy(claseSecundariaId = if (on) Clases.todas.first { it.id != e.claseId }.id else null)
                })
            }
            if (e.claseSecundariaId != null) {
                val sec = clases.filter { it.id != e.claseId && it.id != PERSONALIZADA }
                Desplegable("Clase secundaria", sec, sec.firstOrNull { it.id == e.claseSecundariaId }, { it.nombre }, { e = e.copy(claseSecundariaId = it.id) })
            }
        }
        Desplegable("Especie", especies, especies.firstOrNull { it.id == e.especieId }, { it.nombre }, { e = e.copy(especieId = it.id) })
        if (e.especieId == PERSONALIZADA) {
            OutlinedTextField(e.especieNombre, { e = e.copy(especieNombre = it) }, label = { Text("Nombre de la especie") }, singleLine = true)
        }
        CampoNumero(nivelTxt, { nivelTxt = it }, "Nivel", soporte = "Cambiarlo aquí no suma PG; usa «Subir de nivel»")
        Desplegable(
            "Alineamiento", Alineamientos.todos, e.alineamiento.ifBlank { null }, { it }, { e = e.copy(alineamiento = it) },
            soporte = Alineamientos.aviso(Clases.porId(e.claseId), e.alineamiento)
        )
        DesplegableEditable("Deidad", e.deidad, Alineamientos.deidades, { e = e.copy(deidad = it) })
        OutlinedTextField(e.idiomas, { e = e.copy(idiomas = it) }, label = { Text("Idiomas") })
        OutlinedTextField(e.jugador, { e = e.copy(jugador = it) }, label = { Text("Jugador") }, singleLine = true)
        OutlinedTextField(e.lugarOrigen, { e = e.copy(lugarOrigen = it) }, label = { Text("Lugar de origen") }, singleLine = true)
    }
}

@Composable
private fun BioDialog(p: Personaje, onDismiss: () -> Unit, onGuardar: (Personaje) -> Unit) {
    var e by remember { mutableStateOf(p) }
    FormularioDialog("Descripción", onDismiss = onDismiss, onGuardar = { onGuardar(e) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(e.edad, { e = e.copy(edad = it) }, label = { Text("Edad") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(e.genero, { e = e.copy(genero = it) }, label = { Text("Género") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(e.altura, { e = e.copy(altura = it) }, label = { Text("Altura") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(e.peso, { e = e.copy(peso = it) }, label = { Text("Peso") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(e.ojos, { e = e.copy(ojos = it) }, label = { Text("Ojos") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(e.cabello, { e = e.copy(cabello = it) }, label = { Text("Cabello") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(e.descripcion, { e = e.copy(descripcion = it) }, label = { Text("Descripción") }, minLines = 2)
        OutlinedTextField(e.personalidad, { e = e.copy(personalidad = it) }, label = { Text("Personalidad") }, minLines = 2)
        OutlinedTextField(e.trasfondo, { e = e.copy(trasfondo = it) }, label = { Text("Trasfondo") }, minLines = 2)
        OutlinedTextField(e.cicatrices, { e = e.copy(cicatrices = it) }, label = { Text("Cicatrices") })
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(primaria, { primaria = it })
            Text("Primaria (ND 12 en vez de 18)")
        }
        if (ui.hoja.clase?.principal == c.atributo && !primaria) {
            Text("Es la característica principal de tu clase.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        CampoNumero(mod, { mod = it }, "Modificador fijado a mano", negativos = true, soporte = "Vacío = según la tabla (${signo(Modifiers.modifier(ajustada))})")
    }
}

@Composable
private fun SubirNivelDialog(ui: HojaUi, onDismiss: () -> Unit, onConfirmar: (nivel: Int, pg: Int) -> Unit) {
    val h = ui.hoja
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
        guardarActivo = nuevo <= Progresion.NIVEL_MAXIMO && (!necesitaTirada || tirada.toIntOrNull() != null)
    ) {
        when {
            clase == null -> {
                Text("Introduce los PG que ganas en este nivel.")
                CampoNumero(tirada, { tirada = it }, "PG ganados")
            }
            nuevo <= 10 -> {
                Text("Tira 1d$dado y escribe el resultado. Se suma tu modificador de CON (${signo(modCon)}), con un mínimo de 1 PG.")
                CampoNumero(tirada, { tirada = it }, "Resultado del d$dado")
            }
            nuevo <= 12 -> Text("A partir del nivel 11 ya no se tira dado: ganas $fijos PG fijos.")
            else -> Text("Después del nivel 12 no se ganan más PG ni BA, pero el nivel sigue contando para tiradas y salvaciones.")
        }
        HorizontalDivider()
        Text("PG: ${ui.estado.pgMaximos} → ${ui.estado.pgMaximos + pg} (+$pg)", fontWeight = FontWeight.Bold)
        if (clase != null) {
            val baNuevo = Progresion.ba(clase, h.secundaria, nuevo)
            Text("BA: ${signo(Progresion.ba(clase, h.secundaria, h.nivel))} → ${signo(baNuevo)}")
            val nuevas = (clase.habilidades.filter { it.nivel == nuevo }).map { it.nombre }
            if (nuevas.isNotEmpty()) Text("Nuevas habilidades: ${nuevas.joinToString()}")
            if (h.esLanzador) Text("Los espacios de conjuro se actualizan solos.", style = MaterialTheme.typography.bodySmall)
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
            { TextButton(onClick = onEliminar) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } }
        } else null
    ) {
        OutlinedTextField(e.nombre, { e = e.copy(nombre = it) }, label = { Text("Nombre") }, singleLine = true)
        Desplegable(
            "Característica de la tirada", atributos, Atributo.fromAbrev(e.atributo),
            { it?.let { a -> "${a.abrev} · ${a.nombre}" } ?: "Ninguna" },
            { e = e.copy(atributo = it?.abrev) }
        )
        OutlinedTextField(e.descripcion, { e = e.copy(descripcion = it) }, label = { Text("Descripción") }, minLines = 3)
    }
}
