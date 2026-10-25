package com.example.cronicasmarca.rules

object Encumbrance {

    /** Límite de carga en VE: puntuación de FUE, +3 si FUE es primaria, +3 si CON es primaria. */
    fun carryLimit(fueScore: Int, fuePrimary: Boolean, conPrimary: Boolean): Int {
        var limit = fueScore
        if (fuePrimary) limit += 3
        if (conPrimary) limit += 3
        return limit
    }

    /** Suma de cantidad × VE de cada objeto. */
    fun totalEncumbrance(items: List<Pair<Int, Float>>): Float =
        items.sumOf { (cantidad, ve) -> (cantidad * ve).toDouble() }.toFloat()

    fun isOverEncumbered(totalVE: Float, limit: Int): Boolean = totalVE > limit
}
