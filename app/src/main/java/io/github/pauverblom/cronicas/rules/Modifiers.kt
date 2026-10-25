package io.github.pauverblom.cronicas.rules

object Modifiers {

    /** Tabla de modificadores por puntuación (reglamento, p. 8). */
    fun modifier(abilityScore: Int): Int = when {
        abilityScore <= 1 -> -4
        abilityScore <= 3 -> -3
        abilityScore <= 5 -> -2
        abilityScore <= 8 -> -1
        abilityScore <= 12 -> 0
        abilityScore <= 15 -> 1
        abilityScore <= 17 -> 2
        else -> 3
    }

    fun format(mod: Int): String = if (mod >= 0) "+$mod" else "$mod"
}
