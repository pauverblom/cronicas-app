package io.github.pauverblom.cronicas.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

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
