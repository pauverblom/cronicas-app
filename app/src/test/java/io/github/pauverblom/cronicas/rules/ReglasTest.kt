package io.github.pauverblom.cronicas.rules

import io.github.pauverblom.cronicas.data.Caracteristicas
import io.github.pauverblom.cronicas.data.Compania
import io.github.pauverblom.cronicas.data.EspacioOverride
import io.github.pauverblom.cronicas.data.Estado
import io.github.pauverblom.cronicas.data.Objeto
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.Repository
import io.github.pauverblom.cronicas.data.Tesoro
import io.github.pauverblom.cronicas.rules.data.Catalogo
import io.github.pauverblom.cronicas.rules.data.Clases
import io.github.pauverblom.cronicas.rules.data.TablaConjuros
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModifiersTest {
    @Test
    fun extremosDeLaTabla() {
        assertEquals(-4, Modifiers.modifier(1))
        assertEquals(-3, Modifiers.modifier(2))
        assertEquals(-3, Modifiers.modifier(3))
        assertEquals(-2, Modifiers.modifier(5))
        assertEquals(-1, Modifiers.modifier(8))
        assertEquals(0, Modifiers.modifier(9))
        assertEquals(0, Modifiers.modifier(12))
        assertEquals(1, Modifiers.modifier(13))
        assertEquals(2, Modifiers.modifier(17))
        assertEquals(3, Modifiers.modifier(18))
        assertEquals(3, Modifiers.modifier(19))
    }
}

class ProgresionTest {
    @Test
    fun baPorClase() {
        assertEquals(1, Clases.guerrero.baEnNivel(1))
        assertEquals(12, Clases.guerrero.baEnNivel(15)) // no sube tras el 12
        assertEquals(2, Clases.mago.baEnNivel(7))
        assertEquals(0, Clases.ladron.baEnNivel(1))
        assertEquals(4, Clases.ladron.baEnNivel(12))
        assertEquals(6, Clases.clerigo.baEnNivel(12))
    }

    @Test
    fun xpYNivel() {
        assertEquals(0, Clases.guerrero.xpParaNivel(1))
        assertEquals(2001, Clases.guerrero.xpParaNivel(2))
        assertEquals(1_000_001, Clases.guerrero.xpParaNivel(12))
        assertEquals(1_250_001, Clases.guerrero.xpParaNivel(13))
        assertEquals(1, Progresion.nivelPorXp(Clases.guerrero, null, 2000))
        assertEquals(2, Progresion.nivelPorXp(Clases.guerrero, null, 2001))
        assertEquals(5, Progresion.nivelPorXp(Clases.guerrero, null, 20000))
    }

    @Test
    fun armonicoDeHuesoSubeComoMagoYAtacaComoLadron() {
        val a = Clases.porId("armonico_hueso")!!
        assertEquals(Clases.mago.xpParaNivel(5), a.xpParaNivel(5))
        assertEquals(Clases.ladron.baEnNivel(9), a.baEnNivel(9))
        assertEquals(6, a.dado)
        assertEquals(Atributo.CAR, a.principal)
    }

    @Test
    fun xpMulticlaseSumaMitadDeLaSecundaria() {
        // Guerrero 2001 + mago 2601 / 2 = 3301
        assertEquals(3301, Progresion.xpParaNivel(Clases.guerrero, Clases.mago, 2))
        assertEquals(1, Progresion.nivelSecundario(1))
        assertEquals(3, Progresion.nivelSecundario(7))
    }

    @Test
    fun multiclaseUsaElMayorBaYElDadoCombinado() {
        // Guerrero 5 = +5; mago 2 = +1
        assertEquals(5, Progresion.ba(Clases.guerrero, Clases.mago, 5))
        assertEquals(6 to 2, Progresion.dado(Clases.guerrero, Clases.mago)) // d10 + d4
        assertEquals(12 to 3, Progresion.dado(Clases.barbaro, Clases.mistico))
    }

