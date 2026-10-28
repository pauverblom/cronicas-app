package io.github.pauverblom.cronicas.data

import androidx.room.Embedded

/** Conjuro del grimorio de un personaje junto con los datos del conjuro (el JOIN de [ConjuroDao]). */
data class ConjuroConDetalle(
    @Embedded val cp: ConjuroPersonaje,
    @Embedded(prefix = "c_") val conjuro: ConjuroPlano
)

/** Columnas de [Conjuro] con prefijo para el JOIN. */
data class ConjuroPlano(
    val id: Long,
    val nombre: String,
    val niveles: String,
    val tl: String,
    val componentes: String,
    val alcance: String,
    val duracion: String,
    val ts: String,
    val rc: String,
    val descripcion: String,
    val esPropio: Boolean
) {
    fun toConjuro() = Conjuro(id, nombre, niveles, tl, componentes, alcance, duracion, ts, rc, descripcion, esPropio)
}
