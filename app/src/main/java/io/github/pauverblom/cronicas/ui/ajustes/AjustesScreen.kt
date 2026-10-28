package io.github.pauverblom.cronicas.ui.ajustes

import io.github.pauverblom.cronicas.ui.plataforma.materialYouDisponible
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pauverblom.cronicas.data.AjustesApp
import io.github.pauverblom.cronicas.data.Modo
import io.github.pauverblom.cronicas.data.Tema
import io.github.pauverblom.cronicas.ui.hud.BotonIcono
import io.github.pauverblom.cronicas.ui.hud.Cabecera
import io.github.pauverblom.cronicas.ui.hud.Etiqueta
import io.github.pauverblom.cronicas.ui.hud.Filete
import io.github.pauverblom.cronicas.ui.hud.Opcion
import io.github.pauverblom.cronicas.ui.hud.TituloPantalla
import io.github.pauverblom.cronicas.ui.hud.Tono
import io.github.pauverblom.cronicas.ui.hud.Ventana
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos

@Composable
fun AjustesScreen(onVolver: () -> Unit, vm: AjustesViewModel = viewModel()) {
    val ajustes by vm.ajustes.collectAsState(initial = AjustesApp())
    val c = Marca.colores
    Column(Modifier.fillMaxSize()) {
        Cabecera(navegacion = {
            BotonIcono(onVolver, "Volver") { Icon(Icons.AutoMirrored.Sharp.ArrowBack, null, Modifier.size(24.dp)) }
        }) { TituloPantalla("Ajustes") }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 22.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Ventana("Tema", Modifier.widthIn(max = 560.dp)) {
                Tema.entries.forEachIndexed { i, t ->
                    if (i > 0) Filete()
                    val disponible = t != Tema.MATERIAL_YOU || materialYouDisponible()
                    Opcion(ajustes.tema == t, { vm.setTema(t) }, Modifier.fillMaxWidth(), activa = disponible) {
                        Column(Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp)) {
                            Text(t.etiqueta, style = Tipos.ui, color = if (disponible) c.tinta else c.tintaTenue)
                            Text(
                                if (disponible) t.descripcion else "Necesita Android 12 o superior",
                                style = Tipos.caption,
                                color = c.tintaSuave
                            )
                        }
                    }
                }
            }
            Ventana("Modo", Modifier.widthIn(max = 560.dp)) {
                val fijo = ajustes.tema == Tema.PERGAMINO_OSCURO || ajustes.tema == Tema.PERGAMINO_CLARO
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                    Modo.entries.forEach { m ->
                        Etiqueta(
                            m.etiqueta,
                            tono = if (ajustes.modo == m && !fijo) Tono.ORO else Tono.LISO,
                            onClick = if (fijo) null else ({ vm.setModo(m) })
                        )
                    }
                }
                if (fijo) {
                    Text(
                        "Los temas pergamino tienen su propio modo claro u oscuro.",
                        style = Tipos.cuerpoSm,
                        color = c.tintaSuave,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
