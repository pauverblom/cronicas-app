package io.github.pauverblom.cronicas.data

import io.github.pauverblom.cronicas.web.jsGuardarLocal
import io.github.pauverblom.cronicas.web.jsLeerLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Ajustes de la versión web, en localStorage. */
class AjustesRepository {
    private val estado = MutableStateFlow(
        AjustesApp(
            tema = Tema.entries.firstOrNull { it.name == jsLeerLocal(CLAVE_TEMA) } ?: Tema.PERGAMINO_OSCURO,
            modo = Modo.entries.firstOrNull { it.name == jsLeerLocal(CLAVE_MODO) } ?: Modo.SISTEMA
        )
    )

    val ajustes: Flow<AjustesApp> = estado

    /** Los ajustes ya cargados (en la web se leen al instante). */
    val actuales: AjustesApp get() = estado.value

    suspend fun setTema(t: Tema) {
        jsGuardarLocal(CLAVE_TEMA, t.name)
        estado.update { it.copy(tema = t) }
    }

    suspend fun setModo(m: Modo) {
        jsGuardarLocal(CLAVE_MODO, m.name)
        estado.update { it.copy(modo = m) }
    }

    private companion object {
        const val CLAVE_TEMA = "cronicas.ajustes.tema"
        const val CLAVE_MODO = "cronicas.ajustes.modo"
    }
}
