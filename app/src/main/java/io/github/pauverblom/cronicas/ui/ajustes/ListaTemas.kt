package io.github.pauverblom.cronicas.ui.ajustes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pauverblom.cronicas.data.ColoresTema
import io.github.pauverblom.cronicas.data.Tema
import io.github.pauverblom.cronicas.ui.common.MuestrasColores
import io.github.pauverblom.cronicas.ui.hud.Filete
import io.github.pauverblom.cronicas.ui.hud.Opcion
import io.github.pauverblom.cronicas.ui.plataforma.materialYouDisponible
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos

/**
 * Los temas para elegir, de la app o de una ficha. null en [opciones] es «Como la app» (solo en una ficha),
 * con [descripcionApp] como explicación. Con «Personalizado» elegido se muestran sus tres colores para cambiarlos;
 * con «Desde retrato», los del retrato ([coloresRetrato]; sin retrato no se puede elegir).
 */
@Composable
fun ListaTemas(
    opciones: List<Tema?>,
    elegido: Tema?,
    onElegir: (Tema?) -> Unit,
    colores: ColoresTema,
    onColores: (ColoresTema) -> Unit,
    coloresRetrato: ColoresTema? = null,
    descripcionApp: String = ""
) {
    val c = Marca.colores
    opciones.forEachIndexed { i, t ->
        if (i > 0) Filete()
        val disponible = when (t) {
            Tema.MATERIAL_YOU -> materialYouDisponible()
            Tema.RETRATO -> coloresRetrato != null
            else -> true
        }
        val descripcion = when {
            t == null -> descripcionApp
            t == Tema.MATERIAL_YOU && !disponible -> "Necesita Android 12 o superior"
            t == Tema.RETRATO && !disponible -> "Necesita un retrato"
            else -> t.descripcion
        }
        Column(Modifier.fillMaxWidth()) {
            Opcion(elegido == t, { onElegir(t) }, Modifier.fillMaxWidth(), activa = disponible) {
                Column(Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp)) {
                    Text(t?.etiqueta ?: "Como la app", style = Tipos.ui, color = if (disponible) c.tinta else c.tintaTenue)
                    Text(descripcion, style = Tipos.caption, color = c.tintaSuave)
                }
            }
            if (elegido == t && t == Tema.PERSONALIZADO) {
                MuestrasColores(colores, onColores, Modifier.padding(start = 48.dp, top = 4.dp, bottom = 12.dp))
            }
            if (elegido == t && t == Tema.RETRATO && coloresRetrato != null) {
                MuestrasColores(coloresRetrato, null, Modifier.padding(start = 48.dp, top = 4.dp, bottom = 12.dp))
            }
        }
    }
}
