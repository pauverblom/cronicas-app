package io.github.pauverblom.cronicas.rules

/** Las seis características, en el orden de la ficha. */
enum class Atributo(val abrev: String, val nombre: String) {
    FUE("FUE", "Fuerza"),
    DES("DES", "Destreza"),
    CON("CON", "Constitución"),
    INT("INT", "Inteligencia"),
    SAB("SAB", "Sabiduría"),
    CAR("CAR", "Carisma");

    companion object {
        fun fromAbrev(s: String?): Atributo? = entries.firstOrNull { it.abrev.equals(s, ignoreCase = true) }
    }
}
