package com.example.cronicasmarca.undo

import com.example.cronicasmarca.data.AccionUndo
import com.example.cronicasmarca.data.Repository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class UndoPayload {
    @Serializable
    data class PgChange(val previousPg: Int) : UndoPayload()

    @Serializable
    data class ConjuroGastado(val conjuroId: Long, val previousGastado: Int) : UndoPayload()

    @Serializable
    data class Descanso(val conjurosGastados: Map<Long, Int>) : UndoPayload()
}

/** Pila de acciones deshacibles, persistida en la tabla accion_undo (máx. 30 por personaje). */
class UndoManager(private val repo: Repository) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun pushPgChange(pid: Long, previousPg: Int) {
        val payload = json.encodeToString<UndoPayload>(UndoPayload.PgChange(previousPg))
        repo.pushUndo(AccionUndo(personajeId = pid, tipo = "pg_change", payloadJson = payload))
    }

    suspend fun pushConjuroGastado(pid: Long, conjuroId: Long, previousGastado: Int) {
        val payload = json.encodeToString<UndoPayload>(UndoPayload.ConjuroGastado(conjuroId, previousGastado))
        repo.pushUndo(AccionUndo(personajeId = pid, tipo = "conjuro_gastado", payloadJson = payload))
    }

    suspend fun pushDescanso(pid: Long, conjurosGastados: Map<Long, Int>) {
        val payload = json.encodeToString<UndoPayload>(UndoPayload.Descanso(conjurosGastados))
        repo.pushUndo(AccionUndo(personajeId = pid, tipo = "descanso", payloadJson = payload))
    }

    /** Deshace la última acción del personaje. Devuelve un mensaje para el usuario, o null si no había nada. */
    suspend fun undo(pid: Long): String? {
        val action = repo.popUndo(pid) ?: return null
        return when (action.tipo) {
            "pg_change" -> {
                val payload = json.decodeFromString<UndoPayload.PgChange>(action.payloadJson)
                repo.updatePg(pid, payload.previousPg)
                "PG restaurados a ${payload.previousPg}"
            }
            "conjuro_gastado" -> {
                val payload = json.decodeFromString<UndoPayload.ConjuroGastado>(action.payloadJson)
                // FIXME: heredado del APK original. Esto sobrescribe preparadoVeces con el valor
                // anterior de gastadoVeces; probablemente solo debería restaurarse gastadoVeces
                // (lo que ya hace el upsert de abajo).
                repo.updatePreparado(pid, payload.conjuroId, payload.previousGastado)
                val cp = repo.getConjuroPersonaje(pid, payload.conjuroId)
                if (cp != null) {
                    repo.upsertConjuroPersonaje(cp.copy(gastadoVeces = payload.previousGastado))
                }
                "Uso de conjuro deshecho"
            }
            "descanso" -> {
                val payload = json.decodeFromString<UndoPayload.Descanso>(action.payloadJson)
                for ((cid, gastado) in payload.conjurosGastados) {
                    val cp = repo.getConjuroPersonaje(pid, cid)
                    if (cp != null) {
                        repo.upsertConjuroPersonaje(cp.copy(gastadoVeces = gastado))
                    }
                }
                "Descanso deshecho"
            }
            else -> null
        }
    }
}
