package io.github.pauverblom.cronicas.ui.wizard

import io.github.pauverblom.cronicas.data.conRetrato
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.pauverblom.cronicas.CronicasApp
import io.github.pauverblom.cronicas.data.Caracteristicas
import io.github.pauverblom.cronicas.data.PERSONALIZADA
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.Tesoro
import io.github.pauverblom.cronicas.rules.Atributo
import io.github.pauverblom.cronicas.rules.data.Especies
import io.github.pauverblom.cronicas.ui.foto.Retratos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WizardViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as CronicasApp).repository
    private val _state = MutableStateFlow(WizardState())
    val state = _state.asStateFlow()

    fun editar(f: (WizardState) -> WizardState) = _state.update(f)

    fun elegirEspecie(id: String) = _state.update { s ->
        val esp = Especies.porId(id)
        s.copy(especieId = id, idiomas = esp?.idiomas ?: s.idiomas).ajustarPrimarias()
    }

    fun elegirClase(id: String) = _state.update { s -> s.copy(claseId = id).ajustarPrimarias() }

    fun alternarPrimaria(a: Atributo) = _state.update { s ->
        if (a == s.clase?.principal) return@update s // la de la clase es obligatoria
        val nuevas = if (a in s.primarias) s.primarias - a else s.primarias + a
        if (nuevas.size > s.primariasRequeridas) s else s.copy(primarias = nuevas)
    }

    /** Mantiene marcada la característica principal de la clase y recorta si sobran primarias. */
    private fun WizardState.ajustarPrimarias(): WizardState {
        val principal = clase?.principal
        var set = primarias.toMutableSet()
        if (principal != null) set.add(principal)
        while (set.size > primariasRequeridas) set.remove(set.first { it != principal })
        return copy(primarias = set)
    }

    fun siguiente() = _state.update { if (it.pasoValido()) it.copy(paso = (it.paso + 1).coerceAtMost(TITULOS.lastIndex)) else it }
    fun anterior() = _state.update { it.copy(paso = (it.paso - 1).coerceAtLeast(0)) }

    fun crear(onCreado: (Long) -> Unit) {
        val s = _state.value
        if (s.creando) return
        _state.update { it.copy(creando = true) }
        viewModelScope.launch {
            val p = Personaje(
                nombre = s.nombre.trim(),
                jugador = s.jugador.trim(),
                claseId = s.claseId ?: PERSONALIZADA,
                claseNombre = if (s.claseId == PERSONALIZADA) s.claseNombre.trim() else "",
                claseSecundariaId = s.secundaria?.id,
                especieId = s.especieId ?: PERSONALIZADA,
                especieNombre = if (s.especieId == PERSONALIZADA) s.especieNombre.trim() else "",
                alineamiento = s.alineamiento,
                deidad = s.deidad.trim(),
                idiomas = s.idiomas.trim(),
                lugarOrigen = s.lugarOrigen.trim(),
                edad = s.edad.trim(),
                genero = s.genero.trim(),
                altura = s.altura.trim(),
                peso = s.peso.trim(),
                ojos = s.ojos.trim(),
                cabello = s.cabello.trim()
            )
            var c = Caracteristicas(personajeId = 0)
            for (a in Atributo.entries) {
                c = c.conBase(a, s.puntuacion(a) ?: 10).conPrimaria(a, a in s.primarias)
            }
            val id = repo.crearPersonaje(p, c, s.pgTotal ?: 1, Tesoro(0, oro = s.oro.toIntOrNull() ?: 0))
            s.retrato?.let { uri ->
                runCatching { Retratos.guardar(getApplication(), id, uri, null) }.getOrNull()?.let { (path, colores) ->
                    repo.getPersonaje(id)?.let { repo.updatePersonaje(it.conRetrato(path, colores)) }
                }
            }
            onCreado(id)
        }
    }
}
