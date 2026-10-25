package io.github.pauverblom.cronicas.data

import kotlinx.coroutines.flow.first

object SeedData {

    /**
     * Carga el catálogo de conjuros la primera vez. No crea ningún personaje: el usuario crea el suyo.
     * Solo se ejecuta si no hay conjuros de catálogo, porque [SpellCatalog.seedCatalog] borra y
     * reinserta el catálogo (y con él los conjuros de catálogo de los grimorios).
     */
    suspend fun seedIfEmpty(repo: Repository) {
        if (repo.allConjuros().first().any { !it.esPropio }) return
        SpellCatalog.seedCatalog(repo)
    }
}
