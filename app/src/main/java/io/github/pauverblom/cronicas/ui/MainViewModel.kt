package io.github.pauverblom.cronicas.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.pauverblom.cronicas.CronicasApp
import io.github.pauverblom.cronicas.data.Arma
import io.github.pauverblom.cronicas.data.Caracteristicas
import io.github.pauverblom.cronicas.data.Conjuro
import io.github.pauverblom.cronicas.data.ConjuroConDetalle
import io.github.pauverblom.cronicas.data.ConjuroPersonaje
import io.github.pauverblom.cronicas.data.EfectoActivo
import io.github.pauverblom.cronicas.data.EspaciosConjuro
import io.github.pauverblom.cronicas.data.EstadoCombate
import io.github.pauverblom.cronicas.data.Nota
import io.github.pauverblom.cronicas.data.Objeto
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.Repository
import io.github.pauverblom.cronicas.data.Tesoro
import io.github.pauverblom.cronicas.rules.Dice
import io.github.pauverblom.cronicas.rules.Encumbrance
import io.github.pauverblom.cronicas.rules.Modifiers
import io.github.pauverblom.cronicas.undo.UndoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MesaUiState(
    val personaje: Personaje? = null,
    val allPersonajes: List<Personaje> = emptyList(),
    val caracteristicas: Caracteristicas? = null,
    val combate: EstadoCombate? = null,
    val armas: List<Arma> = emptyList(),
    val espacios: List<EspaciosConjuro> = emptyList(),
    val conjurosPersonaje: List<ConjuroConDetalle> = emptyList(),
    val efectos: List<EfectoActivo> = emptyList(),
    val objetos: List<Objeto> = emptyList(),
    val tesoro: Tesoro? = null,
    val notas: List<Nota> = emptyList(),
    val snackbarMessage: String? = null,
    val loading: Boolean = true
)

data class DiceRollResult(
    val formula: String,
    val breakdown: String,
    val total: Int
)

