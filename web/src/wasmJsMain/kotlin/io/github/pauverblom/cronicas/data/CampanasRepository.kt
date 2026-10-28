package io.github.pauverblom.cronicas.data

import io.github.pauverblom.cronicas.web.jsGuardarLocal
import io.github.pauverblom.cronicas.web.jsLeerLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

/** Campañas de la versión web, en localStorage. */
class CampanasRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private val estado = MutableStateFlow(
        jsLeerLocal(CLAVE)?.let { runCatching { json.decodeFromString(DatosCampanas.serializer(), it) }.getOrNull() } ?: DatosCampanas()
    )

    val datos: Flow<DatosCampanas> = estado

    suspend fun editar(cambio: (DatosCampanas) -> DatosCampanas) {
        val nuevos = cambio(estado.value)
        jsGuardarLocal(CLAVE, json.encodeToString(DatosCampanas.serializer(), nuevos))
        estado.value = nuevos
    }

    private companion object {
        const val CLAVE = "cronicas.campanas"
    }
}
