package io.github.pauverblom.cronicas.data.sync

/** Códigos de sincronización: 12 letras y números, la única llave de un personaje en la nube. */
object Codigos {
    /** Sin 0/O, 1/I/L, para que se pueda dictar en la mesa sin confusiones. */
    private const val ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val LARGO = 12

    /** Un código nuevo; [azar] devuelve un número aleatorio criptográfico en `0 until n`. */
    fun nuevo(azar: (n: Int) -> Int): String = (1..LARGO).map { ALFABETO[azar(ALFABETO.length)] }.joinToString("")

    /** Mayúsculas y sin guiones ni espacios: «k7qm-2xpa-...» → «K7QM2XPA...». */
    fun normalizar(codigo: String): String = codigo.uppercase().filter { it.isLetterOrDigit() }

    /** «K7QM2XPA9RTE» → «K7QM-2XPA-9RTE». */
    fun formatear(codigo: String): String = codigo.chunked(4).joinToString("-")
}