/** Datos editables de un personaje: los del diálogo de crear/editar personaje. */
data class CharacterForm(
    val nombre: String = "",
    val clase: String = "",
    val especie: String = "",
    val nivel: Int = 1,
    val fue: Int = 10,
    val des: Int = 10,
    val con: Int = 10,
    val int_: Int = 10,
    val sab: Int = 10,
    val car: Int = 10,
    val pgMax: Int = 4,
    val dadoGolpe: String = "d4"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CronicasApp
    private val repo: Repository = app.repository
    private val undoManager = UndoManager(repo)

    // Remembers the selected character across launches. It used to be hardcoded to 1 (the seeded
    // character), which left the app empty and uneditable once that character was deleted.
    private val prefs = application.getSharedPreferences("cronicas", Context.MODE_PRIVATE)
    private val _currentPersonajeId = MutableStateFlow(prefs.getLong(KEY_SELECTED_PERSONAJE, 1L))

    private val _uiState = MutableStateFlow(MesaUiState())
    val uiState: StateFlow<MesaUiState> = _uiState.asStateFlow()

    private val _diceResult = MutableStateFlow<DiceRollResult?>(null)
    val diceResult: StateFlow<DiceRollResult?> = _diceResult.asStateFlow()

    private val _allConjuros = MutableStateFlow<List<Conjuro>>(emptyList())
    val allConjuros: StateFlow<List<Conjuro>> = _allConjuros.asStateFlow()

    init {
        viewModelScope.launch {
            _currentPersonajeId.collectLatest { pid ->
                launch {
                    repo.allPersonajes().collect { list ->
                        val p = list.firstOrNull { it.id == pid }
                        if (p == null && list.isNotEmpty()) {
                            // Selected character no longer exists: fall back to the first one.
                            selectCharacter(list.first().id)
                            return@collect
                        }
                        _uiState.update { it.copy(personaje = p, allPersonajes = list, loading = false) }
                    }
                }
                launch {
                    repo.caracteristicas(pid).collect { c ->
                        _uiState.update { it.copy(caracteristicas = c) }
                    }
                }
                launch {
                    repo.estadoCombate(pid).collect { ec ->
                        _uiState.update { it.copy(combate = ec) }
                    }
                }
                launch {
                    repo.espaciosConjuro(pid).collect { list ->
                        _uiState.update { it.copy(espacios = list) }
                    }
                }
                launch {
                    repo.conjurosPersonaje(pid).collect { list ->
                        _uiState.update { it.copy(conjurosPersonaje = list) }
                    }
                }
                launch {
                    repo.armas(pid).collect { list ->
                        _uiState.update { it.copy(armas = list) }
                    }
                }
                launch {
                    repo.efectosActivos(pid).collect { list ->
                        _uiState.update { it.copy(efectos = list) }
                    }
                }
                launch {
                    repo.objetos(pid).collect { list ->
                        _uiState.update { it.copy(objetos = list) }
                    }
                }
                launch {
                    repo.tesoro(pid).collect { t ->
                        _uiState.update { it.copy(tesoro = t) }
                    }
                }
                launch {
                    repo.notas(pid).collect { list ->
                        _uiState.update { it.copy(notas = list) }
                    }
                }
                launch {
                    repo.allConjuros().collect { list ->
                        _allConjuros.value = list
                    }
                }
            }
        }
    }

    private val pid: Long get() = _currentPersonajeId.value

    // ── PG ──

    /** PG actuales siempre entre 0 y los PG máximos del personaje. */
    private fun clampPg(value: Int, combate: EstadoCombate): Int =
        value.coerceIn(0, combate.pgMaximos.coerceAtLeast(0))

    fun changePg(delta: Int) {
        viewModelScope.launch {
            val current = _uiState.value.combate ?: return@launch
            val newPg = clampPg(current.pgActuales + delta, current)
            if (newPg == current.pgActuales) {
                showSnackbar(if (delta > 0) "PG ya al máximo (${current.pgMaximos})" else "PG ya a 0")
                return@launch
            }
            undoManager.pushPgChange(pid, current.pgActuales)
            repo.updatePg(pid, newPg)
            showSnackbar("PG: ${current.pgActuales} → $newPg")
        }
    }

    fun setPg(value: Int) {
        viewModelScope.launch {
            val current = _uiState.value.combate ?: return@launch
            val newPg = clampPg(value, current)
            if (newPg == current.pgActuales) return@launch
            undoManager.pushPgChange(pid, current.pgActuales)
            repo.updatePg(pid, newPg)
        }
    }

    fun undo() {
        viewModelScope.launch {
            val msg = undoManager.undo(pid)
            if (msg != null) showSnackbar(msg)
        }
    }

    // ── Conjuros ──

    fun lanzarConjuro(conjuroId: Long) {
        viewModelScope.launch {
            val cp = repo.getConjuroPersonaje(pid, conjuroId) ?: return@launch
            if (cp.gastadoVeces >= cp.preparadoVeces) return@launch
            undoManager.pushConjuroGastado(pid, conjuroId, cp.gastadoVeces)
            repo.upsertConjuroPersonaje(cp.copy(gastadoVeces = cp.gastadoVeces + 1))
            showSnackbar("Conjuro lanzado")
        }
    }

    fun prepararConjuro(conjuroId: Long) {
        viewModelScope.launch {
            val cp = repo.getConjuroPersonaje(pid, conjuroId)
            if (cp != null) {
                repo.upsertConjuroPersonaje(cp.copy(preparadoVeces = cp.preparadoVeces + 1))
            } else {
                repo.upsertConjuroPersonaje(
                    ConjuroPersonaje(pid, conjuroId, conocido = true, preparadoVeces = 1, gastadoVeces = 0)
                )
            }
        }
    }

    fun desprepararConjuro(conjuroId: Long) {
        viewModelScope.launch {
            val cp = repo.getConjuroPersonaje(pid, conjuroId) ?: return@launch
            if (cp.preparadoVeces > 0) {
                repo.upsertConjuroPersonaje(cp.copy(preparadoVeces = cp.preparadoVeces - 1))
            }
        }
    }

    /** Descanso: recupera todos los usos gastados (deshacible). */
    fun descansar() {
        viewModelScope.launch {
            val allCps = repo.allConjuroPersonaje(pid)
            val gastados = allCps.filter { it.gastadoVeces > 0 }.associate { it.conjuroId to it.gastadoVeces }
            if (gastados.isNotEmpty()) {
                undoManager.pushDescanso(pid, gastados)
            }
            repo.resetConjurosGastados(pid)
            showSnackbar("Descanso completado")
        }
    }

    fun addToGrimoire(conjuroId: Long) {
        viewModelScope.launch {
            val existing = repo.getConjuroPersonaje(pid, conjuroId)
            if (existing == null) {
                repo.upsertConjuroPersonaje(
                    ConjuroPersonaje(pid, conjuroId, conocido = true, preparadoVeces = 0, gastadoVeces = 0)
                )
            }
        }
    }

    fun removeFromGrimoire(conjuroId: Long) {
        viewModelScope.launch {
            repo.deleteConjuroPersonaje(pid, conjuroId)
        }
    }

    // ── Dados ──

    fun tirarSalvacion(stat: String): DiceRollResult {
        val caract = _uiState.value.caracteristicas
        val mod = if (caract != null) getModForStat(stat, caract) else 0
        val roll = Dice.roll("d20")
        val d20Value = roll.total
        val total = d20Value + mod
        val modStr = if (mod >= 0) "+$mod" else "$mod"
        val result = DiceRollResult(
            formula = "d20 $modStr",
            breakdown = "d20($d20Value) $modStr = $total",
            total = total
        )
        _diceResult.value = result
        return result
    }

    fun rollDice(formula: String): DiceRollResult? {
        if (!Dice.parse(formula)) return null
        val result = Dice.roll(formula)
        val parts = mutableListOf<String>()
        for (roll in result.rolls) {
            val diceStr = roll.results.joinToString(", ")
            parts.add("${roll.count}d${roll.sides}[$diceStr]")
        }
        if (result.constant != 0) {
            parts.add(if (result.constant > 0) "+${result.constant}" else "${result.constant}")
        }
        val breakdown = parts.joinToString(" + ") + " = " + result.total
        val dr = DiceRollResult(formula, breakdown, result.total)
        _diceResult.value = dr
        return dr
    }

    fun clearDiceResult() {
        _diceResult.value = null
    }

    fun importCsv(content: String) {
        viewModelScope.launch {
            val count = repo.importConjurosFromCsv(content)
            showSnackbar("$count conjuros importados")
        }
    }

    // ── Combate ──

    fun rollAttack(arma: Arma): DiceRollResult {
        val combate = _uiState.value.combate
        val caract = _uiState.value.caracteristicas
        val ba = combate?.ba ?: 0
        val statMod = if (caract != null) {
            if (arma.tipo == "distancia") getModForStat("DES", caract) else getModForStat("FUE", caract)
        } else {
            0
        }
        val totalBonus = maxOf(0, ba + statMod + arma.baExtra)
        val roll = Dice.roll("d20")
        val d20Val = roll.total
        val total = d20Val + totalBonus
        val bonusStr = if (totalBonus >= 0) "+$totalBonus" else "$totalBonus"
        val result = DiceRollResult(
            formula = "d20 $bonusStr",
            breakdown = "d20($d20Val) $bonusStr = $total",
            total = total
        )
        _diceResult.value = result
        return result
    }

    fun addArma(arma: Arma) {
        viewModelScope.launch {
            repo.insertArma(arma.copy(id = 0, personajeId = pid))
        }
    }

    fun deleteArma(arma: Arma) {
        viewModelScope.launch {
            repo.deleteArma(arma)
        }
    }

    fun rollDamage(arma: Arma): DiceRollResult? = rollDice(arma.formulaDano)

    // ── Efectos activos ──

    fun addEffect(nombre: String, unidad: String, duracion: Int) {
        viewModelScope.launch {
            repo.insertEfecto(
                EfectoActivo(personajeId = pid, nombre = nombre, unidad = unidad, restante = duracion)
            )
        }
    }

    fun removeEffect(id: Long) {
        viewModelScope.launch {
            repo.deleteEfecto(id)
        }
    }

    fun tickEffects(units: Int, unitType: String) {
        viewModelScope.launch {
            val current = _uiState.value.efectos
            val expiring = mutableListOf<String>()
            for (efecto in current) {
                if (efecto.unidad != unitType) continue
                val newRestante = efecto.restante - units
                if (newRestante <= 0) {
                    repo.deleteEfecto(efecto.id)
                    expiring.add(efecto.nombre)
                } else {
                    repo.insertEfecto(efecto.copy(id = 0, restante = newRestante))
                    repo.deleteEfecto(efecto.id)
                }
            }
            if (expiring.isNotEmpty()) {
                showSnackbar("¡Expirado: ${expiring.joinToString(", ")}!")
            }
        }
    }

    fun advanceRound() = tickEffects(1, "asalto")

    fun advanceTurn() = tickEffects(1, "turno")

    /** Avanza un minuto: efectos por minuto y recuperación de 1 PG de daño atenuado. */
    fun advanceMinute() {
        viewModelScope.launch {
            tickEffects(1, "minuto")
            val combate = _uiState.value.combate ?: return@launch
            if (combate.danoAtenuado > 0) {
                val newDa = combate.danoAtenuado - 1
                repo.updateDanoAtenuado(pid, newDa)
                val newPg = clampPg(combate.pgActuales + 1, combate)
                repo.updatePg(pid, newPg)
                showSnackbar("Daño atenuado: recuperas 1 PG (${combate.danoAtenuado}→$newDa)")
            }
        }
    }

    // ── Equipo ──

    fun addObjeto(nombre: String, cantidad: Int, ve: Float, esMagico: Boolean, descripcion: String) {
        viewModelScope.launch {
            repo.insertObjeto(
                Objeto(
                    personajeId = pid,
                    nombre = nombre,
                    cantidad = cantidad,
                    ve = ve,
                    esMagico = esMagico,
                    descripcion = descripcion
                )
            )
        }
    }

    fun updateObjeto(obj: Objeto) {
        viewModelScope.launch {
            repo.updateObjeto(obj)
        }
    }

    fun deleteObjeto(obj: Objeto) {
        viewModelScope.launch {
            repo.deleteObjeto(obj)
        }
    }

    fun updateMonedas(tipo: String, delta: Int) {
        viewModelScope.launch {
            val current = _uiState.value.tesoro ?: Tesoro(personajeId = pid)
            val updated = when (tipo) {
                "oro" -> current.copy(oro = maxOf(0, current.oro + delta))
                "plata" -> current.copy(plata = maxOf(0, current.plata + delta))
                "platino" -> current.copy(platino = maxOf(0, current.platino + delta))
                "cobre" -> current.copy(cobre = maxOf(0, current.cobre + delta))
                else -> current
            }
            repo.upsertTesoro(updated)
        }
    }

    fun setMonedas(tipo: String, value: Int) {
        viewModelScope.launch {
            val current = _uiState.value.tesoro ?: Tesoro(personajeId = pid)
            val safeVal = maxOf(0, value)
            val updated = when (tipo) {
                "oro" -> current.copy(oro = safeVal)
                "plata" -> current.copy(plata = safeVal)
                "platino" -> current.copy(platino = safeVal)
                "cobre" -> current.copy(cobre = safeVal)
                else -> current
            }
            repo.upsertTesoro(updated)
        }
    }

    fun setGemas(text: String) {
        viewModelScope.launch {
            val current = _uiState.value.tesoro ?: Tesoro(personajeId = pid)
            repo.upsertTesoro(current.copy(gemas = text))
        }
    }

    fun getCarryLimit(): Int {
        val caract = _uiState.value.caracteristicas ?: return 9
        return Encumbrance.carryLimit(caract.fue, caract.fuePrimaria, caract.conPrimaria)
    }

    fun getTotalVe(): Float {
        val armas = _uiState.value.armas
        val objetos = _uiState.value.objetos
        val armaVe = armas.sumOf { it.ve.toDouble() }.toFloat()
        val objVe = objetos.sumOf { (it.cantidad * it.ve).toDouble() }.toFloat()
        return armaVe + objVe
    }

    // ── Personajes ──

    fun switchCharacter(newPid: Long) {
        selectCharacter(newPid)
    }

    private fun selectCharacter(newPid: Long) {
        _currentPersonajeId.value = newPid
        prefs.edit().putLong(KEY_SELECTED_PERSONAJE, newPid).apply()
    }

    fun createCharacter(form: CharacterForm) {
        viewModelScope.launch {
            val newPid = repo.insertPersonaje(
                Personaje(nombre = form.nombre, clase = form.clase, especie = form.especie, nivel = form.nivel)
            )
            repo.upsertCaracteristicas(
                Caracteristicas(
                    personajeId = newPid,
                    fue = form.fue,
                    des = form.des,
                    con = form.con,
                    int_ = form.int_,
                    sab = form.sab,
                    car = form.car
                )
            )
            repo.upsertEstadoCombate(
                EstadoCombate(
                    personajeId = newPid,
                    pgActuales = form.pgMax,
                    pgMaximos = form.pgMax,
                    dadoGolpe = form.dadoGolpe
                )
            )
            repo.upsertTesoro(Tesoro(personajeId = newPid))
            selectCharacter(newPid)
            showSnackbar("${form.nombre} creado")
        }
    }

    /** Valores actuales del personaje seleccionado, para rellenar el diálogo de edición. */
    fun currentCharacterForm(): CharacterForm? {
        val state = _uiState.value
        val p = state.personaje ?: return null
        val c = state.caracteristicas
        val ec = state.combate
        val defaults = CharacterForm()
        return CharacterForm(
            nombre = p.nombre,
            clase = p.clase,
            especie = p.especie,
            nivel = p.nivel,
            fue = c?.fue ?: defaults.fue,
            des = c?.des ?: defaults.des,
            con = c?.con ?: defaults.con,
            int_ = c?.int_ ?: defaults.int_,
            sab = c?.sab ?: defaults.sab,
            car = c?.car ?: defaults.car,
            pgMax = ec?.pgMaximos ?: defaults.pgMax,
            dadoGolpe = ec?.dadoGolpe ?: defaults.dadoGolpe
        )
    }

    /** Edita el personaje seleccionado, conservando el resto de sus datos (PG actuales, CA, etc.). */
    fun updateCharacter(form: CharacterForm) {
        viewModelScope.launch {
            val state = _uiState.value
            val p = state.personaje ?: return@launch
            repo.updatePersonaje(
                p.copy(nombre = form.nombre, clase = form.clase, especie = form.especie, nivel = form.nivel)
            )
            val c = state.caracteristicas ?: Caracteristicas(personajeId = p.id)
            repo.upsertCaracteristicas(
                c.copy(fue = form.fue, des = form.des, con = form.con, int_ = form.int_, sab = form.sab, car = form.car)
            )
            val ec = state.combate ?: EstadoCombate(personajeId = p.id, pgActuales = form.pgMax)
            repo.upsertEstadoCombate(
                ec.copy(
                    pgMaximos = form.pgMax,
                    pgActuales = ec.pgActuales.coerceIn(0, form.pgMax.coerceAtLeast(0)),
                    dadoGolpe = form.dadoGolpe
                )
            )
        }
    }

    fun updateCharacterInfo(personaje: Personaje) {
        viewModelScope.launch {
            repo.updatePersonaje(personaje)
        }
    }

    fun deleteCharacter(personaje: Personaje) {
        viewModelScope.launch {
            val allPjs = _uiState.value.allPersonajes
            repo.deletePersonaje(personaje)
            // Si era el seleccionado, pasa al siguiente. Sin personajes, la app muestra la pantalla de bienvenida.
            if (personaje.id == pid) {
                allPjs.firstOrNull { it.id != personaje.id }?.let { selectCharacter(it.id) }
            }
            showSnackbar("${personaje.nombre} eliminado")
        }
    }

    // ── Notas ──

    fun addNota(titulo: String, contenido: String) {
        viewModelScope.launch {
            repo.insertNota(Nota(personajeId = pid, titulo = titulo, contenido = contenido))
        }
    }

    fun updateNota(nota: Nota) {
        viewModelScope.launch {
            repo.updateNota(nota)
        }
    }

    fun deleteNota(id: Long) {
        viewModelScope.launch {
            repo.deleteNota(id)
        }
    }

    // ── Utilidades ──

    fun getModForStat(stat: String, caract: Caracteristicas): Int = when (stat.uppercase()) {
        "FUE" -> caract.fueOverrideMod ?: Modifiers.modifier(caract.fue)
        "DES" -> caract.desOverrideMod ?: Modifiers.modifier(caract.des)
        "CON" -> caract.conOverrideMod ?: Modifiers.modifier(caract.con)
        "INT" -> caract.intOverrideMod ?: Modifiers.modifier(caract.int_)
        "SAB" -> caract.sabOverrideMod ?: Modifiers.modifier(caract.sab)
        "CAR" -> caract.carOverrideMod ?: Modifiers.modifier(caract.car)
        else -> 0
    }

    /** Por nivel de conjuro: nivel → (espacios libres, total). */
    fun getSlotSummary(): List<Pair<Int, Pair<Int, Int>>> {
        val state = _uiState.value
        val espacios = state.espacios
        val conjuros = state.conjurosPersonaje
        return espacios.map { esp ->
            val gastados = conjuros.filter { it.nivel == esp.nivel }.sumOf { it.gastadoVeces }
            val total = esp.override_ ?: esp.total
            val libres = total - gastados
            esp.nivel to (maxOf(0, libres) to total)
        }
    }

    fun isOverPrepared(nivel: Int): Boolean {
        val state = _uiState.value
        val espacio = state.espacios.firstOrNull { it.nivel == nivel } ?: return false
        val total = espacio.override_ ?: espacio.total
        val preparados = state.conjurosPersonaje.filter { it.nivel == nivel }.sumOf { it.preparadoVeces }
        return preparados > total
    }

    private fun showSnackbar(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private companion object {
        const val KEY_SELECTED_PERSONAJE = "selected_personaje_id"
    }
}
