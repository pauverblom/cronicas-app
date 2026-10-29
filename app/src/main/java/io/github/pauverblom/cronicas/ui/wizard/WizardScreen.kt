package io.github.pauverblom.cronicas.ui.wizard

import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.widthIn
import io.github.pauverblom.cronicas.ui.plataforma.Archivo
import io.github.pauverblom.cronicas.ui.plataforma.ManejarAtras
import io.github.pauverblom.cronicas.ui.plataforma.modeloArchivo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pauverblom.cronicas.data.PERSONALIZADA
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.rules.Atributo
import io.github.pauverblom.cronicas.rules.Modifiers
import io.github.pauverblom.cronicas.rules.data.Alineamientos
import io.github.pauverblom.cronicas.rules.data.ClaseDef
import io.github.pauverblom.cronicas.rules.data.Clases
import io.github.pauverblom.cronicas.rules.data.EspecieDef
import io.github.pauverblom.cronicas.rules.data.Especies
import io.github.pauverblom.cronicas.rules.data.TablaConjuros
import io.github.pauverblom.cronicas.ui.common.Brasas
import io.github.pauverblom.cronicas.ui.common.CampoNumero
import io.github.pauverblom.cronicas.ui.common.Carrusel
import io.github.pauverblom.cronicas.ui.common.Desplegable
import io.github.pauverblom.cronicas.ui.common.DesplegableEditable
import io.github.pauverblom.cronicas.ui.common.Ilustracion
import io.github.pauverblom.cronicas.ui.common.Ilustraciones
import io.github.pauverblom.cronicas.ui.common.Llama
import io.github.pauverblom.cronicas.ui.common.animacionesActivas
import io.github.pauverblom.cronicas.ui.common.animarLlama
import io.github.pauverblom.cronicas.ui.common.llamaDe
import io.github.pauverblom.cronicas.ui.common.signo
import io.github.pauverblom.cronicas.ui.foto.rememberSelectorRetrato
import io.github.pauverblom.cronicas.ui.hud.Boton
import io.github.pauverblom.cronicas.ui.hud.BotonIcono
import io.github.pauverblom.cronicas.ui.hud.Cabecera
import io.github.pauverblom.cronicas.ui.hud.Campo
import io.github.pauverblom.cronicas.ui.hud.Casilla
import io.github.pauverblom.cronicas.ui.hud.DatosTarjeta
import io.github.pauverblom.cronicas.ui.hud.DescripcionTarjeta
import io.github.pauverblom.cronicas.ui.hud.Gema
import io.github.pauverblom.cronicas.ui.hud.Glifo
import io.github.pauverblom.cronicas.ui.hud.IconoGlifo
import io.github.pauverblom.cronicas.ui.hud.MechaPasos
import io.github.pauverblom.cronicas.ui.hud.NombreTarjeta
import io.github.pauverblom.cronicas.ui.hud.Retrato
import io.github.pauverblom.cronicas.ui.hud.SelloLacre
import io.github.pauverblom.cronicas.ui.hud.Separador
import io.github.pauverblom.cronicas.ui.hud.Sigilo
import io.github.pauverblom.cronicas.ui.hud.Subtitulo
import io.github.pauverblom.cronicas.ui.hud.TituloPantalla
import io.github.pauverblom.cronicas.ui.hud.Variante
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class WizardState(
    val paso: Int = 0,
    val nombre: String = "",
    val jugador: String = "",
    val retrato: Archivo? = null,
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
    val edad: String = "",
    val genero: String = "",
    val altura: String = "",
    val peso: String = "",
    val ojos: String = "",
    val cabello: String = "",
    val oro: String = "",
    val creando: Boolean = false
) {
    val especie: EspecieDef? get() = Especies.porId(especieId)
    val clase: ClaseDef? get() = Clases.porId(claseId)
    val secundaria: ClaseDef? get() = if (multiclase) Clases.porId(secundariaId) else null
    val primariasRequeridas: Int get() = 1 + (especie?.primariasExtra ?: 1)

    fun puntuacion(a: Atributo): Int? = puntuaciones[a]?.toIntOrNull()?.takeIf { it in 3..18 }

    /** Dado de golpe de nivel 1 (combinado si hay multiclase), o null con clase personalizada. */
    val dadoGolpe: Int? get() = clase?.let { c -> secundaria?.let { Clases.dadoMulticlase(c.dado, it.dado).first } ?: c.dado }
    val modCon: Int get() = ajustada(Atributo.CON)?.let { Modifiers.modifier(it) } ?: 0

    /** Con clase de reglamento [pgMax] es la tirada del dado y se suma CON (mínimo 1); si no, son los PG totales. */
    val pgTotal: Int? get() {
        val n = pgMax.toIntOrNull() ?: return null
        val dado = dadoGolpe ?: return n.takeIf { it >= 1 }
        return if (n in 1..dado) maxOf(1, n + modCon) else null
    }
    fun ajustada(a: Atributo): Int? = puntuacion(a)?.let { it + (especie?.mods?.get(a) ?: 0) }

    fun pasoValido(p: Int = paso): Boolean = when (p) {
        0 -> nombre.isNotBlank()
        1 -> especieId != null && (especieId != PERSONALIZADA || especieNombre.isNotBlank())
        2 -> claseId != null && (claseId != PERSONALIZADA || claseNombre.isNotBlank()) && (!multiclase || secundaria != null)
        3 -> Atributo.entries.all { puntuacion(it) != null } && primarias.size == primariasRequeridas &&
            (clase == null || clase!!.principal in primarias)
        4 -> pgTotal != null
        else -> true
    }
}

