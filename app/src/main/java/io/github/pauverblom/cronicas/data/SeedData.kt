package io.github.pauverblom.cronicas.data

import android.content.res.AssetManager

object SeedData {

    /** Carga el catálogo de conjuros del reglamento si la base de datos aún no lo tiene. */
    suspend fun seedIfEmpty(repo: Repository, assets: AssetManager) {
        if (repo.countConjurosCatalogo() > 0) return
        val text = assets.open("conjuros.json").bufferedReader().use { it.readText() }
        val conjuros = catalogoConjurosDesdeJson(text)
        repo.insertConjuros(conjuros)
    }
}