    @Test
    fun pgAlSubir() {
        assertEquals(7, Progresion.pgAlSubir(2, tirada = 5, modCon = 2, pgFijos = 4))
        assertEquals(1, Progresion.pgAlSubir(3, tirada = 1, modCon = -2, pgFijos = 4)) // mínimo 1
        assertEquals(4, Progresion.pgAlSubir(11, tirada = 9, modCon = 2, pgFijos = 4))
        assertEquals(4, Progresion.pgAlSubir(12, tirada = 0, modCon = 0, pgFijos = 4))
        assertEquals(0, Progresion.pgAlSubir(13, tirada = 9, modCon = 2, pgFijos = 4))
    }
}

class EspaciosConjuroTest {
    @Test
    fun magoNivel5ConInt16() {
        // Base 5/4/2/1 + 1 de nivel 1 + 1 de nivel 2
        assertEquals(listOf(5, 5, 3, 1, 0, 0, 0, 0, 0, 0), EspaciosConjuro.espacios(TablaConjuros.ARCANA, 5, 16))
    }

    @Test
    fun bonusSoloSiYaPuedeLanzarEseNivel() {
        // Clérigo nivel 1: 3/1; con SAB 18 solo gana el extra de nivel 1
        assertEquals(listOf(3, 2, 0, 0, 0, 0, 0, 0, 0, 0), EspaciosConjuro.espacios(TablaConjuros.DIVINA, 1, 18))
        assertEquals(listOf(4, 2, 0, 0, 0, 0, 0, 0, 0, 0), EspaciosConjuro.espacios(TablaConjuros.ARCANA, 1, 12))
    }
}

class CargaTest {
    @Test
    fun limiteYBandas() {
        assertEquals(10, Carga.limite(10, false, false))
        assertEquals(16, Carga.limite(10, true, true))
        assertEquals(Carga.Banda.LIGERA, Carga.banda(10f, 10))
        assertEquals(Carga.Banda.MEDIA, Carga.banda(10.5f, 10))
        assertEquals(Carga.Banda.MEDIA, Carga.banda(30f, 10))
        assertEquals(Carga.Banda.SOBRECARGADO, Carga.banda(31f, 10))
    }

    @Test
    fun movimientoSegunCarga() {
        assertEquals(10, Carga.movimiento(10, Carga.Banda.LIGERA))
        assertEquals(7, Carga.movimiento(10, Carga.Banda.MEDIA))
        assertEquals(3, Carga.movimiento(6, Carga.Banda.MEDIA))
        assertEquals(3, Carga.movimiento(10, Carga.Banda.SOBRECARGADO))
    }

    @Test
    fun monedas() {
        assertEquals(0, Carga.veMonedas(149))
        assertEquals(1, Carga.veMonedas(150))
        assertEquals(2, Carga.veMonedas(300))
    }
}

class MonedasTest {
    @Test
    fun pagaConMonedasPequenasPrimero() {
        val b = Monedas.Bolsa(cobre = 5, plata = 3, oro = 2)
        assertEquals(Monedas.Bolsa(cobre = 0, plata = 0, oro = 2), Monedas.pagar(b, 35))
    }

    @Test
    fun parteUnaMonedaYDevuelveCambio() {
        // 1 mo para pagar 30 mc: cambio de 7 mp
        assertEquals(Monedas.Bolsa(plata = 7), Monedas.pagar(Monedas.Bolsa(oro = 1), 30))
        // 1 mpt para pagar 15 mo: cambio de 8 mo y 5 mp
        assertEquals(Monedas.Bolsa(plata = 5, oro = 8), Monedas.pagar(Monedas.Bolsa(platino = 1), 150))
    }

    @Test
    fun sinDineroSuficiente() {
        assertNull(Monedas.pagar(Monedas.Bolsa(oro = 1), 101))
    }

    @Test
    fun formato() {
        assertEquals("15 mo", Monedas.formato(1500))
        assertEquals("5 mp", Monedas.formato(50))
        assertEquals("1 mo 5 mp", Monedas.formato(150))
    }
}

