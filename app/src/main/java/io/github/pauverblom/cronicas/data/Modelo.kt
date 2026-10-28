package io.github.pauverblom.cronicas.data

import io.github.pauverblom.cronicas.rules.Monedas
import io.github.pauverblom.cronicas.rules.data.ArticuloDef
import io.github.pauverblom.cronicas.rules.data.Clases
import io.github.pauverblom.cronicas.rules.data.Especies
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Lógica de datos sin base de datos: la comparten la app Android y la web.

/** Lo que muestra cada tarjeta de la pantalla de inicio. */
data class ResumenPersonaje(val personaje: Personaje, val estado: Estado?)

fun Tesoro.bolsa() = Monedas.Bolsa(cobre, plata, electro, oro, platino)
fun Tesoro.conBolsa(b: Monedas.Bolsa) = copy(cobre = b.cobre, plata = b.plata, electro = b.electro, oro = b.oro, platino = b.platino)

/** Objeto del inventario de [pid] para [lotes] lotes del artículo [art] del catálogo. */
fun objetoDeCatalogo(pid: Long, art: ArticuloDef, lotes: Int = 1) = Objeto(
    personajeId = pid,
    categoria = art.categoria.name,
    catalogId = art.id,
    nombre = art.nombre,
    cantidad = art.unidades * lotes,
    ve = art.ve / art.unidades,
    capacidad = art.capacidad,
    formulaDano = art.dano.orEmpty(),
    formulaDanoDosManos = art.danoDosManos.orEmpty(),
    tipoArma = art.tipo.name,
    alcance = art.alcance,
    dosManos = art.dosManos,
    ca = art.ca,
    notas = art.notas
)

/**
 * Habilidades de reglamento que sobran y que faltan para la clase, la secundaria y la especie de [p] a su nivel
 * (p. ej. al subir de nivel o cambiar de clase). Las propias no se tocan.
 */
fun habilidadesDeReglamento(p: Personaje, actuales: List<Habilidad>): Pair<List<Habilidad>, List<Habilidad>> {
    val clase = Clases.porId(p.claseId)
    val secundaria = Clases.porId(p.claseSecundariaId)
    val especie = Especies.porId(p.especieId)
    val nivelSec = maxOf(1, p.nivel / 2)
    val deClase = (clase?.habilidades.orEmpty().filter { it.nivel <= p.nivel } +
        secundaria?.habilidades.orEmpty().filter { it.nivel <= nivelSec }).distinctBy { it.nombre }
    val deEspecie = especie?.rasgos.orEmpty()
    val todasClase = (clase?.habilidades.orEmpty() + secundaria?.habilidades.orEmpty()).map { it.nombre }.toSet()

    val sobran = actuales.filter { h ->
        when (h.origen) {
            Habilidad.ORIGEN_CLASE -> h.nombre !in todasClase
            Habilidad.ORIGEN_ESPECIE -> deEspecie.none { it.nombre == h.nombre }
            else -> false
        }
    }
    val existentes = actuales.map { it.origen to it.nombre }.toSet()
    val nuevas = deClase.filter { (Habilidad.ORIGEN_CLASE to it.nombre) !in existentes }
        .map { Habilidad(personajeId = p.id, origen = Habilidad.ORIGEN_CLASE, nombre = it.nombre, atributo = it.atributo?.abrev, descripcion = it.descripcion) } +
        deEspecie.filter { (Habilidad.ORIGEN_ESPECIE to it.nombre) !in existentes }
            .map { Habilidad(personajeId = p.id, origen = Habilidad.ORIGEN_ESPECIE, nombre = it.nombre, atributo = it.atributo?.abrev, descripcion = it.descripcion) }
    return sobran to nuevas
}

/**
 * Conjuros propios desde un CSV con cabecera. Columnas: nombre, clases, tl, componentes, alcance, duracion, ts, rc,
 * descripcion. «clases» usa el formato "mago=1;clerigo=2".
 */
fun conjurosDesdeCsv(csvContent: String): List<Conjuro> {
    val lines = csvContent.lines().filter { it.isNotBlank() }
    if (lines.size < 2) return emptyList()
    return lines.drop(1).mapNotNull { line ->
        val f = parseCsvLine(line).map { it.trim() }
        if (f.size < 2 || f[0].isBlank()) return@mapNotNull null
        Conjuro(
            nombre = f[0],
            niveles = f.getOrElse(1) { "" },
            tl = f.getOrElse(2) { "" },
            componentes = f.getOrElse(3) { "" },
            alcance = f.getOrElse(4) { "" },
            duracion = f.getOrElse(5) { "" },
            ts = f.getOrElse(6) { "" },
            rc = f.getOrElse(7) { "" },
            descripcion = f.getOrElse(8) { "" },
            esPropio = true
        )
    }
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false
    for (ch in line) {
        when {
            ch == '"' -> inQuotes = !inQuotes
            ch == ',' && !inQuotes -> {
                result.add(current.toString())
                current = StringBuilder()
            }
            else -> current.append(ch)
        }
    }
    result.add(current.toString())
    return result
}

/** Entrada de assets/conjuros.json, generado con scripts/extract_spells.py. */
@Serializable
private data class ConjuroJson(
    val nombre: String,
    val clases: Map<String, Int>,
    val tl: String = "",
    val alcance: String = "",
    val componentes: String = "",
    val duracion: String = "",
    val ts: String = "",
    val rc: String = "",
    val descripcion: String = ""
)

private val jsonCatalogo = Json { ignoreUnknownKeys = true }

/** El catálogo de conjuros del reglamento (assets/conjuros.json), en el orden del archivo. */
fun catalogoConjurosDesdeJson(texto: String): List<Conjuro> =
    jsonCatalogo.decodeFromString<List<ConjuroJson>>(texto).map {
        Conjuro(
            nombre = it.nombre,
            niveles = Conjuro.formatNiveles(it.clases),
            tl = it.tl,
            componentes = it.componentes,
            alcance = it.alcance,
            duracion = it.duracion,
            ts = it.ts,
            rc = it.rc,
            descripcion = it.descripcion,
            esPropio = false
        )
    }
