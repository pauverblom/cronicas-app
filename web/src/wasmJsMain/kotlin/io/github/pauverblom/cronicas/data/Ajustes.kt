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
            // «Desde retrato» ya no es un tema de toda la app (ahora es de cada ficha)
            tema = Tema.DE_LA_APP.firstOrNull { it.name == jsLeerLocal(CLAVE_TEMA) } ?: Tema.PERGAMINO_OSCURO,
            modo = Modo.entries.firstOrNull { it.name == jsLeerLocal(CLAVE_MODO) } ?: Modo.SISTEMA,
            colores = ColoresTema.PREDETERMINADOS.let { p ->
                ColoresTema(leerColor(CLAVE_ACENTO) ?: p.acento, leerColor(CLAVE_MARCO) ?: p.marco, leerColor(CLAVE_FONDO) ?: p.fondo)
            }
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

    suspend fun setColores(c: ColoresTema) {
        jsGuardarLocal(CLAVE_ACENTO, c.acento.toString())
        jsGuardarLocal(CLAVE_MARCO, c.marco.toString())
        jsGuardarLocal(CLAVE_FONDO, c.fondo.toString())
        estado.update { it.copy(colores = c) }
    }

    private fun leerColor(clave: String): Int? = jsLeerLocal(clave)?.toIntOrNull()

    private companion object {
        const val CLAVE_ACENTO = "cronicas.ajustes.acento"
        const val CLAVE_MARCO = "cronicas.ajustes.marco"
        const val CLAVE_FONDO = "cronicas.ajustes.fondo"
        const val CLAVE_TEMA = "cronicas.ajustes.tema"
        const val CLAVE_MODO = "cronicas.ajustes.modo"
    }
}
