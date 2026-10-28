package io.github.pauverblom.cronicas.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Campaña: un grupo de personajes en la pantalla de inicio. Es solo de este dispositivo, no se sincroniza. */
@Serializable
data class Campana(val id: Long, val nombre: String)

/**
 * Las campañas de este dispositivo y a cuál pertenece cada personaje (como mucho a una).
 * Una campaña existe mientras tenga algún personaje: al quedarse vacía desaparece.
 */
@Serializable
data class DatosCampanas(
    val campanas: List<Campana> = emptyList(),
    /** Id de personaje → id de campaña. */
    val miembros: Map<Long, Long> = emptyMap(),
    /** Campañas plegadas en la pantalla de inicio. */
    val plegadas: Set<Long> = emptySet(),
    val siguienteId: Long = 1
) {
    fun campanaDe(pid: Long): Campana? = miembros[pid]?.let { id -> campanas.firstOrNull { it.id == id } }

    /** Mete a [pid] en la campaña [campanaId], o lo deja sin campaña con null. */
    fun mover(pid: Long, campanaId: Long?): DatosCampanas {
        val m = if (campanaId == null || campanas.none { it.id == campanaId }) miembros - pid else miembros + (pid to campanaId)
        return copy(miembros = m).sinVacias()
    }

    /** Crea la campaña [nombre] y mete en ella a [pid]. */
    fun crearYMover(pid: Long, nombre: String): DatosCampanas {
        val c = Campana(siguienteId, nombre.trim())
        return copy(campanas = campanas + c, siguienteId = siguienteId + 1).mover(pid, c.id)
    }

    fun renombrar(id: Long, nombre: String): DatosCampanas =
        copy(campanas = campanas.map { if (it.id == id) it.copy(nombre = nombre.trim()) else it })

    /** Deshace la campaña: sus personajes quedan sin campaña. */
    fun disolver(id: Long): DatosCampanas = copy(miembros = miembros.filterValues { it != id }).sinVacias()

    fun plegar(id: Long, plegada: Boolean): DatosCampanas = copy(plegadas = if (plegada) plegadas + id else plegadas - id)

    private fun sinVacias(): DatosCampanas {
        val usadas = miembros.values.toSet()
        return copy(campanas = campanas.filter { it.id in usadas }, plegadas = plegadas.filter { it in usadas }.toSet())
    }
}

private val Context.campanasStore by preferencesDataStore("campanas")

class CampanasRepository(private val context: Context) {
    private val clave = stringPreferencesKey("datos")
    private val json = Json { ignoreUnknownKeys = true }

    val datos: Flow<DatosCampanas> = context.campanasStore.data.map { prefs ->
        prefs[clave]?.let { runCatching { json.decodeFromString(DatosCampanas.serializer(), it) }.getOrNull() } ?: DatosCampanas()
    }

    suspend fun editar(cambio: (DatosCampanas) -> DatosCampanas) {
        context.campanasStore.edit { prefs ->
            val actual = prefs[clave]?.let { runCatching { json.decodeFromString(DatosCampanas.serializer(), it) }.getOrNull() } ?: DatosCampanas()
            prefs[clave] = json.encodeToString(DatosCampanas.serializer(), cambio(actual))
        }
    }
}
