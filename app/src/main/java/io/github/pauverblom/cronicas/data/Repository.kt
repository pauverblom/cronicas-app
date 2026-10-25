package io.github.pauverblom.cronicas.data

import androidx.room.withTransaction
import io.github.pauverblom.cronicas.rules.Monedas
import io.github.pauverblom.cronicas.rules.data.ArticuloDef
import io.github.pauverblom.cronicas.rules.data.Clases
import io.github.pauverblom.cronicas.rules.data.Especies
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.File

/** Lo que muestra cada tarjeta de la pantalla de inicio. */
data class ResumenPersonaje(val personaje: Personaje, val estado: Estado?)

class Repository(private val db: AppDatabase) {

    // ── Personajes ──
    fun resumenes(): Flow<List<ResumenPersonaje>> =
        combine(db.personajeDao().getAll(), db.estadoDao().getAll()) { ps, es ->
            val porId = es.associateBy { it.personajeId }
            ps.map { ResumenPersonaje(it, porId[it.id]) }
        }

    fun personaje(id: Long): Flow<Personaje?> = db.personajeDao().observe(id)
    suspend fun getPersonaje(id: Long): Personaje? = db.personajeDao().getById(id)

    /** Crea el personaje con todas sus filas y las habilidades de su clase y especie. */
    suspend fun crearPersonaje(
        p: Personaje,
        c: Caracteristicas,
        pgMaximos: Int,
        tesoro: Tesoro
    ): Long = db.withTransaction {
        val id = db.personajeDao().insert(p)
        db.caracteristicasDao().upsert(c.copy(personajeId = id))
        db.estadoDao().upsert(Estado(personajeId = id, pgActuales = pgMaximos, pgMaximos = pgMaximos))
        db.tesoroDao().upsert(tesoro.copy(personajeId = id))
        sincronizarHabilidades(id)
        id
    }

    suspend fun updatePersonaje(p: Personaje) = db.withTransaction {
        db.personajeDao().update(p)
        sincronizarHabilidades(p.id)
    }

    suspend fun deletePersonaje(p: Personaje) {
        db.personajeDao().delete(p.id)
        p.fotoPath?.let { File(it).delete() }
    }

    /**
     * Añade las habilidades de reglamento que faltan para la clase, la secundaria y la especie al nivel actual,
     * y quita las de reglamento que ya no corresponden (p. ej. al cambiar de clase).
     */
    suspend fun sincronizarHabilidades(pid: Long) {
        val p = db.personajeDao().getById(pid) ?: return
        val clase = Clases.porId(p.claseId)
        val secundaria = Clases.porId(p.claseSecundariaId)
        val especie = Especies.porId(p.especieId)
        val nivelSec = maxOf(1, p.nivel / 2)
        val deClase = (clase?.habilidades.orEmpty().filter { it.nivel <= p.nivel } +
            secundaria?.habilidades.orEmpty().filter { it.nivel <= nivelSec }).distinctBy { it.nombre }
        val deEspecie = especie?.rasgos.orEmpty()
        val todasClase = (clase?.habilidades.orEmpty() + secundaria?.habilidades.orEmpty()).map { it.nombre }.toSet()

        val actuales = db.habilidadDao().getAll(pid)
        for (h in actuales) {
            val sobra = when (h.origen) {
                Habilidad.ORIGEN_CLASE -> h.nombre !in todasClase
                Habilidad.ORIGEN_ESPECIE -> deEspecie.none { it.nombre == h.nombre }
                else -> false
            }
            if (sobra) db.habilidadDao().delete(h)
        }
        val existentes = actuales.map { it.origen to it.nombre }.toSet()
        val nuevas = deClase.filter { (Habilidad.ORIGEN_CLASE to it.nombre) !in existentes }
            .map { Habilidad(personajeId = pid, origen = Habilidad.ORIGEN_CLASE, nombre = it.nombre, atributo = it.atributo?.abrev, descripcion = it.descripcion) } +
            deEspecie.filter { (Habilidad.ORIGEN_ESPECIE to it.nombre) !in existentes }
                .map { Habilidad(personajeId = pid, origen = Habilidad.ORIGEN_ESPECIE, nombre = it.nombre, atributo = it.atributo?.abrev, descripcion = it.descripcion) }
        if (nuevas.isNotEmpty()) db.habilidadDao().insertAll(nuevas)
    }

