package com.example.cronicasmarca.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cronicasmarca.CronicasApp
import com.example.cronicasmarca.data.Arma
import com.example.cronicasmarca.data.Caracteristicas
import com.example.cronicasmarca.data.Conjuro
import com.example.cronicasmarca.data.ConjuroConDetalle
import com.example.cronicasmarca.data.ConjuroPersonaje
import com.example.cronicasmarca.data.EfectoActivo
import com.example.cronicasmarca.data.EspaciosConjuro
import com.example.cronicasmarca.data.EstadoCombate
import com.example.cronicasmarca.data.Nota
import com.example.cronicasmarca.data.Objeto
import com.example.cronicasmarca.data.Personaje
import com.example.cronicasmarca.data.Repository
import com.example.cronicasmarca.data.Tesoro
import com.example.cronicasmarca.rules.Dice
import com.example.cronicasmarca.rules.Encumbrance
import com.example.cronicasmarca.rules.Modifiers
import com.example.cronicasmarca.undo.UndoManager
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CronicasApp
    private val repo: Repository = app.repository
    private val undoManager = UndoManager(repo)

    private val _currentPersonajeId = MutableStateFlow(1L)

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

    fun changePg(delta: Int) {
        viewModelScope.launch {
            val current = _uiState.value.combate ?: return@launch
            undoManager.pushPgChange(pid, current.pgActuales)
            val newPg = current.pgActuales + delta
            repo.updatePg(pid, newPg)
            showSnackbar("PG: ${current.pgActuales} → $newPg")
        }
    }

    fun setPg(value: Int) {
        viewModelScope.launch {
            val current = _uiState.value.combate ?: return@launch
            undoManager.pushPgChange(pid, current.pgActuales)
            repo.updatePg(pid, value)
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
                val newPg = combate.pgActuales + 1
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
        _currentPersonajeId.value = newPid
    }

    fun createCharacter(
        nombre: String,
        clase: String,
        especie: String,
        nivel: Int,
        fue: Int,
        des: Int,
        con: Int,
        int_: Int,
        sab: Int,
        car: Int,
        pgMax: Int,
        dadoGolpe: String
    ) {
        viewModelScope.launch {
            val newPid = repo.insertPersonaje(
                Personaje(nombre = nombre, clase = clase, especie = especie, nivel = nivel)
            )
            repo.upsertCaracteristicas(
                Caracteristicas(
                    personajeId = newPid,
                    fue = fue,
                    des = des,
                    con = con,
                    int_ = int_,
                    sab = sab,
                    car = car
                )
            )
            repo.upsertEstadoCombate(
                EstadoCombate(
                    personajeId = newPid,
                    pgActuales = pgMax,
                    pgMaximos = pgMax,
                    dadoGolpe = dadoGolpe
                )
            )
            repo.upsertTesoro(Tesoro(personajeId = newPid))
            _currentPersonajeId.value = newPid
            showSnackbar("$nombre creado")
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
            if (allPjs.size <= 1) {
                showSnackbar("No puedes eliminar el único personaje")
                return@launch
            }
            repo.deletePersonaje(personaje)
            val remaining = allPjs.filter { it.id != personaje.id }
            _currentPersonajeId.value = remaining.first().id
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
}
