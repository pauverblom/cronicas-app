package io.github.pauverblom.cronicas.data

import io.github.pauverblom.cronicas.rules.Monedas
import io.github.pauverblom.cronicas.rules.data.ArticuloDef
import io.github.pauverblom.cronicas.ui.foto.AlmacenRetratos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Versión web del Repository de Android: la misma API, sobre las fichas del [Almacen] en vez de Room.
 * Mantiene la misma semántica (borrar un contenedor lo vacía, las habilidades de reglamento se ocultan, etc.).
 */
class Repository internal constructor(internal val almacen: Almacen) {

    private fun <T> deFicha(pid: Long, f: (PersonajeExportado) -> T): Flow<T?> =
        almacen.fichas.map { it[pid]?.let(f) }.distinctUntilChanged()

    private fun <T> listaDe(pid: Long, f: (PersonajeExportado) -> List<T>): Flow<List<T>> =
        almacen.fichas.map { it[pid]?.let(f).orEmpty() }.distinctUntilChanged()

    // ── Personajes ──
    fun resumenes(): Flow<List<ResumenPersonaje>> = almacen.fichas.map { m ->
        m.values.sortedBy { it.personaje.creado }.map { ResumenPersonaje(it.personaje, it.estado) }
    }.distinctUntilChanged()

    fun personaje(id: Long): Flow<Personaje?> = deFicha(id) { it.personaje }
    suspend fun getPersonaje(id: Long): Personaje? = almacen.ficha(id)?.personaje

    suspend fun crearPersonaje(p: Personaje, c: Caracteristicas, pgMaximos: Int, tesoro: Tesoro): Long {
        val id = almacen.nuevoId()
        almacen.poner(
            id,
            PersonajeExportado(
                personaje = p.copy(id = id),
                caracteristicas = c.copy(personajeId = id),
                estado = Estado(personajeId = id, pgActuales = pgMaximos, pgMaximos = pgMaximos),
                tesoro = tesoro.copy(personajeId = id),
                objetos = emptyList(),
                habilidades = emptyList(),
                condiciones = emptyList(),
                companias = emptyList(),
                conjuros = emptyList(),
                espacios = emptyList(),
                notas = emptyList()
            )
        )
        sincronizarHabilidades(id)
        return id
    }

    suspend fun updatePersonaje(p: Personaje) {
        almacen.editar(p.id) { it.copy(personaje = p) }
        sincronizarHabilidades(p.id)
    }

    suspend fun deletePersonaje(p: Personaje) {
        almacen.quitar(p.id)
        p.fotoPath?.let(AlmacenRetratos::borrar)
    }

    suspend fun sincronizarHabilidades(pid: Long) = almacen.editar(pid) { f ->
        val (sobran, nuevas) = habilidadesDeReglamento(f.personaje, f.habilidades)
        f.copy(habilidades = f.habilidades.filter { h -> sobran.none { it.id == h.id } } + nuevas.map { it.copy(id = almacen.nuevoId()) })
    }

    suspend fun subirNivel(pid: Long, nuevoNivel: Int, pgGanados: Int) {
        almacen.editar(pid) { f ->
            val e = f.estado ?: Estado(pid)
            f.copy(
                personaje = f.personaje.copy(nivel = nuevoNivel),
                estado = e.copy(pgMaximos = e.pgMaximos + pgGanados, pgActuales = e.pgActuales + pgGanados)
            )
        }
        sincronizarHabilidades(pid)
    }

    // ── Características y estado ──
    fun caracteristicas(pid: Long): Flow<Caracteristicas?> = deFicha(pid) { it.caracteristicas }
    suspend fun upsertCaracteristicas(c: Caracteristicas) = almacen.editar(c.personajeId) { it.copy(caracteristicas = c) }
    fun estado(pid: Long): Flow<Estado?> = deFicha(pid) { it.estado }
    suspend fun getEstado(pid: Long): Estado? = almacen.ficha(pid)?.estado
    suspend fun upsertEstado(e: Estado) = almacen.editar(e.personajeId) { it.copy(estado = e) }

    // ── Objetos ──
    fun objetos(pid: Long): Flow<List<Objeto>> = listaDe(pid) { f -> f.objetos.sortedBy { it.id } }

