package io.github.pauverblom.cronicas.data

import kotlinx.serialization.Serializable

/** Versión del formato de la ficha exportada y sincronizada. */
const val FORMATO_FICHA = 1

/**
 * Ficha completa de un personaje tal como se exporta a un archivo y se sincroniza: todas sus filas, los conjuros
 * enteros (se enlazan por nombre con el catálogo de cada dispositivo) y, al exportar, el retrato.
 */
@Serializable
data class ConjuroExportado(val conjuro: Conjuro, val nivel: Int, val preparadoVeces: Int, val usadoVeces: Int)

@Serializable
data class PersonajeExportado(
    val formato: Int = FORMATO_FICHA,
    val personaje: Personaje,
    val caracteristicas: Caracteristicas?,
    val estado: Estado?,
    val tesoro: Tesoro?,
    val objetos: List<Objeto>,
    val habilidades: List<Habilidad>,
    val condiciones: List<Condicion>,
    val companias: List<Compania>,
    val conjuros: List<ConjuroExportado>,
    val espacios: List<EspacioOverride>,
    val notas: List<Nota>,
    val retratoBase64: String? = null,
    /** Conjuros guardados en pergaminos, con el id que referencia [Objeto.conjuroId]. */
    val conjurosPergaminos: List<Conjuro> = emptyList()
)