internal val TITULOS = listOf("Nombre y retrato", "Especie", "Clase", "Características", "Puntos de golpe", "Trasfondo", "Oro inicial")

/** Ancho máximo del asistente en pantallas grandes. */
private val AnchoAsistente = 720.dp

@Composable
fun WizardScreen(onCancelar: () -> Unit, onCreado: (Long) -> Unit, vm: WizardViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    val llama = animarLlama(llamaDe(s.claseId))
    val animado = animacionesActivas()
    val c = Marca.colores
    // Mientras se forja el personaje: null = sin finale; el id llega cuando termina de guardarse
    var forja by remember { mutableStateOf<Forja?>(null) }
    ManejarAtras(activo = s.paso > 0 && forja == null) { vm.anterior() }
    Column(Modifier.fillMaxSize()) {
        Cabecera(navegacion = {
            BotonIcono(onCancelar, "Cancelar") { Icon(Icons.Sharp.Close, null, Modifier.size(24.dp)) }
        }) { TituloPantalla("Nuevo personaje") }
        // En tablet y ordenador el asistente no pasa de [AnchoAsistente], centrado
        MechaPasos(
            s.paso + 1, TITULOS.size, TITULOS[s.paso], llama,
            Modifier.align(Alignment.CenterHorizontally).widthIn(max = AnchoAsistente).padding(horizontal = 16.dp).padding(bottom = 8.dp)
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Brasas(llama, Modifier.matchParentSize(), densidad = 0.5f)
            // Avanzar entra por la derecha y retroceder por la izquierda; cada paso empieza arriba del todo
            AnimatedContent(
                s.paso,
                transitionSpec = {
                    val sentido = if (targetState > initialState) 1 else -1
                    val t = if (animado) 300 else 0
                    (slideInHorizontally(tween(t)) { sentido * it / 3 } + fadeIn(tween(t))) togetherWith
                        (slideOutHorizontally(tween(t)) { -sentido * it / 3 } + fadeOut(tween(t * 3 / 4)))
                },
                label = "paso"
            ) { paso ->
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier
                                // Los pasos con carrusel, a todo el ancho: se ven las tarjetas vecinas
                            .widthIn(max = if (paso == 1 || paso == 2) Dp.Infinity else AnchoAsistente)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (paso) {
                            0 -> PasoNombre(s, vm, llama)
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
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, c.vitela100)))
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier
                    .widthIn(max = AnchoAsistente)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (s.paso > 0) Boton("Atrás", vm::anterior)
                Spacer(Modifier.weight(1f))
                if (s.paso < TITULOS.lastIndex) {
                    Boton("Siguiente", vm::siguiente, variante = Variante.DORADO, activo = s.pasoValido())
                } else {
                    Boton(
                        "Crear personaje",
                        {
                            if (animado) {
                                forja = Forja()
                                vm.crear { id -> forja = forja?.copy(id = id) }
                            } else {
                                vm.crear(onCreado)
                            }
                        },
                        variante = Variante.DORADO,
                        activo = !s.creando
                    )
                }
            }
        }
    }
    forja?.let { f ->
        FinaleForja(s.nombre.trim(), s.retrato, llama, listo = f.id != null, onFin = { f.id?.let(onCreado) })
    }
}