    /** Sube (o fija) el nivel y suma los PG ganados a los máximos y a los actuales. */
    suspend fun subirNivel(pid: Long, nuevoNivel: Int, pgGanados: Int) = db.withTransaction {
        val p = db.personajeDao().getById(pid) ?: return@withTransaction
        db.personajeDao().update(p.copy(nivel = nuevoNivel))
        val e = db.estadoDao().get(pid) ?: Estado(pid)
        db.estadoDao().upsert(e.copy(pgMaximos = e.pgMaximos + pgGanados, pgActuales = e.pgActuales + pgGanados))
        sincronizarHabilidades(pid)
    }

    // ── Características y estado ──
    fun caracteristicas(pid: Long): Flow<Caracteristicas?> = db.caracteristicasDao().observe(pid)
    suspend fun upsertCaracteristicas(c: Caracteristicas) = db.caracteristicasDao().upsert(c)
    fun estado(pid: Long): Flow<Estado?> = db.estadoDao().observe(pid)
    suspend fun getEstado(pid: Long): Estado? = db.estadoDao().get(pid)
    suspend fun upsertEstado(e: Estado) = db.estadoDao().upsert(e)

    // ── Objetos ──
    fun objetos(pid: Long): Flow<List<Objeto>> = db.objetoDao().observe(pid)
    suspend fun insertObjeto(o: Objeto): Long = db.objetoDao().insert(o)
    suspend fun updateObjeto(o: Objeto) = db.objetoDao().update(o)

    suspend fun deleteObjeto(o: Objeto) = db.withTransaction {
        db.objetoDao().vaciarContenedor(o.id)
        db.objetoDao().delete(o)
    }

    /**
     * Añade un artículo del catálogo. Si [comprar], descuenta el precio de las monedas;
     * devuelve false si no hay dinero suficiente (y entonces no añade nada).
     */
    suspend fun anadirDeCatalogo(pid: Long, art: ArticuloDef, lotes: Int, comprar: Boolean): Boolean = db.withTransaction {
        if (comprar) {
            val t = db.tesoroDao().get(pid) ?: Tesoro(pid)
            val pagado = Monedas.pagar(t.bolsa(), art.precioMc * lotes) ?: return@withTransaction false
            db.tesoroDao().upsert(t.conBolsa(pagado))
        }
        db.objetoDao().insert(objetoDesde(pid, art, lotes))
        true
    }

    // ── Tesoro ──
    fun tesoro(pid: Long): Flow<Tesoro?> = db.tesoroDao().observe(pid)
    suspend fun upsertTesoro(t: Tesoro) = db.tesoroDao().upsert(t)

    // ── Habilidades, condiciones, compañía ──
    fun habilidades(pid: Long): Flow<List<Habilidad>> = db.habilidadDao().observe(pid)
    suspend fun insertHabilidad(h: Habilidad) = db.habilidadDao().insert(h)
    suspend fun updateHabilidad(h: Habilidad) = db.habilidadDao().update(h)

    /** Las de reglamento se ocultan (para que no vuelvan al subir de nivel); las propias se borran. */
    suspend fun deleteHabilidad(h: Habilidad) =
        if (h.origen == Habilidad.ORIGEN_PROPIA) db.habilidadDao().delete(h) else db.habilidadDao().update(h.copy(oculta = true))

    fun condiciones(pid: Long): Flow<List<Condicion>> = db.condicionDao().observe(pid)
    suspend fun insertCondicion(c: Condicion) = db.condicionDao().insert(c)
    suspend fun updateCondicion(c: Condicion) = db.condicionDao().update(c)
    suspend fun deleteCondicion(c: Condicion) = db.condicionDao().delete(c)

    fun companias(pid: Long): Flow<List<Compania>> = db.companiaDao().observe(pid)
    suspend fun insertCompania(c: Compania) = db.companiaDao().insert(c)
    suspend fun updateCompania(c: Compania) = db.companiaDao().update(c)