class HojaTest {
    private fun personaje(clase: String, especie: String = "humano", nivel: Int = 1) =
        Personaje(id = 1, nombre = "Prueba", claseId = clase, especieId = especie, nivel = nivel)

    private fun caract(fue: Int = 10, des: Int = 10, con: Int = 10, int: Int = 10, sab: Int = 10, car: Int = 10, primarias: Set<Atributo> = emptySet()) =
        Caracteristicas(
            1, fue, des, con, int, sab, car,
            fuePrimaria = Atributo.FUE in primarias, desPrimaria = Atributo.DES in primarias, conPrimaria = Atributo.CON in primarias,
            intPrimaria = Atributo.INT in primarias, sabPrimaria = Atributo.SAB in primarias, carPrimaria = Atributo.CAR in primarias
        )

    private fun objeto(id: String, equipado: Boolean = false, cantidad: Int = 1) =
        Repository.objetoDesde(1, Catalogo.porId(id)!!).copy(id = id.hashCode().toLong(), equipado = equipado).let {
            if (cantidad != 1) it.copy(cantidad = cantidad) else it
        }

    @Test
    fun modificadoresDeEspecieYSalvaciones() {
        // Semiorco: FUE+1, CON+1, CAR-2
        val h = Hoja.calcular(personaje("ladron", "semiorco", nivel = 3), caract(fue = 12, car = 13, des = 16, primarias = setOf(Atributo.DES)), Estado(1))
        assertEquals(13, h.car(Atributo.FUE).puntuacion)
        assertEquals(1, h.mod(Atributo.FUE))
        assertEquals(11, h.car(Atributo.CAR).puntuacion)
        assertEquals(0, h.mod(Atributo.CAR))
        // Salvación: mod + nivel; ND 12 si es primaria, 18 si no
        assertEquals(2 + 3, h.car(Atributo.DES).bonusTirada)
        assertEquals(12, h.car(Atributo.DES).nd)
        assertEquals(18, h.car(Atributo.FUE).nd)
        assertEquals(10, h.movimiento)
    }

    @Test
    fun caConArmaduraEscudoYDes() {
        val objetos = listOf(objeto("cota_mallas", equipado = true), objeto("escudo_medio_metal", equipado = true), objeto("yelmo", equipado = true))
        val h = Hoja.calcular(personaje("guerrero"), caract(des = 13), Estado(1), objetos)
        // 10 + 5 + 1 (DES) + 1 (escudo); el casco solo cuenta contra la cabeza
        assertEquals(17, h.ca.total)
        assertEquals(5, h.ca.casco)
    }

    @Test
    fun bonificadoresMagicosALaCaNoSeAcumulan() {
        val anillo = Objeto(id = 10, personajeId = 1, nombre = "Anillo de protección", categoria = "EQUIPO", ca = 1, equipado = true)
        val capa = Objeto(id = 11, personajeId = 1, nombre = "Capa de protección", categoria = "EQUIPO", ca = 2, equipado = true)
        val h = Hoja.calcular(personaje("mago"), caract(), Estado(1), listOf(anillo, capa))
        assertEquals(12, h.ca.total)
    }

    @Test
    fun caForzadaYOtros() {
        val h = Hoja.calcular(personaje("guerrero"), caract(), Estado(1, caOtros = 2))
        assertEquals(12, h.ca.total)
        val forzada = Hoja.calcular(personaje("guerrero"), caract(), Estado(1, caOverride = 20))
        assertEquals(20, forzada.ca.total)
        assertTrue(forzada.ca.forzada)
    }

