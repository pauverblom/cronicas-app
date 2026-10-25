package io.github.pauverblom.cronicas.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Tema(val etiqueta: String, val descripcion: String) {
    PERGAMINO_OSCURO("Pergamino oscuro", "Marrón oscuro y oro"),
    PERGAMINO_CLARO("Pergamino claro", "Papel crema, tinta oscura y rojo"),
    MATERIAL_YOU("Material You", "Colores del fondo de pantalla (Android 12+)"),
    RETRATO("Desde retrato", "Cada ficha toma los colores del retrato de su personaje")
}

enum class Modo(val etiqueta: String) { SISTEMA("Sistema"), CLARO("Claro"), OSCURO("Oscuro") }

data class AjustesApp(val tema: Tema = Tema.PERGAMINO_OSCURO, val modo: Modo = Modo.SISTEMA)

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
