package io.github.pauverblom.cronicas.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("ajustes")

class AjustesRepository(private val context: Context) {
    private val temaKey = stringPreferencesKey("tema")
    private val modoKey = stringPreferencesKey("modo")

    val ajustes: Flow<AjustesApp> = context.dataStore.data.map { prefs ->
        AjustesApp(
            tema = Tema.entries.firstOrNull { it.name == prefs[temaKey] } ?: Tema.PERGAMINO_OSCURO,
            modo = Modo.entries.firstOrNull { it.name == prefs[modoKey] } ?: Modo.SISTEMA
        )
    }

    suspend fun setTema(t: Tema) = context.dataStore.edit { it[temaKey] = t.name }
    suspend fun setModo(m: Modo) = context.dataStore.edit { it[modoKey] = m.name }
}
