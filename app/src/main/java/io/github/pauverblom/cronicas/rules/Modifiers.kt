package io.github.pauverblom.cronicas.rules

object Modifiers {

    private val defaultTable: Map<IntRange, Int> = mapOf(
        3..3 to -3,
        4..5 to -2,
        6..8 to -1,
        9..12 to 0,
        13..15 to 1,
        16..17 to 2,
        18..18 to 3
    )

    private var customTable: Map<Int, Int>? = null

    fun loadCustomTable(table: Map<Int, Int>) {
        customTable = table
    }

    fun clearCustomTable() {
        customTable = null
    }

    fun modifier(abilityScore: Int): Int {
        customTable?.let { return it[abilityScore] ?: 0 }
        return defaultTable.entries.firstOrNull { abilityScore in it.key }?.value ?: 0
    }
}