    suspend fun insertObjeto(o: Objeto): Long {
        val id = almacen.nuevoId()
        almacen.editar(o.personajeId) { it.copy(objetos = it.objetos + o.copy(id = id)) }
        return id
    }

    suspend fun updateObjeto(o: Objeto) = almacen.editar(o.personajeId) { f ->
        f.copy(objetos = f.objetos.map { if (it.id == o.id) o else it })
    }

    suspend fun deleteObjeto(o: Objeto) = almacen.editar(o.personajeId) { f ->
        f.copy(objetos = f.objetos.filter { it.id != o.id }.map { if (it.contenedorId == o.id) it.copy(contenedorId = null) else it })
    }

    suspend fun anadirDeCatalogo(pid: Long, art: ArticuloDef, lotes: Int, comprar: Boolean): Boolean {
        val f = almacen.ficha(pid) ?: return false
        var tesoro = f.tesoro ?: Tesoro(pid)
        if (comprar) {
            val pagado = Monedas.pagar(tesoro.bolsa(), art.precioMc * lotes) ?: return false
            tesoro = tesoro.conBolsa(pagado)
        }
        val o = objetoDeCatalogo(pid, art, lotes).copy(id = almacen.nuevoId())
        almacen.editar(pid) { it.copy(tesoro = tesoro, objetos = it.objetos + o) }
        return true
    }

    // ── Tesoro ──
    fun tesoro(pid: Long): Flow<Tesoro?> = deFicha(pid) { it.tesoro }
    suspend fun upsertTesoro(t: Tesoro) = almacen.editar(t.personajeId) { it.copy(tesoro = t) }

    // ── Habilidades, condiciones, compañía ──
    fun habilidades(pid: Long): Flow<List<Habilidad>> = listaDe(pid) { f -> f.habilidades.sortedBy { it.id } }

    suspend fun insertHabilidad(h: Habilidad): Long {
        val id = almacen.nuevoId()
        almacen.editar(h.personajeId) { it.copy(habilidades = it.habilidades + h.copy(id = id)) }
        return id
    }

    suspend fun updateHabilidad(h: Habilidad) = almacen.editar(h.personajeId) { f ->
        f.copy(habilidades = f.habilidades.map { if (it.id == h.id) h else it })
    }

    /** Las de reglamento se ocultan (para que no vuelvan al subir de nivel); las propias se borran. */
    suspend fun deleteHabilidad(h: Habilidad) =
        if (h.origen == Habilidad.ORIGEN_PROPIA) {
            almacen.editar(h.personajeId) { f -> f.copy(habilidades = f.habilidades.filter { it.id != h.id }) }
        } else updateHabilidad(h.copy(oculta = true))

    fun condiciones(pid: Long): Flow<List<Condicion>> = listaDe(pid) { f -> f.condiciones.sortedBy { it.id } }

    suspend fun insertCondicion(c: Condicion): Long {
        val id = almacen.nuevoId()
        almacen.editar(c.personajeId) { it.copy(condiciones = it.condiciones + c.copy(id = id)) }
        return id
    }

    suspend fun updateCondicion(c: Condicion) = almacen.editar(c.personajeId) { f ->
        f.copy(condiciones = f.condiciones.map { if (it.id == c.id) c else it })
    }

    suspend fun deleteCondicion(c: Condicion) = almacen.editar(c.personajeId) { f ->
        f.copy(condiciones = f.condiciones.filter { it.id != c.id })
    }

    fun companias(pid: Long): Flow<List<Compania>> = listaDe(pid) { f -> f.companias.sortedBy { it.id } }

    suspend fun insertCompania(c: Compania): Long {
        val id = almacen.nuevoId()
        almacen.editar(c.personajeId) { it.copy(companias = it.companias + c.copy(id = id)) }
        return id
    }

    suspend fun updateCompania(c: Compania) = almacen.editar(c.personajeId) { f ->
        f.copy(companias = f.companias.map { if (it.id == c.id) c else it })
    }

    suspend fun deleteCompania(c: Compania) = almacen.editar(c.personajeId) { f ->
        f.copy(
            companias = f.companias.filter { it.id != c.id },
            objetos = f.objetos.map { if (it.companiaId == c.id) it.copy(companiaId = null) else it }
        )
    }

