package io.github.pauverblom.cronicas.ui.hoja

import io.github.pauverblom.cronicas.data.Caracteristicas
import io.github.pauverblom.cronicas.data.Compania
import io.github.pauverblom.cronicas.data.Condicion
import io.github.pauverblom.cronicas.data.ConjuroConDetalle
import io.github.pauverblom.cronicas.data.EspacioOverride
import io.github.pauverblom.cronicas.data.Estado
import io.github.pauverblom.cronicas.data.Habilidad
import io.github.pauverblom.cronicas.data.Nota
import io.github.pauverblom.cronicas.data.Objeto
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.Tesoro
import io.github.pauverblom.cronicas.rules.HojaDerivada

/** Todo lo que muestra la ficha de un personaje. */
data class HojaUi(
    val personaje: Personaje,
    val caracteristicas: Caracteristicas,
    val estado: Estado,
    val objetos: List<Objeto>,
    val tesoro: Tesoro,
    val habilidades: List<Habilidad>,
    val condiciones: List<Condicion>,
    val companias: List<Compania>,
    val conjuros: List<ConjuroConDetalle>,
    val espacios: List<EspacioOverride>,
    val notas: List<Nota>,
    val hoja: HojaDerivada
) {
    /** La pestaña Magia aparece para clases lanzadoras o si el personaje ya tiene conjuros. */
    val muestraMagia: Boolean get() = hoja.esLanzador || conjuros.isNotEmpty()
}
