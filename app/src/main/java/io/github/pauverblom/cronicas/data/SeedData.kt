package io.github.pauverblom.cronicas.data

import android.content.res.AssetManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object SeedData {

    /** Entrada de assets/conjuros.json, generado con scripts/extract_spells.py. */
    @Serializable
    private data class ConjuroJson(
        val nombre: String,
        val clases: Map<String, Int>,
        val tl: String = "",
        val alcance: String = "",
        val componentes: String = "",
        val duracion: String = "",
        val ts: String = "",
        val rc: String = "",
        val descripcion: String = ""
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** Carga el catálogo de conjuros del reglamento si la base de datos aún no lo tiene. */
    suspend fun seedIfEmpty(repo: Repository, assets: AssetManager) {
        if (repo.countConjurosCatalogo() > 0) return
        val text = assets.open("conjuros.json").bufferedReader().use { it.readText() }
        val conjuros = json.decodeFromString<List<ConjuroJson>>(text).map {
            Conjuro(
                nombre = it.nombre,
                niveles = Conjuro.formatNiveles(it.clases),
                tl = it.tl,
                componentes = it.componentes,
                alcance = it.alcance,
                duracion = it.duracion,
                ts = it.ts,
                rc = it.rc,
                descripcion = it.descripcion,
                esPropio = false
            )
        }
        repo.insertConjuros(conjuros)
    }
}