    @Test
    fun ataquesCuerpoADistanciaYMinimoCero() {
        val objetos = listOf(objeto("espada_larga"), objeto("arco_corto"), objeto("daga"))
        val h = Hoja.calcular(personaje("guerrero"), caract(fue = 16, des = 4), Estado(1), objetos)
        val espada = h.ataques.first { it.nombre == "Espada larga" }
        assertEquals(1 + 2, espada.lineas[0].ba)
        assertEquals("1d8+2", espada.lineas[0].dano)
        assertEquals("2d4+2", espada.lineas[1].dano) // versátil a dos manos
        val arco = h.ataques.first { it.nombre == "Arco corto" }
        assertEquals(0, arco.lineas.single().ba) // BA 1 + DES -2 => no baja de +0
        assertEquals("1d6", arco.lineas.single().dano) // a distancia no suma FUE
        val daga = h.ataques.first { it.nombre == "Daga" }
        assertEquals(listOf("Cuerpo a cuerpo", "Arrojada"), daga.lineas.map { it.modo })
        assertEquals("1d4+2", daga.lineas[1].dano) // arrojadiza sí suma FUE
        val sinArmas = h.ataques.first()
        assertEquals("1d3+2", sinArmas.lineas.single().dano)
    }

    @Test
    fun armaNoPermitidaPenaliza() {
        val h = Hoja.calcular(personaje("mago", nivel = 5), caract(fue = 13), Estado(1), listOf(objeto("espada_larga")))
        val espada = h.ataques.first { it.nombre == "Espada larga" }
        // BA 1 + FUE 1 - 4 => 0
        assertEquals(0, espada.lineas[0].ba)
        assertTrue(espada.aviso != null)
    }

    @Test
    fun cargaConContenedoresMonturaYMonedas() {
        val mochila = objeto("mochila").copy(id = 100)
        val cuerda = objeto("cuerda_canamo").copy(id = 101, contenedorId = 100)
        val raciones = objeto("racion", cantidad = 5).copy(id = 102)
        val mula = Compania(id = 7, personajeId = 1, nombre = "Mula", capacidadVe = 50)
        val enMula = objeto("pala").copy(id = 103, companiaId = 7)
        val casco = objeto("yelmo_completo", equipado = true).copy(id = 104)
        val h = Hoja.calcular(
            personaje("guerrero"), caract(fue = 12, primarias = setOf(Atributo.FUE)), Estado(1),
            listOf(mochila, cuerda, raciones, enMula, casco), Tesoro(1, oro = 300), listOf(mula)
        )
        // Mochila 2 + raciones 5 + monedas 2; la cuerda va dentro, la pala en la mula y el casco puesto no cuenta
        assertEquals(9f, h.carga.total, 0.001f)
        assertEquals(15, h.carga.limite)
        assertEquals(3f, h.carga.usoContenedores[100L]!!, 0.001f)
        assertEquals(3f, h.carga.usoCompanias[7L]!!, 0.001f)
    }

    @Test
    fun sobrecargadoPierdeDesALaCaYMovimiento() {
        val piedras = Objeto(id = 1, personajeId = 1, nombre = "Piedras", cantidad = 40, ve = 1f)
        val h = Hoja.calcular(personaje("guerrero"), caract(fue = 10, des = 16), Estado(1), listOf(piedras))
        assertEquals(Carga.Banda.SOBRECARGADO, h.carga.banda)
        assertEquals(10, h.ca.total)
        assertEquals(3, h.movimiento)
    }

    @Test
    fun misticoUsaSuTabla() {
        val h = Hoja.calcular(personaje("mistico", nivel = 7), caract(fue = 13), Estado(1))
        assertEquals(14, h.ca.total)
        assertEquals(16, h.movimiento)
        assertEquals("1d10+1", h.ataques.first().lineas.single().dano)
    }

    @Test
    fun espaciosForzadosYXp() {
        val p = personaje("mago", nivel = 5).copy(experiencia = 42501)
        val h = Hoja.calcular(p, caract(int = 16), Estado(1), espaciosForzados = listOf(EspacioOverride(1, 1, 7)))
        assertEquals(listOf(5, 7, 3, 1, 0, 0, 0, 0, 0, 0), h.magia.single().espacios)
        assertEquals(42501, h.xpSiguiente)
        assertTrue(h.puedeSubir)
        assertFalse(Hoja.calcular(personaje("guerrero"), caract(), Estado(1)).esLanzador)
    }
}
