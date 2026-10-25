package io.github.pauverblom.cronicas.data

/** Arma predefinida que se puede añadir a cualquier personaje. */
data class CatalogWeapon(
    val nombre: String,
    val formulaDano: String,
    val tipo: String = "cuerpo",
    val notas: String = "",
    val ve: Int = 0,
    val baExtra: Int = 0
) {
    fun toArma(personajeId: Long) = Arma(
        personajeId = personajeId,
        nombre = nombre,
        baExtra = baExtra,
        formulaDano = formulaDano,
        tipo = tipo,
        notas = notas,
        ve = ve
    )
}

/** Catálogo de armas. Para añadir más, basta con ampliar la lista. */
object WeaponCatalog {
    val armas = listOf(
        CatalogWeapon(nombre = "Espada corta", formulaDano = "1d6", tipo = "cuerpo", ve = 2),
        CatalogWeapon(nombre = "Látigo", formulaDano = "1d3", tipo = "cuerpo", notas = "Alcance 4 m (+DES)", ve = 2)
    )
}
