package io.github.pauverblom.cronicas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampanasTest {

    @Test
    fun crearYMoverPersonajes() {
        val d = DatosCampanas().crearYMover(1, " La Marca ").crearYMover(2, "Pozasdemugre").mover(3, 1)
        assertEquals(listOf("La Marca", "Pozasdemugre"), d.campanas.map { it.nombre })
        assertEquals("La Marca", d.campanaDe(1)?.nombre)
        assertEquals("La Marca", d.campanaDe(3)?.nombre)
        assertEquals("Pozasdemugre", d.campanaDe(2)?.nombre)
        assertNull(d.campanaDe(4))
    }

    @Test
    fun unaCampanaVaciaDesaparece() {
        val d = DatosCampanas().crearYMover(1, "Una").plegar(1, true).mover(1, null)
        assertTrue(d.campanas.isEmpty())
        assertTrue(d.plegadas.isEmpty())
        // Los ids no se reutilizan
        assertEquals(2L, d.crearYMover(5, "Otra").campanas.single().id)
    }

    @Test
    fun disolverDejaLosPersonajesSinCampana() {
        val d = DatosCampanas().crearYMover(1, "Una").mover(2, 1).crearYMover(3, "Dos").disolver(1)
        assertNull(d.campanaDe(1))
        assertNull(d.campanaDe(2))
        assertEquals(listOf("Dos"), d.campanas.map { it.nombre })
    }

    @Test
    fun moverAUnaCampanaInexistenteLaQuita() {
        val d = DatosCampanas().crearYMover(1, "Una").mover(1, 99)
        assertNull(d.campanaDe(1))
        assertTrue(d.campanas.isEmpty())
    }
}
