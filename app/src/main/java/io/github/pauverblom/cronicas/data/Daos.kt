package io.github.pauverblom.cronicas.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonajeDao {
    @Query("SELECT * FROM personaje")
    fun getAll(): Flow<List<Personaje>>

    @Query("SELECT * FROM personaje WHERE id = :id")
    suspend fun getById(id: Long): Personaje?

    @Insert
    suspend fun insert(p: Personaje): Long

    @Update
    suspend fun update(p: Personaje)

    @Delete
    suspend fun delete(p: Personaje)

    @Query("SELECT COUNT(*) FROM personaje")
    suspend fun count(): Int
}

@Dao
interface CaracteristicasDao {
    @Query("SELECT * FROM caracteristicas WHERE personajeId = :pid")
    fun getByPersonaje(pid: Long): Flow<Caracteristicas?>

    @Query("SELECT * FROM caracteristicas WHERE personajeId = :pid")
    suspend fun getByPersonajeSync(pid: Long): Caracteristicas?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: Caracteristicas)
}

@Dao
interface EstadoCombateDao {
    @Query("SELECT * FROM estado_combate WHERE personajeId = :pid")
    fun getByPersonaje(pid: Long): Flow<EstadoCombate?>

    @Query("SELECT * FROM estado_combate WHERE personajeId = :pid")
    suspend fun getByPersonajeSync(pid: Long): EstadoCombate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: EstadoCombate)

    @Query("UPDATE estado_combate SET pgActuales = :pg WHERE personajeId = :pid")
    suspend fun updatePg(pid: Long, pg: Int)

    @Query("UPDATE estado_combate SET danoAtenuado = :da WHERE personajeId = :pid")
    suspend fun updateDanoAtenuado(pid: Long, da: Int)
}

@Dao
interface ArmaDao {
    @Query("SELECT * FROM arma WHERE personajeId = :pid")
    fun getByPersonaje(pid: Long): Flow<List<Arma>>

    @Insert
    suspend fun insert(a: Arma): Long

    @Delete
    suspend fun delete(a: Arma)

    @Update
    suspend fun update(a: Arma)
}

@Dao
interface ConjuroDao {
    @Query("SELECT * FROM conjuro ORDER BY nivel, nombre")
    fun getAll(): Flow<List<Conjuro>>

    @Query("SELECT * FROM conjuro WHERE id = :id")
    suspend fun getById(id: Long): Conjuro?

    @Insert
    suspend fun insert(c: Conjuro): Long

    @Insert
    suspend fun insertAll(conjuros: List<Conjuro>): List<Long>

    @Delete
    suspend fun delete(c: Conjuro)

    @Update
    suspend fun update(c: Conjuro)

    @Query("DELETE FROM conjuro WHERE esPropio = 0")
    suspend fun deleteNonOwn()
}

/** Fila de conjuro_personaje unida con los datos del conjuro. */
data class ConjuroConDetalle(
    val personajeId: Long,
    val conjuroId: Long,
    val conocido: Boolean,
    val preparadoVeces: Int,
    val gastadoVeces: Int,
    val nombre: String,
    val nivel: Int,
    val tl: Int,
    val componentes: String,
    val alcance: String,
    val duracion: String,
    val ts: String,
    val rc: String,
    val descripcion: String,
    val notaTactica: String,
    val verificar: Boolean
)

@Dao
interface ConjuroPersonajeDao {
    @Query(
        """
        SELECT cp.*, c.nombre, c.nivel, c.tl, c.componentes, c.alcance,
               c.duracion, c.ts, c.rc, c.descripcion, c.notaTactica, c.verificar
        FROM conjuro_personaje cp
        JOIN conjuro c ON cp.conjuroId = c.id
        WHERE cp.personajeId = :pid
        ORDER BY c.nivel, c.nombre
    """
    )
    fun getWithConjuro(pid: Long): Flow<List<ConjuroConDetalle>>

    @Query("SELECT * FROM conjuro_personaje WHERE personajeId = :pid AND conjuroId = :cid")
    suspend fun get(pid: Long, cid: Long): ConjuroPersonaje?

