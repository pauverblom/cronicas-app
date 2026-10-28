package io.github.pauverblom.cronicas.data.sync

import io.github.pauverblom.cronicas.data.ConjuroExportado
import io.github.pauverblom.cronicas.data.PersonajeExportado
import io.github.pauverblom.cronicas.data.Conjuro
import kotlinx.serialization.json.Json

/**
 * Forma normalizada de una ficha para saber si dos dispositivos tienen el mismo contenido.
 * Los ids de la base de datos son locales de cada dispositivo, así que se cambian por la posición de cada fila
 * (y las referencias entre filas, por esas posiciones). Lo que no se sincroniza (ruta y fecha del retrato,
 * fecha de creación, texto de los conjuros, que es del catálogo de cada dispositivo) se deja fuera.
 */
object Canonica {

    private val json = Json { encodeDefaults = true }

    fun de(e: PersonajeExportado): String {
        val companias = e.companias.withIndex().associate { (i, c) -> c.id to i + 1L }
        val objetos = e.objetos.withIndex().associate { (i, o) -> o.id to i + 1L }
        val nombrePergamino = e.conjurosPergaminos.associate { it.id to it.nombre }
        val pergaminos = nombrePergamino.values.distinct().sorted().withIndex().associate { (i, n) -> n to i + 1L }

        val normalizada = e.copy(
            formato = 0,
            personaje = e.personaje.copy(id = 0, fotoPath = null, creado = 0),
            caracteristicas = e.caracteristicas?.copy(personajeId = 0),
            estado = e.estado?.copy(personajeId = 0),
            tesoro = e.tesoro?.copy(personajeId = 0),
            objetos = e.objetos.map { o ->
                o.copy(
                    id = objetos.getValue(o.id),
                    personajeId = 0,
                    contenedorId = o.contenedorId?.let { objetos[it] ?: -1 },
                    companiaId = o.companiaId?.let { companias[it] ?: -1 },
                    conjuroId = o.conjuroId?.let { nombrePergamino[it]?.let(pergaminos::get) ?: -1 }
                )
            },
            habilidades = e.habilidades.map { it.copy(id = 0, personajeId = 0) },
            condiciones = e.condiciones.map { it.copy(id = 0, personajeId = 0) },
            companias = e.companias.map { it.copy(id = companias.getValue(it.id), personajeId = 0) },
            conjuros = e.conjuros
                .map { ConjuroExportado(Conjuro(nombre = it.conjuro.nombre), it.nivel, it.preparadoVeces, it.usadoVeces) }
                .sortedWith(compareBy({ it.conjuro.nombre }, { it.nivel })),
            espacios = e.espacios.map { it.copy(personajeId = 0) }.sortedBy { it.nivel },
            notas = e.notas.map { it.copy(id = 0, personajeId = 0) },
            retratoBase64 = null,
            conjurosPergaminos = emptyList()
        )
        return json.encodeToString(PersonajeExportado.serializer(), normalizada)
    }
}
