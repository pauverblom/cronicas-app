package io.github.pauverblom.cronicas.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("ajustes")

class AjustesRepository(private val context: Context) {
    private val temaKey = stringPreferencesKey("tema")
    private val modoKey = stringPreferencesKey("modo")
    private val acentoKey = intPreferencesKey("acento")
    private val marcoKey = intPreferencesKey("marco")
    private val fondoKey = intPreferencesKey("fondo")

    val ajustes: Flow<AjustesApp> = context.dataStore.data.map { prefs ->
        val p = ColoresTema.PREDETERMINADOS
        AjustesApp(
            // «Desde retrato» ya no es un tema de toda la app (ahora es de cada ficha)
            tema = Tema.DE_LA_APP.firstOrNull { it.name == prefs[temaKey] } ?: Tema.PERGAMINO_OSCURO,
            modo = Modo.entries.firstOrNull { it.name == prefs[modoKey] } ?: Modo.SISTEMA,
            colores = ColoresTema(prefs[acentoKey] ?: p.acento, prefs[marcoKey] ?: p.marco, prefs[fondoKey] ?: p.fondo)
        )
    }

    suspend fun setTema(t: Tema) = context.dataStore.edit { it[temaKey] = t.name }
    suspend fun setModo(m: Modo) = context.dataStore.edit { it[modoKey] = m.name }
    suspend fun setColores(c: ColoresTema) = context.dataStore.edit {
        it[acentoKey] = c.acento
        it[marcoKey] = c.marco
        it[fondoKey] = c.fondo
    }
}
