package io.github.pauverblom.cronicas.ui.hoja

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.pauverblom.cronicas.data.Nota
import io.github.pauverblom.cronicas.ui.common.ConfirmarDialog
import io.github.pauverblom.cronicas.ui.common.FormularioDialog
import io.github.pauverblom.cronicas.ui.hud.BotonIcono
import io.github.pauverblom.cronicas.ui.hud.Campo
import io.github.pauverblom.cronicas.ui.hud.EstadoVacio
import io.github.pauverblom.cronicas.ui.hud.Glifo
import io.github.pauverblom.cronicas.ui.hud.IconoGlifo
import io.github.pauverblom.cronicas.ui.hud.MedallonFlotante
import io.github.pauverblom.cronicas.ui.hud.Placa
import io.github.pauverblom.cronicas.ui.theme.Marca
import io.github.pauverblom.cronicas.ui.theme.Tipos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotasTab(ui: HojaUi, vm: HojaViewModel) {
    var editando by remember { mutableStateOf<Nota?>(null) }
    var borrando by remember { mutableStateOf<Nota?>(null) }
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES")) }
    val c = Marca.colores

    Box(Modifier.fillMaxSize()) {
        if (ui.notas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EstadoVacio(null, "Sin notas. Pulsa + para apuntar algo de la partida.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(320.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ui.notas, key = { it.id }) { n ->
                    Placa(Modifier.fillMaxWidth(), onClick = { editando = n }) {
                        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    n.titulo.ifBlank { "Sin título" },
                                    style = Tipos.encabezado.copy(letterSpacing = Tipos.etiquetaCaps.letterSpacing),
                                    color = c.oro,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                BotonIcono({ borrando = n }, "Eliminar nota") { IconoGlifo(Glifo.CERRAR, tamano = 16.dp, color = c.tintaSuave) }
                            }
                            if (n.contenido.isNotBlank()) {
                                Text(n.contenido, style = Tipos.cuerpo, color = c.tinta, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 12.dp))
                            }
                            Text(formato.format(Date(n.timestamp)), style = Tipos.caption, color = c.tintaSuave, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
        MedallonFlotante(
            Glifo.MAS, "Nueva nota",
            onClick = { editando = Nota(personajeId = ui.personaje.id) },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(24.dp)
        )
    }

    editando?.let { n ->
        var titulo by remember(n) { mutableStateOf(n.titulo) }
        var contenido by remember(n) { mutableStateOf(n.contenido) }
        FormularioDialog(
            if (n.id == 0L) "Nueva nota" else "Editar nota",
            onDismiss = { editando = null },
            guardarActivo = titulo.isNotBlank() || contenido.isNotBlank(),
            onGuardar = { vm.guardarNota(n.copy(titulo = titulo, contenido = contenido)); editando = null }
        ) {
            Campo(titulo, { titulo = it }, "Título")
            Campo(contenido, { contenido = it }, "Contenido", unaLinea = false, minLineas = 7)
        }
    }
    borrando?.let { n ->
        ConfirmarDialog("Eliminar nota", "¿Eliminar «${n.titulo.ifBlank { "Sin título" }}»?", onDismiss = { borrando = null }) { vm.eliminarNota(n.id) }
    }
}
