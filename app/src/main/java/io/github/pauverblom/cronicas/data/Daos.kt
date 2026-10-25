package io.github.pauverblom.cronicas.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonajeDao {
    @Query("SELECT * FROM personaje ORDER BY creado")
    fun getAll(): Flow<List<Personaje>>

    @Query("SELECT * FROM personaje WHERE id = :id")
    fun observe(id: Long): Flow<Personaje?>

    @Query("SELECT * FROM personaje WHERE id = :id")
    suspend fun getById(id: Long): Personaje?

    @Insert
    suspend fun insert(p: Personaje): Long

    @Update
    suspend fun update(p: Personaje)

    @Query("DELETE FROM personaje WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CaracteristicasDao {
    @Query("SELECT * FROM caracteristicas WHERE personajeId = :pid")
    fun observe(pid: Long): Flow<Caracteristicas?>

    @Query("SELECT * FROM caracteristicas WHERE personajeId = :pid")
    suspend fun get(pid: Long): Caracteristicas?

    @Query("SELECT * FROM caracteristicas")
    fun getAll(): Flow<List<Caracteristicas>>

    @Upsert
    suspend fun upsert(c: Caracteristicas)
}

@Dao
interface EstadoDao {
    @Query("SELECT * FROM estado WHERE personajeId = :pid")
    fun observe(pid: Long): Flow<Estado?>

    @Query("SELECT * FROM estado WHERE personajeId = :pid")
    suspend fun get(pid: Long): Estado?

    @Query("SELECT * FROM estado")
    fun getAll(): Flow<List<Estado>>

    @Upsert
    suspend fun upsert(e: Estado)
}

@Dao
interface ObjetoDao {
    @Query("SELECT * FROM objeto WHERE personajeId = :pid ORDER BY id")
    fun observe(pid: Long): Flow<List<Objeto>>

    @Query("SELECT * FROM objeto WHERE personajeId = :pid ORDER BY id")
    suspend fun getAll(pid: Long): List<Objeto>

    @Insert
    suspend fun insert(o: Objeto): Long

    @Update
    suspend fun update(o: Objeto)

    @Delete
    suspend fun delete(o: Objeto)

    /** Saca el contenido de un contenedor que se va a borrar. */
    @Query("UPDATE objeto SET contenedorId = NULL WHERE contenedorId = :contenedorId")
    suspend fun vaciarContenedor(contenedorId: Long)

    @Query("UPDATE objeto SET companiaId = NULL WHERE companiaId = :companiaId")
    suspend fun descargarCompania(companiaId: Long)
}

@Dao
interface TesoroDao {
    @Query("SELECT * FROM tesoro WHERE personajeId = :pid")
    fun observe(pid: Long): Flow<Tesoro?>

    @Query("SELECT * FROM tesoro WHERE personajeId = :pid")
    suspend fun get(pid: Long): Tesoro?

    @Upsert
    suspend fun upsert(t: Tesoro)
}

@Dao
interface HabilidadDao {
    @Query("SELECT * FROM habilidad WHERE personajeId = :pid ORDER BY id")
    fun observe(pid: Long): Flow<List<Habilidad>>

    @Query("SELECT * FROM habilidad WHERE personajeId = :pid ORDER BY id")
    suspend fun getAll(pid: Long): List<Habilidad>

    @Insert
    suspend fun insert(h: Habilidad): Long

    @Insert
    suspend fun insertAll(h: List<Habilidad>)

    @Update
    suspend fun update(h: Habilidad)

    @Delete
    suspend fun delete(h: Habilidad)
}

@Dao
interface CondicionDao {
    @Query("SELECT * FROM condicion WHERE personajeId = :pid ORDER BY id")
    fun observe(pid: Long): Flow<List<Condicion>>

    @Query("SELECT * FROM condicion WHERE personajeId = :pid ORDER BY id")
    suspend fun getAll(pid: Long): List<Condicion>

    @Insert
    suspend fun insert(c: Condicion): Long

    @Update
    suspend fun update(c: Condicion)

    @Delete
    suspend fun delete(c: Condicion)
}

@Dao
interface CompaniaDao {
    @Query("SELECT * FROM compania WHERE personajeId = :pid ORDER BY id")
    fun observe(pid: Long): Flow<List<Compania>>

    @Query("SELECT * FROM compania WHERE personajeId = :pid ORDER BY id")
    suspend fun getAll(pid: Long): List<Compania>

    @Insert
    suspend fun insert(c: Compania): Long

    @Update
    suspend fun update(c: Compania)

    @Delete
    suspend fun delete(c: Compania)
}

data class ConjuroConDetalle(
    @Embedded val cp: ConjuroPersonaje,
    @Embedded(prefix = "c_") val conjuro: ConjuroPlano
)

/** Columnas de [Conjuro] con prefijo para el JOIN. */
data class ConjuroPlano(
    val id: Long,
    val nombre: String,
    val niveles: String,
    val tl: String,
    val componentes: String,
    val alcance: String,
    val duracion: String,
    val ts: String,
    val rc: String,
    val descripcion: String,
    val esPropio: Boolean
) {
    fun toConjuro() = Conjuro(id, nombre, niveles, tl, componentes, alcance, duracion, ts, rc, descripcion, esPropio)
}

@Dao
interface ConjuroDao {
    @Query("SELECT * FROM conjuro ORDER BY nombre COLLATE NOCASE")
    fun getAll(): Flow<List<Conjuro>>

    @Query("SELECT * FROM conjuro")
    suspend fun getAllOnce(): List<Conjuro>

    @Query("SELECT COUNT(*) FROM conjuro WHERE esPropio = 0")
    suspend fun countCatalogo(): Int

    @Query("SELECT * FROM conjuro WHERE nombre = :nombre LIMIT 1")
    suspend fun byName(nombre: String): Conjuro?

    @Insert
    suspend fun insert(c: Conjuro): Long

    @Insert
    suspend fun insertAll(list: List<Conjuro>): List<Long>

    @Update
    suspend fun update(c: Conjuro)

    @Query(
        """
        SELECT cp.*, c.id AS c_id, c.nombre AS c_nombre, c.niveles AS c_niveles, c.tl AS c_tl,
               c.componentes AS c_componentes, c.alcance AS c_alcance, c.duracion AS c_duracion,
               c.ts AS c_ts, c.rc AS c_rc, c.descripcion AS c_descripcion, c.esPropio AS c_esPropio
        FROM conjuro_personaje cp JOIN conjuro c ON c.id = cp.conjuroId
        WHERE cp.personajeId = :pid
        ORDER BY cp.nivel, c.nombre COLLATE NOCASE
        """
    )
    fun observeDePersonaje(pid: Long): Flow<List<ConjuroConDetalle>>

    @Query("SELECT * FROM conjuro_personaje WHERE personajeId = :pid")
    suspend fun getDePersonaje(pid: Long): List<ConjuroPersonaje>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDePersonaje(cp: ConjuroPersonaje)

    @Update
    suspend fun updateDePersonaje(cp: ConjuroPersonaje)

    @Query("DELETE FROM conjuro_personaje WHERE personajeId = :pid AND conjuroId = :cid")
    suspend fun deleteDePersonaje(pid: Long, cid: Long)

    @Query("UPDATE conjuro_personaje SET usadoVeces = 0 WHERE personajeId = :pid")
    suspend fun descansar(pid: Long)

    @Query("SELECT * FROM espacio_override WHERE personajeId = :pid")
    fun observeEspacios(pid: Long): Flow<List<EspacioOverride>>

    @Query("SELECT * FROM espacio_override WHERE personajeId = :pid")
    suspend fun getEspacios(pid: Long): List<EspacioOverride>

    @Upsert
    suspend fun upsertEspacio(e: EspacioOverride)

    @Query("DELETE FROM espacio_override WHERE personajeId = :pid AND nivel = :nivel")
    suspend fun deleteEspacio(pid: Long, nivel: Int)
}

@Dao
interface NotaDao {
    @Query("SELECT * FROM nota WHERE personajeId = :pid ORDER BY timestamp DESC")
    fun observe(pid: Long): Flow<List<Nota>>

    @Query("SELECT * FROM nota WHERE personajeId = :pid ORDER BY timestamp DESC")
    suspend fun getAll(pid: Long): List<Nota>

    @Insert
    suspend fun insert(n: Nota): Long

    @Update
    suspend fun update(n: Nota)

    @Query("DELETE FROM nota WHERE id = :id")
    suspend fun delete(id: Long)
}