    @Query("SELECT * FROM conjuro_personaje WHERE personajeId = :pid")
    suspend fun getAllForPersonaje(pid: Long): List<ConjuroPersonaje>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cp: ConjuroPersonaje)

    @Delete
    suspend fun delete(cp: ConjuroPersonaje)

    @Query("UPDATE conjuro_personaje SET gastadoVeces = gastadoVeces + 1 WHERE personajeId = :pid AND conjuroId = :cid AND gastadoVeces < preparadoVeces")
    suspend fun gastar(pid: Long, cid: Long): Int

    @Query("UPDATE conjuro_personaje SET gastadoVeces = 0 WHERE personajeId = :pid")
    suspend fun resetGastados(pid: Long)

    @Query("UPDATE conjuro_personaje SET preparadoVeces = :veces WHERE personajeId = :pid AND conjuroId = :cid")
    suspend fun updatePreparado(pid: Long, cid: Long, veces: Int)

    @Query("DELETE FROM conjuro_personaje WHERE personajeId = :pid AND conjuroId = :cid")
    suspend fun deleteByIds(pid: Long, cid: Long)
}

@Dao
interface EspaciosConjuroDao {
    @Query("SELECT * FROM espacios_conjuro WHERE personajeId = :pid ORDER BY nivel")
    fun getByPersonaje(pid: Long): Flow<List<EspaciosConjuro>>

    @Query("SELECT * FROM espacios_conjuro WHERE personajeId = :pid ORDER BY nivel")
    suspend fun getByPersonajeSync(pid: Long): List<EspaciosConjuro>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: EspaciosConjuro)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(slots: List<EspaciosConjuro>)
}

@Dao
interface ObjetoDao {
    @Query("SELECT * FROM objeto WHERE personajeId = :pid ORDER BY esMagico DESC, nombre")
    fun getByPersonaje(pid: Long): Flow<List<Objeto>>

    @Insert
    suspend fun insert(o: Objeto): Long

    @Delete
    suspend fun delete(o: Objeto)

    @Update
    suspend fun update(o: Objeto)
}

@Dao
interface TesoroDao {
    @Query("SELECT * FROM tesoro WHERE personajeId = :pid")
    fun getByPersonaje(pid: Long): Flow<Tesoro?>

    @Query("SELECT * FROM tesoro WHERE personajeId = :pid")
    suspend fun getByPersonajeSync(pid: Long): Tesoro?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(t: Tesoro)
}

@Dao
interface EfectoActivoDao {
    @Query("SELECT * FROM efecto_activo WHERE personajeId = :pid")
    fun getByPersonaje(pid: Long): Flow<List<EfectoActivo>>

    @Insert
    suspend fun insert(e: EfectoActivo): Long

    @Delete
    suspend fun delete(e: EfectoActivo)

    @Query("DELETE FROM efecto_activo WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface NotaDao {
    @Query("SELECT * FROM nota WHERE personajeId = :pid ORDER BY timestamp DESC")
    fun getByPersonaje(pid: Long): Flow<List<Nota>>

    @Insert
    suspend fun insert(n: Nota): Long

    @Update
    suspend fun update(n: Nota)

    @Query("DELETE FROM nota WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AccionUndoDao {
    @Insert
    suspend fun insert(a: AccionUndo): Long

    @Query("DELETE FROM accion_undo WHERE id = :id")
    suspend fun delete(id: Long)

    /** Conserva solo las 30 acciones más recientes por personaje. */
    @Query("DELETE FROM accion_undo WHERE personajeId = :pid AND id NOT IN (SELECT id FROM accion_undo WHERE personajeId = :pid ORDER BY timestamp DESC LIMIT 30)")
    suspend fun trimOld(pid: Long)

    @Query("SELECT * FROM accion_undo WHERE personajeId = :pid ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastAction(pid: Long): AccionUndo?

    @Query("SELECT COUNT(*) FROM accion_undo WHERE personajeId = :pid")
    suspend fun count(pid: Long): Int
}
