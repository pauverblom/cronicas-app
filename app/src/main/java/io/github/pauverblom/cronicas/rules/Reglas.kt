package io.github.pauverblom.cronicas.rules

import io.github.pauverblom.cronicas.rules.data.ClaseDef
import io.github.pauverblom.cronicas.rules.data.Clases
import io.github.pauverblom.cronicas.rules.data.TablaConjuros

/** Experiencia, dados de golpe y BA (reglamento, cap. 2). */
object Progresion {

    const val NIVEL_MAXIMO = 20

    /** PX para alcanzar [nivel]. Con multiclase se suma la mitad de los PX de la clase secundaria (p. 35). */
    fun xpParaNivel(clase: ClaseDef, secundaria: ClaseDef?, nivel: Int): Int =
        clase.xpParaNivel(nivel) + (secundaria?.xpParaNivel(nivel)?.div(2) ?: 0)

    fun nivelPorXp(clase: ClaseDef, secundaria: ClaseDef?, xp: Int): Int =
        (1..NIVEL_MAXIMO).last { xpParaNivel(clase, secundaria, it) <= xp }

    /** Con multiclase, la clase secundaria va a la mitad del nivel principal. */
    fun nivelSecundario(nivel: Int): Int = maxOf(1, nivel / 2)

    /** Dado de golpe y PG fijos por nivel a partir del 11. */
    fun dado(clase: ClaseDef, secundaria: ClaseDef?): Pair<Int, Int> =
        if (secundaria == null) clase.dado to clase.pgFijos else Clases.dadoMulticlase(clase.dado, secundaria.dado)

    /** Con multiclase se usa el mayor BA de las dos clases. */
    fun ba(clase: ClaseDef, secundaria: ClaseDef?, nivel: Int): Int =
        maxOf(clase.baEnNivel(nivel), secundaria?.baEnNivel(nivelSecundario(nivel)) ?: Int.MIN_VALUE)

    /**
     * PG que se ganan al llegar a [nuevoNivel]: hasta el 10, la tirada del dado + mod. CON (mínimo 1);
     * en el 11 y el 12, los PG fijos de la clase; a partir del 13, nada.
     */
    fun pgAlSubir(nuevoNivel: Int, tirada: Int, modCon: Int, pgFijos: Int): Int = when {
        nuevoNivel <= 10 -> maxOf(1, tirada + modCon)
        nuevoNivel <= 12 -> pgFijos
        else -> 0
    }
}

/** Tiradas de salvación (p. 67): 1d20 + mod. + nivel contra ND 12 (primaria) o 18 (secundaria). */
object Salvaciones {

    val amenazas: Map<Atributo, List<String>> = mapOf(
        Atributo.FUE to listOf("Parálisis", "Constricción", "Aplastamiento"),
        Atributo.DES to listOf("Trampas", "Ataques de aliento"),
        Atributo.CON to listOf("Enfermedad", "Drenaje de energía", "Veneno"),
        Atributo.INT to listOf("Magia arcana", "Ilusiones"),
        Atributo.SAB to listOf("Magia divina", "Confusión", "Ataque de mirada", "Polimorfización", "Petrificación", "Parálisis"),
        Atributo.CAR to listOf("Ataque de muerte", "Encantamiento", "Miedo")
    )

    fun nd(primaria: Boolean): Int = if (primaria) 12 else 18
}

/** Carga (p. 48). */
object Carga {

    enum class Banda(val etiqueta: String) { LIGERA("Ligera"), MEDIA("Media"), SOBRECARGADO("Sobrecargado") }

    /** Límite de carga: puntuación de FUE, +3 si FUE es primaria, +3 si CON es primaria. */
    fun limite(fue: Int, fuePrimaria: Boolean, conPrimaria: Boolean): Int =
        fue + (if (fuePrimaria) 3 else 0) + (if (conPrimaria) 3 else 0)

    /** Cada 150 monedas de cualquier tipo cuentan como 1 VE. */
    fun veMonedas(totalMonedas: Int): Int = totalMonedas / 150

    fun banda(totalVe: Float, limite: Int): Banda = when {
        totalVe <= limite -> Banda.LIGERA
        totalVe <= limite * 3 -> Banda.MEDIA
        else -> Banda.SOBRECARGADO
    }

