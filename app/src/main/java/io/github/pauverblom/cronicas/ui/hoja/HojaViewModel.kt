package io.github.pauverblom.cronicas.ui.hoja

import io.github.pauverblom.cronicas.data.Tema
import io.github.pauverblom.cronicas.data.ColoresTema
import io.github.pauverblom.cronicas.data.conRetrato
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import io.github.pauverblom.cronicas.CronicasApp
import io.github.pauverblom.cronicas.data.Backup
import io.github.pauverblom.cronicas.data.Caracteristicas
import io.github.pauverblom.cronicas.data.Compania
import io.github.pauverblom.cronicas.data.Condicion
import io.github.pauverblom.cronicas.data.Conjuro
import io.github.pauverblom.cronicas.data.ConjuroConDetalle
import io.github.pauverblom.cronicas.data.ConjuroPersonaje
import io.github.pauverblom.cronicas.data.EspacioOverride
import io.github.pauverblom.cronicas.data.Estado
import io.github.pauverblom.cronicas.data.Habilidad
import io.github.pauverblom.cronicas.data.Nota
import io.github.pauverblom.cronicas.data.Objeto
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.Tesoro
import io.github.pauverblom.cronicas.rules.Hoja
import io.github.pauverblom.cronicas.rules.data.ArticuloDef
import io.github.pauverblom.cronicas.rules.data.Categoria
import io.github.pauverblom.cronicas.ui.RutaHoja
import io.github.pauverblom.cronicas.ui.foto.Retratos
import io.github.pauverblom.cronicas.ui.plataforma.Archivo
import io.github.pauverblom.cronicas.ui.plataforma.ahora
import io.github.pauverblom.cronicas.ui.plataforma.escribirArchivo
import io.github.pauverblom.cronicas.ui.plataforma.leerArchivo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class Base(val p: Personaje?, val c: Caracteristicas?, val e: Estado?, val o: List<Objeto>, val t: Tesoro?)
private data class Extra(val h: List<Habilidad>, val cond: List<Condicion>, val comp: List<Compania>)
private data class Magia(val cj: List<ConjuroConDetalle>, val esp: List<EspacioOverride>, val notas: List<Nota>)

class HojaViewModel(app: Application, savedState: SavedStateHandle) : AndroidViewModel(app) {
    private val repo = (app as CronicasApp).repository
    private val ruta = savedState.toRoute<RutaHoja>()
    val pid: Long = ruta.personajeId
    /** Nombre, retrato y clase que trae la ruta, para la cabecera mientras se carga la ficha. */
    val previa = Personaje(id = pid, nombre = ruta.nombre, fotoPath = ruta.foto).let { p -> ruta.claseId?.let { p.copy(claseId = it) } ?: p }

    private val base: Flow<Base> = combine(
        repo.personaje(pid), repo.caracteristicas(pid), repo.estado(pid), repo.objetos(pid), repo.tesoro(pid), ::Base
    )
    private val extra: Flow<Extra> = combine(repo.habilidades(pid), repo.condiciones(pid), repo.companias(pid), ::Extra)
    private val magia: Flow<Magia> = combine(repo.conjurosDe(pid), repo.espaciosForzados(pid), repo.notas(pid), ::Magia)

