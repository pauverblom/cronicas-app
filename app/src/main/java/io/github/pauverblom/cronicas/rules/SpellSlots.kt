package io.github.pauverblom.cronicas.rules

object SpellSlots {

    fun freeSlots(total: Int, usedByPrepared: Int): Int = total - usedByPrepared

    fun isOverPrepared(total: Int, preparedCount: Int): Boolean = preparedCount > total
}