/** Estado del finale: [id] es null hasta que el personaje queda guardado. */
private data class Forja(val id: Long? = null)

/**
 * La forja (unos 2 s): se oscurece la pantalla, aparece un sigilo, sube un muro de brasas, el retrato surge en su
 * anillo de fuego, el nombre se cierra desde un espaciado amplio y el sello de lacre cae encima con un golpe
 * háptico. Al terminar, y en cuanto el personaje esté guardado ([listo]), llama a [onFin]. Tocar la pantalla lo salta.
 */
@Composable
private fun FinaleForja(nombre: String, retrato: Archivo?, llama: Llama, listo: Boolean, onFin: () -> Unit) {
    val haptica = LocalHapticFeedback.current
    val c = Marca.colores
    val fondo = remember { Animatable(0f) }
    val sigilo = remember { Animatable(0f) }
    val fuego = remember { Animatable(0f) }
    val retratoAnim = remember { Animatable(0f) }
    val nombreAnim = remember { Animatable(0f) }
    val sello = remember { Animatable(0f) }
    var terminado by remember { mutableStateOf(false) }
    val fin by rememberUpdatedState(onFin)
    LaunchedEffect(Unit) {
        launch { fondo.animateTo(1f, tween(300)) }
        launch { sigilo.animateTo(1f, tween(600)) }
        launch { fuego.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) }
        delay(300)
        launch { retratoAnim.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
        delay(400)
        launch { nombreAnim.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        delay(650)
        sello.animateTo(1f, tween(220, easing = FastOutLinearInEasing))
        haptica.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(500)
        terminado = true
    }
    LaunchedEffect(terminado, listo) { if (terminado && listo) fin() }

    ManejarAtras {} // no se puede volver atrás a medio forjar
    Box(
        Modifier
            .fillMaxSize()
            .background(c.velo.copy(alpha = (c.velo.alpha + 0.1f).coerceAtMost(1f) * fondo.value))
            .clickable(interactionSource = null, indication = null) { terminado = true },
        contentAlignment = Alignment.Center
    ) {
        // Resplandor que sube desde abajo y muro de brasas
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val alto = size.height * 0.65f * fuego.value
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, llama.nucleo.copy(alpha = 0.45f), llama.brillo.copy(alpha = 0.7f)),
                            startY = size.height - alto, endY = size.height
                        ),
                        topLeft = Offset(0f, size.height - alto),
                        size = Size(size.width, alto)
                    )
                }
        )
        Brasas(llama, Modifier.fillMaxSize(), densidad = 7f)
        Sigilo(300.dp, Modifier.graphicsLayer { alpha = sigilo.value }, llama = llama)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Retrato(
                retrato?.let(::modeloArchivo), nombre, llama, 160.dp,
                Modifier.graphicsLayer {
                    val e = 0.3f + 0.7f * retratoAnim.value
                    scaleX = e; scaleY = e; alpha = retratoAnim.value.coerceIn(0f, 1f)
                },
                anillo = true
            )
            Text(
                nombre,
                // El espaciado se cierra de 0,4 em a 0,02 em mientras aparece
                style = Tipos.tituloXl.copy(letterSpacing = (0.02f + 0.38f * (1f - nombreAnim.value)).em),
                color = llama.chispa,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp).graphicsLayer { alpha = nombreAnim.value }
            )
            SelloLacre(nombre, Modifier.graphicsLayer {
                // Cae desde grande y transparente hasta su sitio
                val e = 1.6f - 0.6f * sello.value
                scaleX = e; scaleY = e; alpha = sello.value; rotationZ = -12f * sello.value
            })
        }
    }
}

