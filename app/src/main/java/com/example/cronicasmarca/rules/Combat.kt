package com.example.cronicasmarca.rules

object Combat {

    fun calcCA(
        caBase: Int = 10,
        caArmadura: Int = 0,
        modDes: Int = 0,
        caEquipo: Int = 0,
        caOtros: Int = 0,
        caOverride: Int? = null
    ): Int = caOverride ?: (caBase + caArmadura + modDes + caEquipo + caOtros)

    fun attackBonus(ba: Int, abilityMod: Int, weaponBaExtra: Int = 0): Int =
        maxOf(0, ba + abilityMod + weaponBaExtra)

    fun runSpeed(marchSpeed: Int): Int = marchSpeed * 2

    enum class HpColor { NORMAL, AMBER, RED, GREY }

    fun hpColor(current: Int, max: Int): HpColor {
        if (max <= 0 || current <= 0) return HpColor.GREY
        val ratio = current.toFloat() / max
        return when {
            ratio <= 0.25f -> HpColor.RED
            ratio <= 0.5f -> HpColor.AMBER
            else -> HpColor.NORMAL
        }
    }
}
