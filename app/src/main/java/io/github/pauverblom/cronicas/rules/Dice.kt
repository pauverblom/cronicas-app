package io.github.pauverblom.cronicas.rules

import java.security.SecureRandom
import java.util.Random

data class SingleRoll(val count: Int, val sides: Int, val results: List<Int>) {
    val subtotal: Int get() = results.sum()
}

data class DiceResult(
    val formula: String,
    val rolls: List<SingleRoll>,
    val constant: Int,
    val total: Int
)

object Dice {
    private val random = SecureRandom()

    private val TERM_REGEX = Regex("([+-]?)(\\d*)d(\\d+|%)")
    private val CONSTANT_REGEX = Regex("([+-])(\\d+)(?!d)")
    private val FULL_FORMULA_REGEX = Regex("^(\\d*d(\\d+|%))(([+-](\\d*d(\\d+|%)|\\d+))*)$")

    /** Devuelve true si la fórmula (p. ej. "2d6+1d4+3") es válida. */
    fun parse(formula: String): Boolean {
        val cleaned = formula.replace(" ", "").lowercase()
        if (cleaned.isEmpty()) return false
        return FULL_FORMULA_REGEX.matches(cleaned)
    }

    fun roll(formula: String, rng: Random = random): DiceResult {
        val cleaned = formula.replace(" ", "").lowercase()
        require(parse(formula)) { "Fórmula de dados inválida: $formula" }

        val normalised = if (cleaned[0] == '+' || cleaned[0] == '-') cleaned else "+$cleaned"

        val rolls = mutableListOf<SingleRoll>()
        var constant = 0

        // Separa la fórmula en términos con su signo: "+2d6", "-1", ...
        val terms = mutableListOf<String>()
        val sb = StringBuilder()
        for (ch in normalised) {
            if ((ch == '+' || ch == '-') && sb.isNotEmpty()) {
                terms.add(sb.toString())
                sb.clear()
            }
            sb.append(ch)
        }
        if (sb.isNotEmpty()) terms.add(sb.toString())

        for (term in terms) {
            val diceMatch = Regex("([+-]?)(\\d*)d(\\d+|%)").find(term)
            if (diceMatch != null) {
                val sign = if (diceMatch.groupValues[1] == "-") -1 else 1
                val count = diceMatch.groupValues[2].let { if (it.isEmpty()) 1 else it.toInt() }
                val sidesStr = diceMatch.groupValues[3]
                val sides = if (sidesStr == "%") 100 else sidesStr.toInt()
                require(count > 0) { "El número de dados debe ser positivo" }
                require(sides > 0) { "El número de caras debe ser positivo" }
                val results = (1..count).map { (rng.nextInt(sides) + 1) * sign }
                rolls.add(SingleRoll(count * sign, sides, results))
            } else {
                val constMatch = Regex("([+-]?)(\\d+)").find(term)
                if (constMatch != null) {
                    val sign = if (constMatch.groupValues[1] == "-") -1 else 1
                    constant += constMatch.groupValues[2].toInt() * sign
                }
            }
        }

        val total = rolls.sumOf { it.subtotal } + constant
        return DiceResult(formula, rolls, constant, total)
    }
}
