package io.github.pauverblom.cronicas.ui.hoja

import io.github.pauverblom.cronicas.data.Personaje

/**
 * El personaje que se acaba de tocar en la lista, para que su ficha se pinte con su tema, su retrato y su nombre
 * desde el primer fotograma, sin esperar a cargarla (lo que la transición necesita). Solo vive en memoria: si no
 * está (la app se reabrió en la ficha), la ficha espera a cargarse como siempre.
 */
object VistaPrevia {
    private val personajes = HashMap<Long, Personaje>()

    fun poner(p: Personaje) {
        personajes[p.id] = p
    }

    fun de(pid: Long): Personaje? = personajes[pid]
}