    suspend fun deleteCompania(c: Compania) = db.withTransaction {
        db.objetoDao().descargarCompania(c.id)
        db.companiaDao().delete(c)
    }

    // ── Conjuros ──
    fun conjuros(): Flow<List<Conjuro>> = db.conjuroDao().getAll()
    suspend fun countConjurosCatalogo(): Int = db.conjuroDao().countCatalogo()
    suspend fun insertConjuros(list: List<Conjuro>) = db.conjuroDao().insertAll(list)
    suspend fun insertConjuro(c: Conjuro): Long = db.conjuroDao().insert(c)
    fun conjurosDe(pid: Long): Flow<List<ConjuroConDetalle>> = db.conjuroDao().observeDePersonaje(pid)

    suspend fun aprenderConjuro(pid: Long, cid: Long, nivel: Int) =
        db.conjuroDao().insertDePersonaje(ConjuroPersonaje(pid, cid, nivel))

    suspend fun olvidarConjuro(pid: Long, cid: Long) = db.conjuroDao().deleteDePersonaje(pid, cid)
    suspend fun updateConjuroPersonaje(cp: ConjuroPersonaje) = db.conjuroDao().updateDePersonaje(cp)
    suspend fun descansar(pid: Long) = db.conjuroDao().descansar(pid)

    fun espaciosForzados(pid: Long): Flow<List<EspacioOverride>> = db.conjuroDao().observeEspacios(pid)
    suspend fun forzarEspacios(pid: Long, nivel: Int, total: Int?) =
        if (total == null) db.conjuroDao().deleteEspacio(pid, nivel) else db.conjuroDao().upsertEspacio(EspacioOverride(pid, nivel, total))

    /**
     * Columnas (con cabecera): nombre, clases, tl, componentes, alcance, duracion, ts, rc, descripcion.
     * "clases" usa el formato "mago=1;clerigo=2".
     */
    suspend fun importConjurosFromCsv(csvContent: String): Int {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return 0
        val conjuros = lines.drop(1).mapNotNull { line ->
            val f = parseCsvLine(line).map { it.trim() }
            if (f.size < 2 || f[0].isBlank()) return@mapNotNull null
            Conjuro(
                nombre = f[0],
                niveles = f.getOrElse(1) { "" },
                tl = f.getOrElse(2) { "" },
                componentes = f.getOrElse(3) { "" },
                alcance = f.getOrElse(4) { "" },
                duracion = f.getOrElse(5) { "" },
                ts = f.getOrElse(6) { "" },
                rc = f.getOrElse(7) { "" },
                descripcion = f.getOrElse(8) { "" },
                esPropio = true
            )
        }
        if (conjuros.isNotEmpty()) db.conjuroDao().insertAll(conjuros)
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

    // ── Notas ──
    fun notas(pid: Long): Flow<List<Nota>> = db.notaDao().observe(pid)
    suspend fun insertNota(n: Nota): Long = db.notaDao().insert(n)
    suspend fun updateNota(n: Nota) = db.notaDao().update(n)
    suspend fun deleteNota(id: Long) = db.notaDao().delete(id)

    internal val database: AppDatabase get() = db

    companion object {
        fun objetoDesde(pid: Long, art: ArticuloDef, lotes: Int = 1) = Objeto(
            personajeId = pid,
            categoria = art.categoria.name,
            catalogId = art.id,
            nombre = art.nombre,
            cantidad = art.unidades * lotes,
            ve = art.ve / art.unidades,
            capacidad = art.capacidad,
            formulaDano = art.dano.orEmpty(),
            formulaDanoDosManos = art.danoDosManos.orEmpty(),
            tipoArma = art.tipo.name,
            alcance = art.alcance,
            dosManos = art.dosManos,
            ca = art.ca,
            notas = art.notas
        )
    }
}

fun Tesoro.bolsa() = Monedas.Bolsa(cobre, plata, electro, oro, platino)
fun Tesoro.conBolsa(b: Monedas.Bolsa) = copy(cobre = b.cobre, plata = b.plata, electro = b.electro, oro = b.oro, platino = b.platino)
