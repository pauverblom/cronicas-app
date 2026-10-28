package io.github.pauverblom.cronicas.data

import io.github.pauverblom.cronicas.web.jsGuardarLocal
import io.github.pauverblom.cronicas.web.jsLeerLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Los datos de la versión web: cada personaje es su ficha entera ([PersonajeExportado]), con ids únicos en todo el
 * almacén (como en la base de datos de Android), guardada en localStorage. El catálogo de conjuros viene de
 * conjuros.json con ids 1..N; los conjuros propios tienen ids propios y también se guardan.
 */
@OptIn(FlowPreview::class)
class Almacen(val catalogo: List<Conjuro>, scope: CoroutineScope) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serFichas = MapSerializer(Long.serializer(), PersonajeExportado.serializer())
    private val serPropios = ListSerializer(Conjuro.serializer())

    private val _fichas = MutableStateFlow(
        jsLeerLocal(CLAVE_FICHAS)?.let { runCatching { json.decodeFromString(serFichas, it) }.getOrNull() } ?: emptyMap()
    )
    val fichas: StateFlow<Map<Long, PersonajeExportado>> = _fichas.asStateFlow()

    private val _propios = MutableStateFlow(
        jsLeerLocal(CLAVE_PROPIOS)?.let { runCatching { json.decodeFromString(serPropios, it) }.getOrNull() } ?: emptyList()
    )
    val propios: StateFlow<List<Conjuro>> = _propios.asStateFlow()

    private var contador: Long = jsLeerLocal(CLAVE_CONTADOR)?.toLongOrNull() ?: PRIMER_ID

    init {
        // Se guarda poco después de cada cambio, no en cada pulsación
        _fichas.drop(1).debounce(300).onEach { jsGuardarLocal(CLAVE_FICHAS, json.encodeToString(serFichas, it)) }.launchIn(scope)
        _propios.drop(1).onEach { jsGuardarLocal(CLAVE_PROPIOS, json.encodeToString(serPropios, it)) }.launchIn(scope)
    }

    /** Un id nuevo, único en todo el almacén (personajes, objetos, notas, conjuros propios...). */
    fun nuevoId(): Long {
        contador += 1
        jsGuardarLocal(CLAVE_CONTADOR, contador.toString())
        return contador
    }

    fun ficha(pid: Long): PersonajeExportado? = _fichas.value[pid]

    fun poner(pid: Long, ficha: PersonajeExportado) = _fichas.update { it + (pid to ficha) }

    fun quitar(pid: Long) = _fichas.update { it - pid }

    /** Cambia la ficha de [pid] (si existe). */
    fun editar(pid: Long, cambio: (PersonajeExportado) -> PersonajeExportado) = _fichas.update { m ->
        val f = m[pid] ?: return@update m
        m + (pid to cambio(f))
    }

    // ── Conjuros ──

    fun todosLosConjuros(): List<Conjuro> = catalogo + _propios.value

    fun conjuro(id: Long): Conjuro? = catalogo.getOrNull((id - 1).toInt())?.takeIf { it.id == id } ?: _propios.value.firstOrNull { it.id == id }

    fun nuevoPropio(c: Conjuro): Long {
        val id = nuevoId()
        _propios.update { it + c.copy(id = id, esPropio = true) }
        return id
    }

    /** El id local de un conjuro que llega de otro dispositivo: por nombre en el catálogo o los propios, o uno nuevo. */
    fun idConjuro(c: Conjuro): Long =
        todosLosConjuros().firstOrNull { it.nombre == c.nombre }?.id ?: nuevoPropio(c)

    /**
     * Prepara una ficha que llega de fuera (otro dispositivo, un archivo) para guardarla como [pid]: ids nuevos para
     * todas sus filas (con sus referencias), conjuros enlazados por nombre, y la foto y fecha de creación locales.
     */
    fun adoptar(e: PersonajeExportado, pid: Long, fotoPath: String?, creado: Long?): PersonajeExportado {
        val companias = e.companias.associate { it.id to nuevoId() }
        val objetos = e.objetos.associate { it.id to nuevoId() }
        val pergaminos = e.conjurosPergaminos.associate { it.id to idConjuro(it) }
        return PersonajeExportado(
            personaje = e.personaje.copy(id = pid, fotoPath = fotoPath, creado = creado ?: e.personaje.creado),
            caracteristicas = (e.caracteristicas ?: Caracteristicas(0)).copy(personajeId = pid),
            estado = (e.estado ?: Estado(0)).copy(personajeId = pid),
            tesoro = (e.tesoro ?: Tesoro(0)).copy(personajeId = pid),
            objetos = e.objetos.map { o ->
                o.copy(
                    id = objetos.getValue(o.id),
                    personajeId = pid,
                    contenedorId = o.contenedorId?.let(objetos::get),
                    companiaId = o.companiaId?.let(companias::get),
                    conjuroId = o.conjuroId?.let(pergaminos::get)
                )
            },
            habilidades = e.habilidades.map { it.copy(id = nuevoId(), personajeId = pid) },
            condiciones = e.condiciones.map { it.copy(id = nuevoId(), personajeId = pid) },
            companias = e.companias.map { it.copy(id = companias.getValue(it.id), personajeId = pid) },
            conjuros = e.conjuros.map { c -> c.copy(conjuro = conjuro(idConjuro(c.conjuro)) ?: c.conjuro) },
            espacios = e.espacios.map { it.copy(personajeId = pid) },
            notas = e.notas.map { it.copy(id = nuevoId(), personajeId = pid) },
            conjurosPergaminos = e.conjurosPergaminos.mapNotNull { c -> pergaminos[c.id]?.let(::conjuro) }
        )
    }

    /** La ficha de [pid] lista para exportar o sincronizar (sin la ruta local del retrato). */
    fun exportable(pid: Long): PersonajeExportado? = ficha(pid)?.let { f ->
        f.copy(
            personaje = f.personaje.copy(fotoPath = null),
            retratoBase64 = null,
            conjurosPergaminos = f.objetos.mapNotNull { it.conjuroId }.distinct().mapNotNull(::conjuro)
        )
    }

    companion object {
        private const val CLAVE_FICHAS = "cronicas.fichas"
        private const val CLAVE_PROPIOS = "cronicas.conjurosPropios"
        private const val CLAVE_CONTADOR = "cronicas.contador"
        /** Por encima de los ids del catálogo. */
        private const val PRIMER_ID = 1_000_000L
    }
}