    // ── Conjuros ──
    fun conjuros(): Flow<List<Conjuro>> = almacen.propios.map { (almacen.catalogo + it).sortedBy { c -> c.nombre.lowercase() } }
    suspend fun countConjurosCatalogo(): Int = almacen.catalogo.size

    suspend fun insertConjuros(list: List<Conjuro>) {
        list.forEach { almacen.nuevoPropio(it) }
    }

    suspend fun insertConjuro(c: Conjuro): Long = almacen.nuevoPropio(c)

    fun conjurosDe(pid: Long): Flow<List<ConjuroConDetalle>> = listaDe(pid) { f ->
        f.conjuros.map { c ->
            val cj = c.conjuro
            ConjuroConDetalle(
                ConjuroPersonaje(pid, cj.id, c.nivel, c.preparadoVeces, c.usadoVeces),
                ConjuroPlano(cj.id, cj.nombre, cj.niveles, cj.tl, cj.componentes, cj.alcance, cj.duracion, cj.ts, cj.rc, cj.descripcion, cj.esPropio)
            )
        }.sortedWith(compareBy({ it.cp.nivel }, { it.conjuro.nombre.lowercase() }))
    }

    /** Como el INSERT OR IGNORE de Android: si ya lo conoce, no cambia nada. */
    suspend fun aprenderConjuro(pid: Long, cid: Long, nivel: Int) {
        val c = almacen.conjuro(cid) ?: return
        almacen.editar(pid) { f ->
            if (f.conjuros.any { it.conjuro.id == cid }) f else f.copy(conjuros = f.conjuros + ConjuroExportado(c, nivel, 0, 0))
        }
    }

    suspend fun olvidarConjuro(pid: Long, cid: Long) = almacen.editar(pid) { f ->
        f.copy(conjuros = f.conjuros.filter { it.conjuro.id != cid })
    }

    suspend fun updateConjuroPersonaje(cp: ConjuroPersonaje) = almacen.editar(cp.personajeId) { f ->
        f.copy(conjuros = f.conjuros.map {
            if (it.conjuro.id == cp.conjuroId) it.copy(nivel = cp.nivel, preparadoVeces = cp.preparadoVeces, usadoVeces = cp.usadoVeces) else it
        })
    }

    suspend fun descansar(pid: Long) = almacen.editar(pid) { f -> f.copy(conjuros = f.conjuros.map { it.copy(usadoVeces = 0) }) }

    fun espaciosForzados(pid: Long): Flow<List<EspacioOverride>> = listaDe(pid) { it.espacios }

    suspend fun forzarEspacios(pid: Long, nivel: Int, total: Int?) = almacen.editar(pid) { f ->
        val resto = f.espacios.filter { it.nivel != nivel }
        f.copy(espacios = if (total == null) resto else resto + EspacioOverride(pid, nivel, total))
    }

    suspend fun importConjurosFromCsv(csvContent: String): Int {
        val conjuros = conjurosDesdeCsv(csvContent)
        insertConjuros(conjuros)
        return conjuros.size
    }

    // ── Notas ──
    fun notas(pid: Long): Flow<List<Nota>> = listaDe(pid) { f -> f.notas.sortedByDescending { it.timestamp } }

    suspend fun insertNota(n: Nota): Long {
        val id = almacen.nuevoId()
        almacen.editar(n.personajeId) { it.copy(notas = it.notas + n.copy(id = id)) }
        return id
    }

    suspend fun updateNota(n: Nota) = almacen.editar(n.personajeId) { f -> f.copy(notas = f.notas.map { if (it.id == n.id) n else it }) }

    suspend fun deleteNota(id: Long) {
        val pid = almacen.fichas.value.entries.firstOrNull { (_, f) -> f.notas.any { it.id == id } }?.key ?: return
        almacen.editar(pid) { f -> f.copy(notas = f.notas.filter { it.id != id }) }
    }

    companion object {
        fun objetoDesde(pid: Long, art: ArticuloDef, lotes: Int = 1) = objetoDeCatalogo(pid, art, lotes)
    }
}
