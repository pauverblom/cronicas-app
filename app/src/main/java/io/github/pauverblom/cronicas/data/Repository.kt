package io.github.pauverblom.cronicas.data

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class Repository(private val db: AppDatabase) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // ── Personaje ──
    fun allPersonajes(): Flow<List<Personaje>> = db.personajeDao().getAll()
    suspend fun getPersonaje(id: Long): Personaje? = db.personajeDao().getById(id)
    suspend fun insertPersonaje(p: Personaje): Long = db.personajeDao().insert(p)
    suspend fun updatePersonaje(p: Personaje) = db.personajeDao().update(p)
    suspend fun deletePersonaje(p: Personaje) = db.personajeDao().delete(p)
    suspend fun personajeCount(): Int = db.personajeDao().count()

    // ── Características ──
    fun caracteristicas(pid: Long): Flow<Caracteristicas?> = db.caracteristicasDao().getByPersonaje(pid)
    suspend fun caracteristicasSync(pid: Long): Caracteristicas? = db.caracteristicasDao().getByPersonajeSync(pid)
    suspend fun upsertCaracteristicas(c: Caracteristicas) = db.caracteristicasDao().upsert(c)

    // ── Combate ──
    fun estadoCombate(pid: Long): Flow<EstadoCombate?> = db.estadoCombateDao().getByPersonaje(pid)
    suspend fun estadoCombateSync(pid: Long): EstadoCombate? = db.estadoCombateDao().getByPersonajeSync(pid)
    suspend fun upsertEstadoCombate(e: EstadoCombate) = db.estadoCombateDao().upsert(e)
    suspend fun updatePg(pid: Long, pg: Int) = db.estadoCombateDao().updatePg(pid, pg)

    // ── Armas ──
    fun armas(pid: Long): Flow<List<Arma>> = db.armaDao().getByPersonaje(pid)
    suspend fun insertArma(a: Arma): Long = db.armaDao().insert(a)

    // ── Conjuros ──
    fun allConjuros(): Flow<List<Conjuro>> = db.conjuroDao().getAll()
    suspend fun insertConjuro(c: Conjuro): Long = db.conjuroDao().insert(c)
    suspend fun insertConjuros(list: List<Conjuro>): List<Long> = db.conjuroDao().insertAll(list)
    suspend fun deleteNonOwnConjuros() = db.conjuroDao().deleteNonOwn()

    fun conjurosPersonaje(pid: Long): Flow<List<ConjuroConDetalle>> = db.conjuroPersonajeDao().getWithConjuro(pid)
    suspend fun getConjuroPersonaje(pid: Long, cid: Long): ConjuroPersonaje? = db.conjuroPersonajeDao().get(pid, cid)
    suspend fun upsertConjuroPersonaje(cp: ConjuroPersonaje) = db.conjuroPersonajeDao().upsert(cp)
    suspend fun gastarConjuro(pid: Long, cid: Long): Int = db.conjuroPersonajeDao().gastar(pid, cid)
    suspend fun resetConjurosGastados(pid: Long) = db.conjuroPersonajeDao().resetGastados(pid)
    suspend fun updatePreparado(pid: Long, cid: Long, veces: Int) = db.conjuroPersonajeDao().updatePreparado(pid, cid, veces)
    suspend fun deleteConjuroPersonaje(pid: Long, cid: Long) = db.conjuroPersonajeDao().deleteByIds(pid, cid)
    suspend fun allConjuroPersonaje(pid: Long): List<ConjuroPersonaje> = db.conjuroPersonajeDao().getAllForPersonaje(pid)

    fun espaciosConjuro(pid: Long): Flow<List<EspaciosConjuro>> = db.espaciosConjuroDao().getByPersonaje(pid)
    suspend fun espaciosConjuroSync(pid: Long): List<EspaciosConjuro> = db.espaciosConjuroDao().getByPersonajeSync(pid)
    suspend fun upsertEspaciosConjuro(e: EspaciosConjuro) = db.espaciosConjuroDao().upsert(e)

    // ── Objetos ──
    fun objetos(pid: Long): Flow<List<Objeto>> = db.objetoDao().getByPersonaje(pid)
    suspend fun insertObjeto(o: Objeto): Long = db.objetoDao().insert(o)
    suspend fun updateObjeto(o: Objeto) = db.objetoDao().update(o)
    suspend fun deleteObjeto(o: Objeto) = db.objetoDao().delete(o)

    // ── Tesoro ──
    fun tesoro(pid: Long): Flow<Tesoro?> = db.tesoroDao().getByPersonaje(pid)
    suspend fun tesoroSync(pid: Long): Tesoro? = db.tesoroDao().getByPersonajeSync(pid)
    suspend fun upsertTesoro(t: Tesoro) = db.tesoroDao().upsert(t)

    // ── Efectos activos ──
    fun efectosActivos(pid: Long): Flow<List<EfectoActivo>> = db.efectoActivoDao().getByPersonaje(pid)
    suspend fun insertEfecto(e: EfectoActivo): Long = db.efectoActivoDao().insert(e)
    suspend fun deleteEfecto(id: Long) = db.efectoActivoDao().deleteById(id)

    // ── Notas ──
    fun notas(pid: Long): Flow<List<Nota>> = db.notaDao().getByPersonaje(pid)
    suspend fun insertNota(n: Nota): Long = db.notaDao().insert(n)
    suspend fun updateNota(n: Nota) = db.notaDao().update(n)
    suspend fun deleteNota(id: Long) = db.notaDao().deleteById(id)

    suspend fun updateDanoAtenuado(pid: Long, da: Int) = db.estadoCombateDao().updateDanoAtenuado(pid, da)

    // ── Deshacer ──
    suspend fun pushUndo(a: AccionUndo) {
        db.accionUndoDao().insert(a)
        db.accionUndoDao().trimOld(a.personajeId)
    }

    suspend fun popUndo(pid: Long): AccionUndo? {
        val last = db.accionUndoDao().getLastAction(pid) ?: return null
        db.accionUndoDao().delete(last.id)
        return last
    }

    // ── Importación CSV ──
    /**
     * Columnas esperadas (con cabecera):
     * nombre, nivel, tl, componentes, alcance, duracion, ts, rc, descripcion
     */
    suspend fun importConjurosFromCsv(csvContent: String): Int {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return 0

        val conjuros = mutableListOf<Conjuro>()
        for (line in lines.drop(1)) {
            val fields = parseCsvLine(line)
            if (fields.size < 2) continue
            conjuros.add(
                Conjuro(
                    nombre = fields.getOrElse(0) { "" }.trim(),
                    nivel = fields.getOrElse(1) { "0" }.trim().toIntOrNull() ?: 0,
                    tl = fields.getOrElse(2) { "1" }.trim().toIntOrNull() ?: 1,
                    componentes = fields.getOrElse(3) { "" }.trim(),
                    alcance = fields.getOrElse(4) { "" }.trim(),
                    duracion = fields.getOrElse(5) { "" }.trim(),
                    ts = fields.getOrElse(6) { "" }.trim(),
                    rc = fields.getOrElse(7) { "" }.trim(),
                    descripcion = fields.getOrElse(8) { "" }.trim(),
                    esPropio = false
                )
            )
        }
        if (conjuros.isNotEmpty()) {
            db.conjuroDao().insertAll(conjuros)
        }
        return conjuros.size
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }
}