    /** Media: -3 m (mínimo 3 m). Sobrecargado: 3 m. */
    fun movimiento(base: Int, banda: Banda): Int = when (banda) {
        Banda.LIGERA -> base
        Banda.MEDIA -> maxOf(3, base - 3)
        Banda.SOBRECARGADO -> minOf(3, base)
    }

    fun efectos(banda: Banda): String? = when (banda) {
        Banda.LIGERA -> null
        Banda.MEDIA -> "-2 a las tiradas de DES, -3 m de movimiento"
        Banda.SOBRECARGADO -> "Movimiento 3 m, fallo automático en DES, sin DES a la CA"
    }
}

/** Espacios de conjuro por nivel (pp. 20, 26, 31) más conjuros adicionales por SAB/INT. */
object EspaciosConjuro {

    /** Espacios para los niveles de conjuro 0..9 de un lanzador de [nivel] con [puntuacion] en su característica. */
    fun espacios(tabla: TablaConjuros, nivel: Int, puntuacion: Int): List<Int> {
        val filas = if (tabla == TablaConjuros.DIVINA) Clases.espaciosDivinos else Clases.espaciosArcanos
        val base = filas[nivel.coerceIn(1, filas.size) - 1]
        val out = MutableList(10) { base.getOrElse(it) { 0 } }
        // 13-15: +1 de nivel 1; 16-17: también +1 de nivel 2; 18-19: también +1 de nivel 3.
        // Solo si ya puede lanzar conjuros de ese nivel.
        val extra = when {
            puntuacion >= 18 -> 3
            puntuacion >= 16 -> 2
            puntuacion >= 13 -> 1
            else -> 0
        }
        for (lvl in 1..extra) if (out[lvl] > 0) out[lvl]++
        return out
    }
}

/** Monedas: 1 mpt = 10 mo; 1 mo = 2 me = 10 mp = 100 mc (p. 47). */
object Monedas {

    data class Bolsa(val cobre: Int = 0, val plata: Int = 0, val electro: Int = 0, val oro: Int = 0, val platino: Int = 0) {
        val totalMc: Int get() = cobre + plata * 10 + electro * 50 + oro * 100 + platino * 1000
        val totalMonedas: Int get() = cobre + plata + electro + oro + platino
    }

    private val VALORES = intArrayOf(1, 10, 50, 100, 1000)

    private fun Bolsa.toArray() = intArrayOf(cobre, plata, electro, oro, platino)
    private fun IntArray.toBolsa() = Bolsa(this[0], this[1], this[2], this[3], this[4])

    /**
     * Paga [precioMc] empezando por las monedas pequeñas; si hace falta, parte una moneda mayor
     * y devuelve el cambio en monedas menores. Devuelve null si no hay bastante dinero.
     */
    fun pagar(bolsa: Bolsa, precioMc: Int): Bolsa? {
        if (precioMc <= 0) return bolsa
        if (bolsa.totalMc < precioMc) return null
        val m = bolsa.toArray()
        var resto = precioMc
        for (i in m.indices) {
            val usa = minOf(m[i], resto / VALORES[i])
            m[i] -= usa
            resto -= usa * VALORES[i]
        }
        if (resto > 0) {
            val i = m.indices.first { m[it] > 0 && VALORES[it] > resto }
            m[i]--
            var cambio = VALORES[i] - resto
            for (j in i - 1 downTo 0) {
                if (j == 2) continue // el cambio no se da en electro
                m[j] += cambio / VALORES[j]
                cambio %= VALORES[j]
            }
        }
        return m.toBolsa()
    }

    /** "15 mo", "5 mp", "1 mo 5 mp". */
    fun formato(precioMc: Int): String {
        if (precioMc == 0) return "—"
        val partes = mutableListOf<String>()
        var r = precioMc
        if (r >= 100) { partes += "${r / 100} mo"; r %= 100 }
        if (r >= 10) { partes += "${r / 10} mp"; r %= 10 }
        if (r > 0) partes += "$r mc"
        return partes.joinToString(" ")
    }
}

/** Suma un bonificador a una fórmula de daño: ("1d8", 2) -> "1d8+2". */
fun sumarDano(formula: String, bonus: Int): String = when {
    formula.isBlank() -> if (bonus == 0) "—" else Modifiers.format(bonus)
    bonus == 0 -> formula
    else -> formula + Modifiers.format(bonus)
}