/** Texto de instrucciones de un paso del asistente. */
@Composable
private fun Instruccion(texto: String) {
    // Alineada con la columna del asistente también en los pasos con carrusel, que van a todo el ancho
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Text(texto, style = Tipos.cuerpo, color = Marca.colores.tinta, modifier = Modifier.widthIn(max = AnchoAsistente - 32.dp).fillMaxWidth())
    }
}

@Composable
private fun Ayuda(texto: String) {
    Text(texto, style = Tipos.cuerpoSm, color = Marca.colores.tintaSuave)
}

@Composable
private fun PasoNombre(s: WizardState, vm: WizardViewModel, llama: Llama) {
    val selector = rememberSelectorRetrato { uri -> vm.editar { it.copy(retrato = uri) } }
    Campo(s.nombre, { v -> vm.editar { it.copy(nombre = v) } }, "Nombre del personaje")
    Campo(s.jugador, { v -> vm.editar { it.copy(jugador = v) } }, "Nombre del jugador (opcional)")
    Subtitulo("Retrato", Modifier.padding(top = 8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Retrato(s.retrato?.let(::modeloArchivo), s.nombre.ifBlank { "?" }, llama, 112.dp, anillo = true)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Boton("Galería", { selector(false) })
            Boton("Cámara", { selector(true) })
            if (s.retrato != null) Boton("Quitar", { vm.editar { it.copy(retrato = null) } }, variante = Variante.PELIGRO_DISCRETO)
        }
    }
    Ayuda("Con el tema «Desde retrato», la ficha usará los colores de esta imagen.")
}

/** Datos de una especie para su tarjeta. */
private fun datosEspecie(e: EspecieDef) = listOf(
    "Características" to e.mods.entries.joinToString(", ") { "${it.key.nombre} ${signo(it.value)}" }.ifBlank { "sin modificadores" },
    "Tamaño" to if (e.pequeno) "pequeño, 6 m de movimiento" else "mediano, 10 m de movimiento",
    "Primarias" to "la de la clase y ${e.primariasExtra} a elegir",
    "Rasgos" to e.rasgos.joinToString { it.nombre }
)

@Composable
private fun PasoEspecie(s: WizardState, vm: WizardViewModel) {
    val opciones = Especies.todas.map { it.id } + PERSONALIZADA
    val pager = rememberPagerState(initialPage = opciones.indexOf(s.especieId).coerceAtLeast(0)) { opciones.size }
    Instruccion("Desliza para ver las especies y toca la que quieras.")
    // Hasta elegir clase, las especies arden con el fuego común (o el de la clase si se vuelve atrás)
    Carrusel(pager, opciones, { it == s.especieId }, vm::elegirEspecie, clave = { it }, llama = { llamaDe(s.claseId) }) { id ->
        val e = Especies.porId(id)
        if (e == null) {
            Ilustracion(null, "Personalizada", 168.dp)
            NombreTarjeta("Personalizada")
            DescripcionTarjeta("Una especie que no está en el reglamento. Sin modificadores automáticos: lo rellenas tú.")
            return@Carrusel
        }
        Ilustracion(Ilustraciones.especie(e.id), e.nombre, 168.dp, archivoFaltante = Ilustraciones.archivoEspecie(e.id))
        NombreTarjeta(e.nombre)
        DescripcionTarjeta(e.descripcion)
        DatosTarjeta(datosEspecie(e))
    }
    if (s.especieId == PERSONALIZADA) {
        Campo(s.especieNombre, { v -> vm.editar { it.copy(especieNombre = v) } }, "Nombre de la especie")
    }
}

