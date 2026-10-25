package io.github.pauverblom.cronicas.ui.wizard

import android.app.Application
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import io.github.pauverblom.cronicas.CronicasApp
import io.github.pauverblom.cronicas.data.Caracteristicas
import io.github.pauverblom.cronicas.data.PERSONALIZADA
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.Tesoro
import io.github.pauverblom.cronicas.rules.Atributo
import io.github.pauverblom.cronicas.rules.Modifiers
import io.github.pauverblom.cronicas.rules.data.Alineamientos
import io.github.pauverblom.cronicas.rules.data.ClaseDef
import io.github.pauverblom.cronicas.rules.data.Clases
import io.github.pauverblom.cronicas.rules.data.EspecieDef
import io.github.pauverblom.cronicas.rules.data.Especies
import io.github.pauverblom.cronicas.ui.common.CampoNumero
import io.github.pauverblom.cronicas.ui.common.Desplegable
import io.github.pauverblom.cronicas.ui.common.DesplegableEditable
import io.github.pauverblom.cronicas.ui.common.signo
import io.github.pauverblom.cronicas.ui.foto.RetratoTarjeta
import io.github.pauverblom.cronicas.ui.foto.Retratos
import io.github.pauverblom.cronicas.ui.foto.rememberSelectorRetrato
import io.github.pauverblom.cronicas.ui.theme.LocalColoresHoja
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WizardState(
    val paso: Int = 0,
    val nombre: String = "",
    val jugador: String = "",
    val retrato: Uri? = null,
    val especieId: String? = null,
    val especieNombre: String = "",
    val claseId: String? = null,
    val claseNombre: String = "",
    val multiclase: Boolean = false,
    val secundariaId: String? = null,
    val puntuaciones: Map<Atributo, String> = Atributo.entries.associateWith { "" },
    val primarias: Set<Atributo> = emptySet(),
    val pgMax: String = "",
    val alineamiento: String = "",
    val deidad: String = "",
    val idiomas: String = "",
    val lugarOrigen: String = "",
    val oro: String = "",
    val creando: Boolean = false
) {
    val especie: EspecieDef? get() = Especies.porId(especieId)
    val clase: ClaseDef? get() = Clases.porId(claseId)
    val secundaria: ClaseDef? get() = if (multiclase) Clases.porId(secundariaId) else null
    val primariasRequeridas: Int get() = 1 + (especie?.primariasExtra ?: 1)

    fun puntuacion(a: Atributo): Int? = puntuaciones[a]?.toIntOrNull()?.takeIf { it in 3..18 }
    fun ajustada(a: Atributo): Int? = puntuacion(a)?.let { it + (especie?.mods?.get(a) ?: 0) }

    fun pasoValido(p: Int = paso): Boolean = when (p) {
        0 -> nombre.isNotBlank()
        1 -> especieId != null && (especieId != PERSONALIZADA || especieNombre.isNotBlank())
        2 -> claseId != null && (claseId != PERSONALIZADA || claseNombre.isNotBlank()) && (!multiclase || secundaria != null)
        3 -> Atributo.entries.all { puntuacion(it) != null } && primarias.size == primariasRequeridas &&
            (clase == null || clase!!.principal in primarias)
        4 -> (pgMax.toIntOrNull() ?: 0) >= 1
        else -> true
    }
}

private val TITULOS = listOf("Nombre y retrato", "Especie", "Clase", "Características", "Puntos de golpe", "Trasfondo", "Oro inicial")

class WizardViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as CronicasApp).repository
    private val _state = MutableStateFlow(WizardState())
    val state = _state.asStateFlow()

    fun editar(f: (WizardState) -> WizardState) = _state.update(f)

    fun elegirEspecie(id: String) = _state.update { s ->
        val esp = Especies.porId(id)
        s.copy(especieId = id, idiomas = esp?.idiomas ?: s.idiomas).ajustarPrimarias()
    }

    fun elegirClase(id: String) = _state.update { s -> s.copy(claseId = id).ajustarPrimarias() }

    fun alternarPrimaria(a: Atributo) = _state.update { s ->
        if (a == s.clase?.principal) return@update s // la de la clase es obligatoria
        val nuevas = if (a in s.primarias) s.primarias - a else s.primarias + a
        if (nuevas.size > s.primariasRequeridas) s else s.copy(primarias = nuevas)
    }

    /** Mantiene marcada la característica principal de la clase y recorta si sobran primarias. */
    private fun WizardState.ajustarPrimarias(): WizardState {
        val principal = clase?.principal
        var set = primarias.toMutableSet()
        if (principal != null) set.add(principal)
        while (set.size > primariasRequeridas) set.remove(set.first { it != principal })
        return copy(primarias = set)
    }

    fun siguiente() = _state.update { if (it.pasoValido()) it.copy(paso = (it.paso + 1).coerceAtMost(TITULOS.lastIndex)) else it }
    fun anterior() = _state.update { it.copy(paso = (it.paso - 1).coerceAtLeast(0)) }

    fun crear(onCreado: (Long) -> Unit) {
        val s = _state.value
        if (s.creando) return
        _state.update { it.copy(creando = true) }
        viewModelScope.launch {
            val p = Personaje(
                nombre = s.nombre.trim(),
                jugador = s.jugador.trim(),
                claseId = s.claseId ?: PERSONALIZADA,
                claseNombre = if (s.claseId == PERSONALIZADA) s.claseNombre.trim() else "",
                claseSecundariaId = s.secundaria?.id,
                especieId = s.especieId ?: PERSONALIZADA,
                especieNombre = if (s.especieId == PERSONALIZADA) s.especieNombre.trim() else "",
                alineamiento = s.alineamiento,
                deidad = s.deidad.trim(),
                idiomas = s.idiomas.trim(),
                lugarOrigen = s.lugarOrigen.trim()
            )
            var c = Caracteristicas(personajeId = 0)
            for (a in Atributo.entries) {
                c = c.conBase(a, s.puntuacion(a) ?: 10).conPrimaria(a, a in s.primarias)
            }
            val id = repo.crearPersonaje(p, c, s.pgMax.toIntOrNull() ?: 1, Tesoro(0, oro = s.oro.toIntOrNull() ?: 0))
            s.retrato?.let { uri ->
                runCatching { Retratos.guardar(getApplication(), id, uri, null) }.getOrNull()?.let { (path, semilla) ->
                    repo.getPersonaje(id)?.let { repo.updatePersonaje(it.copy(fotoPath = path, colorSemilla = semilla)) }
                }
            }
            onCreado(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardScreen(onCancelar: () -> Unit, onCreado: (Long) -> Unit, vm: WizardViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    BackHandler(enabled = s.paso > 0) { vm.anterior() }
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Nuevo personaje", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Paso ${s.paso + 1} de ${TITULOS.size}: ${TITULOS[s.paso]}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = { IconButton(onClick = onCancelar) { Icon(Icons.Default.Close, "Cancelar") } }
                )
                LinearProgressIndicator(progress = { (s.paso + 1f) / TITULOS.size }, modifier = Modifier.fillMaxWidth())
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (s.paso > 0) OutlinedButton(onClick = vm::anterior) { Text("Atrás") }
                    Spacer(Modifier.weight(1f))
                    if (s.paso < TITULOS.lastIndex) {
                        Button(onClick = vm::siguiente, enabled = s.pasoValido()) { Text("Siguiente") }
                    } else {
                        Button(onClick = { vm.crear(onCreado) }, enabled = !s.creando) { Text("Crear personaje") }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (s.paso) {
                0 -> PasoNombre(s, vm)
                1 -> PasoEspecie(s, vm)
                2 -> PasoClase(s, vm)
                3 -> PasoCaracteristicas(s, vm)
                4 -> PasoPg(s, vm)
                5 -> PasoTrasfondo(s, vm)
                6 -> PasoOro(s, vm)
            }
        }
    }
}

@Composable
private fun PasoNombre(s: WizardState, vm: WizardViewModel) {
    val selector = rememberSelectorRetrato { uri -> vm.editar { it.copy(retrato = uri) } }
    OutlinedTextField(s.nombre, { v -> vm.editar { it.copy(nombre = v) } }, label = { Text("Nombre del personaje") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(s.jugador, { v -> vm.editar { it.copy(jugador = v) } }, label = { Text("Nombre del jugador (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Text("Retrato", style = MaterialTheme.typography.titleSmall)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (s.retrato != null) {
            AsyncImage(
                model = s.retrato,
                contentDescription = "Retrato",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RetratoTarjeta)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { selector(false) }) {
                Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text("Galería")
            }
            OutlinedButton(onClick = { selector(true) }) {
                Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("Cámara")
            }
            if (s.retrato != null) TextButton(onClick = { vm.editar { it.copy(retrato = null) } }) { Text("Quitar") }
        }
    }
    Text(
        "Con el tema \"Desde retrato\", la ficha usará los colores de esta imagen.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun OpcionCard(seleccionada: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (seleccionada) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) { Column(Modifier.padding(12.dp)) { content() } }
}

@Composable
private fun PasoEspecie(s: WizardState, vm: WizardViewModel) {
    Especies.todas.forEach { e ->
        OpcionCard(s.especieId == e.id, { vm.elegirEspecie(e.id) }) {
            Text(e.nombre, style = MaterialTheme.typography.titleMedium)
            val mods = e.mods.entries.joinToString(", ") { "${it.key.abrev} ${signo(it.value)}" }.ifBlank { "Sin modificadores" }
            Text(
                "$mods · ${if (e.pequeno) "Pequeño, 6 m" else "Mediano, 10 m"} · ${e.primariasExtra + 1} primarias",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    OpcionCard(s.especieId == PERSONALIZADA, { vm.elegirEspecie(PERSONALIZADA) }) {
        Text("Personalizada", style = MaterialTheme.typography.titleMedium)
        Text("Sin datos de reglamento: lo rellenas tú.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (s.especieId == PERSONALIZADA) {
        OutlinedTextField(s.especieNombre, { v -> vm.editar { it.copy(especieNombre = v) } }, label = { Text("Nombre de la especie") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PasoClase(s: WizardState, vm: WizardViewModel) {
    Clases.todas.forEach { c ->
        OpcionCard(s.claseId == c.id, { vm.elegirClase(c.id) }) {
            Text(c.nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                "Principal: ${c.principal.abrev} · DG d${c.dado}" + if (c.tablaConjuros != null) " · Conjuros" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    OpcionCard(s.claseId == PERSONALIZADA, { vm.elegirClase(PERSONALIZADA) }) {
        Text("Personalizada", style = MaterialTheme.typography.titleMedium)
        Text("Sin cálculos automáticos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (s.claseId == PERSONALIZADA) {
        OutlinedTextField(s.claseNombre, { v -> vm.editar { it.copy(claseNombre = v) } }, label = { Text("Nombre de la clase") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
    if (s.clase != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Multiclase (regla opcional)", style = MaterialTheme.typography.titleSmall)
                Text("Clase secundaria a mitad de nivel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(s.multiclase, { v -> vm.editar { it.copy(multiclase = v) } })
        }
        if (s.multiclase) {
            Desplegable(
                "Clase secundaria",
                Clases.todas.filter { it.id != s.claseId },
                s.secundaria,
                { it.nombre },
                { c -> vm.editar { it.copy(secundariaId = c.id) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PasoCaracteristicas(s: WizardState, vm: WizardViewModel) {
    Text(
        "Tira 3d6 seis veces y asigna los resultados. Marca ${s.primariasRequeridas} características primarias" +
            (s.clase?.let { " (${it.principal.abrev} es obligatoria para ${it.nombre.lowercase()})" } ?: "") + ".",
        style = MaterialTheme.typography.bodyMedium
    )
    val colores = LocalColoresHoja.current
    Atributo.entries.forEach { a ->
        val ajustada = s.ajustada(a)
        val modEsp = s.especie?.mods?.get(a)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CampoNumero(
                s.puntuaciones[a].orEmpty(),
                { v -> vm.editar { it.copy(puntuaciones = it.puntuaciones + (a to v)) } },
                a.nombre,
                modifier = Modifier.width(140.dp),
                maxLength = 2
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (ajustada != null) {
                    Text(
                        (if (modEsp != null) "$ajustada (${signo(modEsp)} especie)" else "$ajustada") + " → ${signo(Modifiers.modifier(ajustada))}",
                        color = colores.atributos[a] ?: MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            val obligatoria = a == s.clase?.principal
            Checkbox(checked = a in s.primarias, onCheckedChange = { vm.alternarPrimaria(a) }, enabled = !obligatoria)
            Icon(Icons.Default.Star, contentDescription = "Primaria", tint = if (a in s.primarias) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
        }
    }
    Text("${s.primarias.size} de ${s.primariasRequeridas} primarias", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PasoPg(s: WizardState, vm: WizardViewModel) {
    val modCon = s.ajustada(Atributo.CON)?.let { Modifiers.modifier(it) } ?: 0
    val clase = s.clase
    if (clase != null) {
        val dado = if (s.secundaria != null) Clases.dadoMulticlase(clase.dado, s.secundaria!!.dado).first else clase.dado
        Text("Tira 1d$dado y suma tu modificador de CON (${signo(modCon)}). El mínimo es 1.", style = MaterialTheme.typography.bodyMedium)
    } else {
        Text("Introduce los puntos de golpe máximos de tu personaje.", style = MaterialTheme.typography.bodyMedium)
    }
    CampoNumero(s.pgMax, { v -> vm.editar { it.copy(pgMax = v) } }, "PG máximos", modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PasoTrasfondo(s: WizardState, vm: WizardViewModel) {
    Desplegable(
        "Alineamiento",
        Alineamientos.todos,
        s.alineamiento.ifBlank { null },
        { it },
        { v -> vm.editar { it.copy(alineamiento = v) } },
        modifier = Modifier.fillMaxWidth(),
        soporte = Alineamientos.aviso(s.clase, s.alineamiento) ?: s.clase?.let { "Sugerido: ${it.alineamiento}" }
    )
    DesplegableEditable("Deidad", s.deidad, Alineamientos.deidades, { v -> vm.editar { it.copy(deidad = v) } }, Modifier.fillMaxWidth())
    OutlinedTextField(s.idiomas, { v -> vm.editar { it.copy(idiomas = v) } }, label = { Text("Idiomas") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(s.lugarOrigen, { v -> vm.editar { it.copy(lugarOrigen = v) } }, label = { Text("Lugar de origen") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Text(
        "La descripción, personalidad y apariencia se rellenan luego en la pestaña Personaje.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PasoOro(s: WizardState, vm: WizardViewModel) {
    s.clase?.let { Text("Oro inicial de ${it.nombre.lowercase()}: tira ${it.oroInicial}.", style = MaterialTheme.typography.bodyMedium) }
    CampoNumero(s.oro, { v -> vm.editar { it.copy(oro = v) } }, "Monedas de oro", modifier = Modifier.fillMaxWidth())
    Text(
        "Podrás comprar el equipo desde la pestaña Equipo; el precio se descuenta de tus monedas.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