    /** null mientras carga; si el personaje se borra, [borrado] pasa a true. */
    val ui = combine(base, extra, magia) { b, x, m ->
        val p = b.p ?: return@combine null
        val c = b.c ?: Caracteristicas(pid)
        val e = b.e ?: Estado(pid)
        val t = b.t ?: Tesoro(pid)
        HojaUi(
            p, c, e, b.o, t, x.h.filter { !it.oculta }, x.cond, x.comp, m.cj, m.esp, m.notas,
            Hoja.calcular(p, c, e, b.o, t, x.comp, m.esp)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val catalogoConjuros = repo.conjuros().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun lanzar(block: suspend () -> Unit) = viewModelScope.launch { block() }

    // ── Personaje ──
    fun actualizarPersonaje(f: (Personaje) -> Personaje) = lanzar {
        repo.getPersonaje(pid)?.let { repo.updatePersonaje(f(it)) }
    }

    fun setRetrato(archivo: Archivo) = lanzar {
        val p = repo.getPersonaje(pid) ?: return@lanzar
        val (path, colores) = runCatching { Retratos.guardar(getApplication(), pid, archivo, p.fotoPath) }.getOrNull() ?: return@lanzar
        repo.updatePersonaje(p.conRetrato(path, colores))
    }

    /** Tema propio de la ficha ([tema] null: el de la app); [colores] solo para el personalizado. */
    fun setTema(tema: Tema?, colores: ColoresTema?) = lanzar {
        val p = repo.getPersonaje(pid) ?: return@lanzar
        repo.updatePersonaje(p.copy(tema = tema?.name, colorAcento = colores?.acento, colorMarco = colores?.marco, colorFondo = colores?.fondo))
    }

    fun quitarRetrato() = lanzar {
        val p = repo.getPersonaje(pid) ?: return@lanzar
        p.fotoPath?.let { Retratos.borrar(it) }
        repo.updatePersonaje(p.conRetrato(null, null))
    }

    fun subirNivel(nuevoNivel: Int, pgGanados: Int) = lanzar { repo.subirNivel(pid, nuevoNivel, pgGanados) }

    fun eliminar(onFin: () -> Unit) = lanzar {
        repo.getPersonaje(pid)?.let { repo.deletePersonaje(it) }
        onFin()
    }

    fun exportar(archivo: Archivo, onFin: (Boolean) -> Unit) = lanzar {
        val ok = runCatching {
            val json = Backup.exportar(repo, pid)
            escribirArchivo(getApplication(), archivo, json.encodeToByteArray())
        }.isSuccess
        onFin(ok)
    }

    // ── Características y estado ──
    fun actualizarCaracteristicas(f: (Caracteristicas) -> Caracteristicas) = lanzar {
        val actual = ui.value?.caracteristicas ?: Caracteristicas(pid)
        repo.upsertCaracteristicas(f(actual))
    }

    fun actualizarEstado(f: (Estado) -> Estado) = lanzar {
        val actual = repo.getEstado(pid) ?: Estado(pid)
        repo.upsertEstado(f(actual))
    }

    /** Daño o curación normal. Al curar se recupera antes el daño normal y después el atenuado. */
    fun cambiarPg(delta: Int) = actualizarEstado {
        val pg = (it.pgActuales + delta).coerceIn(0, it.pgMaximos)
        it.copy(pgActuales = pg, danoAtenuado = it.danoAtenuado.coerceAtMost(it.pgMaximos - pg))
    }

    /** Un punto de daño atenuado: resta PG como el normal, pero se anota aparte. */
    fun golpeAtenuado() = actualizarEstado {
        if (it.pgActuales <= 0) it else it.copy(pgActuales = it.pgActuales - 1, danoAtenuado = it.danoAtenuado + 1)
    }

    /** Pasa un minuto: el daño atenuado devuelve 1 PG. */
    fun recuperarAtenuado() = actualizarEstado {
        if (it.danoAtenuado <= 0) it else it.copy(pgActuales = (it.pgActuales + 1).coerceAtMost(it.pgMaximos), danoAtenuado = it.danoAtenuado - 1)
    }

    fun setPgMax(max: Int) = actualizarEstado {
        val pg = it.pgActuales.coerceAtMost(max)
        it.copy(pgMaximos = max, pgActuales = pg, danoAtenuado = it.danoAtenuado.coerceIn(0, (max - pg).coerceAtLeast(0)))
    }

    // ── Objetos ──
    fun guardarObjeto(o: Objeto) = lanzar {
        if (o.id == 0L) repo.insertObjeto(o.copy(personajeId = pid)) else repo.updateObjeto(o)
        if (o.equipado) desequiparOtros(o)
    }

    fun eliminarObjeto(o: Objeto) = lanzar { repo.deleteObjeto(o) }

    /** El pergamino se consume al leerlo, salga bien o mal la tirada. */
    fun gastarPergamino(o: Objeto) = lanzar {
        if (o.cantidad > 1) repo.updateObjeto(o.copy(cantidad = o.cantidad - 1)) else repo.deleteObjeto(o)
    }

    /** Solo se puede llevar puesta una armadura, un escudo y un casco a la vez. */
    fun alternarEquipado(o: Objeto) = lanzar {
        val nuevo = o.copy(equipado = !o.equipado, companiaId = if (!o.equipado) null else o.companiaId, contenedorId = if (!o.equipado) null else o.contenedorId)
        repo.updateObjeto(nuevo)
        if (nuevo.equipado) desequiparOtros(nuevo)
    }

    private suspend fun desequiparOtros(o: Objeto) {
        val cat = Categoria.of(o.categoria)
        if (cat !in setOf(Categoria.ARMADURA, Categoria.ESCUDO, Categoria.CASCO)) return
        repo.objetos(pid).first()
            .filter { it.id != o.id && it.equipado && Categoria.of(it.categoria) == cat }
            .forEach { repo.updateObjeto(it.copy(equipado = false)) }
    }

    fun anadirDeCatalogo(art: ArticuloDef, lotes: Int, comprar: Boolean, onFin: (Boolean) -> Unit) = lanzar {
        onFin(repo.anadirDeCatalogo(pid, art, lotes, comprar))
    }

    fun guardarTesoro(t: Tesoro) = lanzar { repo.upsertTesoro(t.copy(personajeId = pid)) }

    // ── Habilidades, condiciones, compañía ──
    fun guardarHabilidad(h: Habilidad) = lanzar {
        if (h.id == 0L) repo.insertHabilidad(h.copy(personajeId = pid)) else repo.updateHabilidad(h)
    }

    fun eliminarHabilidad(h: Habilidad) = lanzar { repo.deleteHabilidad(h) }

    fun guardarCondicion(c: Condicion) = lanzar {
        if (c.id == 0L) repo.insertCondicion(c.copy(personajeId = pid)) else repo.updateCondicion(c)
    }

    fun eliminarCondicion(c: Condicion) = lanzar { repo.deleteCondicion(c) }

    fun guardarCompania(c: Compania) = lanzar {
        if (c.id == 0L) repo.insertCompania(c.copy(personajeId = pid)) else repo.updateCompania(c)
    }

    fun eliminarCompania(c: Compania) = lanzar { repo.deleteCompania(c) }

    // ── Conjuros ──
    fun aprender(c: Conjuro, nivel: Int) = lanzar { repo.aprenderConjuro(pid, c.id, nivel) }
    fun olvidar(cid: Long) = lanzar { repo.olvidarConjuro(pid, cid) }

    fun preparar(cp: ConjuroPersonaje, delta: Int) = lanzar {
        val veces = (cp.preparadoVeces + delta).coerceAtLeast(0)
        repo.updateConjuroPersonaje(cp.copy(preparadoVeces = veces, usadoVeces = cp.usadoVeces.coerceAtMost(veces)))
    }

    fun setUsados(cp: ConjuroPersonaje, usados: Int) = lanzar {
        repo.updateConjuroPersonaje(cp.copy(usadoVeces = usados.coerceIn(0, cp.preparadoVeces)))
    }

    fun descansar() = lanzar { repo.descansar(pid) }
    fun forzarEspacios(nivel: Int, total: Int?) = lanzar { repo.forzarEspacios(pid, nivel, total) }

    fun crearConjuro(c: Conjuro, nivel: Int) = lanzar {
        val id = repo.insertConjuro(c.copy(esPropio = true))
        repo.aprenderConjuro(pid, id, nivel)
    }

    /** Conjuro propio que no se aprende, p. ej. el de un pergamino que da el Narrador. */
    fun crearConjuroPropio(c: Conjuro, onFin: (Long) -> Unit) = lanzar { onFin(repo.insertConjuro(c.copy(esPropio = true))) }

    fun importarCsv(archivo: Archivo, onFin: (Int) -> Unit) = lanzar {
        val n = runCatching {
            val texto = leerArchivo(getApplication(), archivo).decodeToString()
            repo.importConjurosFromCsv(texto)
        }.getOrDefault(0)
        onFin(n)
    }

    // ── Notas ──
    fun guardarNota(n: Nota) = lanzar {
        if (n.id == 0L) repo.insertNota(n.copy(personajeId = pid)) else repo.updateNota(n.copy(timestamp = ahora()))
    }

    fun eliminarNota(id: Long) = lanzar { repo.deleteNota(id) }
}