/** Datos de una clase para su tarjeta. */
private fun datosClase(c: ClaseDef) = listOfNotNull(
    "Característica principal" to c.principal.nombre,
    "Dado de golpe" to "d${c.dado}",
    "Alineamiento" to c.alineamiento,
    c.tablaConjuros?.let { "Magia" to if (it == TablaConjuros.DIVINA) "conjuros divinos" else "conjuros arcanos" },
    "Habilidades" to c.habilidades.filter { it.nivel == 1 }.joinToString { it.nombre },
    c.fuente?.let { "Fuente" to it }
)

@Composable
private fun PasoClase(s: WizardState, vm: WizardViewModel) {
    val opciones = Clases.todas.map { it.id } + PERSONALIZADA
    val pager = rememberPagerState(initialPage = opciones.indexOf(s.claseId).coerceAtLeast(0)) { opciones.size }
    Instruccion("Desliza para ver las clases y toca la que quieras.")
    // Cada clase arde con su propio fuego
    Carrusel(pager, opciones, { it == s.claseId }, vm::elegirClase, clave = { it }, llama = { llamaDe(it) }) { id ->
        val cl = Clases.porId(id)
        if (cl == null) {
            Ilustracion(null, "Personalizada", 168.dp)
            NombreTarjeta("Personalizada")
            DescripcionTarjeta("Una clase que no está en el reglamento. Sin cálculos automáticos: lo rellenas tú.")
            return@Carrusel
        }
        Ilustracion(Ilustraciones.clase(cl.id), cl.nombre, 168.dp, archivoFaltante = Ilustraciones.archivoClase(cl.id))
        NombreTarjeta(cl.nombre)
        DescripcionTarjeta(cl.descripcion)
        DatosTarjeta(datosClase(cl))
    }
    if (s.claseId == PERSONALIZADA) {
        Campo(s.claseNombre, { v -> vm.editar { it.copy(claseNombre = v) } }, "Nombre de la clase")
    }
    if (s.clase != null) {
        Casilla(s.multiclase, { v -> vm.editar { it.copy(multiclase = v) } }) {
            Column(Modifier.padding(start = 4.dp)) {
                Text("Multiclase (regla opcional)", style = Tipos.ui, color = Marca.colores.tinta)
                Text("Clase secundaria a mitad de nivel", style = Tipos.caption, color = Marca.colores.tintaSuave)
            }
        }
        if (s.multiclase) {
            Desplegable(
                "Clase secundaria",
                Clases.todas.filter { it.id != s.claseId },
                s.secundaria,
                { it.nombre },
                { cl -> vm.editar { it.copy(secundariaId = cl.id) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PasoCaracteristicas(s: WizardState, vm: WizardViewModel) {
    val c = Marca.colores
    Instruccion(
        "Tira 3d6 seis veces y asigna los resultados (cada uno entre 3 y 18). Marca ${s.primariasRequeridas} características primarias" +
            (s.clase?.let { " (${it.principal.nombre} es obligatoria para ${it.nombre.lowercase()})" } ?: "") + "."
    )
    Atributo.entries.forEach { a ->
        val ajustada = s.ajustada(a)
        val modEsp = s.especie?.mods?.get(a)
        val color = c.atributo(a)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CampoNumero(
                s.puntuaciones[a].orEmpty(),
                { v -> vm.editar { it.copy(puntuaciones = it.puntuaciones + (a to v)) } },
                a.nombre,
                modifier = Modifier.width(132.dp),
                maxLength = 2,
                error = s.puntuaciones[a]?.toIntOrNull()?.takeIf { it !in 3..18 }?.let { "Entre 3 y 18" }
            )
            Spacer(Modifier.width(12.dp))
            // El modificador que da la puntuación y, si la especie la cambia, la cuenta que lleva a ella
            Column(Modifier.weight(1f).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val tirada = s.puntuacion(a)
                if (ajustada != null && tirada != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Gema(color, 9.dp)
                        Text("Modificador", style = Tipos.uiSm, color = c.tintaSuave)
                        Text(signo(Modifiers.modifier(ajustada)), style = Tipos.cifraMd, color = color)
                    }
                    if (modEsp != null) {
                        Text(
                            "$tirada ${signo(modEsp)} por ${s.especie?.nombre?.lowercase().orEmpty()} = $ajustada",
                            style = Tipos.caption,
                            color = c.tintaSuave
                        )
                    }
                } else if (modEsp != null) {
                    Text("${signo(modEsp)} por ${s.especie?.nombre?.lowercase().orEmpty()}", style = Tipos.caption, color = c.tintaSuave)
                }
            }
            val obligatoria = a == s.clase?.principal
            Casilla(
                a in s.primarias, { vm.alternarPrimaria(a) },
                Modifier.padding(top = 18.dp),
                activa = !obligatoria,
                descripcion = "${a.nombre} primaria"
            ) {
                IconoGlifo(Glifo.ESTRELLA, tamano = 18.dp, color = if (a in s.primarias) c.oro else c.tintaTenue)
            }
        }
    }
    Ayuda("${s.primarias.size} de ${s.primariasRequeridas} primarias")
}

@Composable
private fun PasoPg(s: WizardState, vm: WizardViewModel) {
    val dado = s.dadoGolpe
    val c = Marca.colores
    if (dado != null) {
        Instruccion(
            "Tira 1d$dado y escribe el resultado. Se te sumará ${signo(s.modCon)} (tu modificador de Constitución); " +
                "si el total queda por debajo de 1, tendrás 1 PG."
        )
        val tirada = s.pgMax.toIntOrNull()
        CampoNumero(
            s.pgMax, { v -> vm.editar { it.copy(pgMax = v) } }, "Resultado del d$dado",
            modifier = Modifier.fillMaxWidth(),
            maxLength = 2,
            soporte = "Entre 1 y $dado",
            error = tirada?.takeIf { it !in 1..dado }?.let { "Un d$dado da entre 1 y $dado" }
        )
        s.pgTotal?.let {
            Separador(compacto = true)
            Text("PG máximos: $it", style = Tipos.cifraMd, color = c.pgSano, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    } else {
        Instruccion("Introduce los puntos de golpe máximos de tu personaje.")
        CampoNumero(s.pgMax, { v -> vm.editar { it.copy(pgMax = v) } }, "PG máximos", modifier = Modifier.fillMaxWidth())
    }
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
    Campo(s.idiomas, { v -> vm.editar { it.copy(idiomas = v) } }, "Idiomas", unaLinea = false)
    Campo(s.lugarOrigen, { v -> vm.editar { it.copy(lugarOrigen = v) } }, "Lugar de origen")
    Subtitulo("Apariencia (opcional)", Modifier.padding(top = 8.dp))
    @Composable
    fun Pareja(a: String, va: String, fa: (WizardState, String) -> WizardState, b: String, vb: String, fb: (WizardState, String) -> WizardState) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo(va, { v -> vm.editar { fa(it, v) } }, a, Modifier.weight(1f))
            Campo(vb, { v -> vm.editar { fb(it, v) } }, b, Modifier.weight(1f))
        }
    }
    Pareja("Edad", s.edad, { w, v -> w.copy(edad = v) }, "Género", s.genero, { w, v -> w.copy(genero = v) })
    Pareja("Altura", s.altura, { w, v -> w.copy(altura = v) }, "Peso", s.peso, { w, v -> w.copy(peso = v) })
    Pareja("Ojos", s.ojos, { w, v -> w.copy(ojos = v) }, "Cabello", s.cabello, { w, v -> w.copy(cabello = v) })
    Ayuda("La descripción, personalidad, trasfondo y cicatrices se rellenan luego en la pestaña Personaje.")
}

@Composable
private fun PasoOro(s: WizardState, vm: WizardViewModel) {
    s.clase?.let { Instruccion("Oro inicial de ${it.nombre.lowercase()}: tira ${it.oroInicial}.") }
    CampoNumero(s.oro, { v -> vm.editar { it.copy(oro = v) } }, "Monedas de oro", modifier = Modifier.fillMaxWidth(), sufijo = "mo")
    Ayuda("Podrás comprar el equipo desde la pestaña Equipo; el precio se descuenta de tus monedas.")
}
