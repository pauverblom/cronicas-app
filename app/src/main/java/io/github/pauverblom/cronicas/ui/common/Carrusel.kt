package io.github.pauverblom.cronicas.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Fondo de papel para las ilustraciones (son tinta sobre blanco), igual en tema claro y oscuro. */
private val Papel = Color(0xFFFBF6EA)

/** Ilustración del reglamento sobre fondo de papel; sin imagen muestra la inicial de [nombre]. */
@Composable
fun Ilustracion(uri: String?, nombre: String, alto: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .clip(RoundedCornerShape(12.dp))
            .background(if (uri != null) Papel else MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = nombre,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(alto).padding(8.dp)
            )
        } else {
            Text(
                nombre.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Carrusel de tarjetas de una en una; las vecinas asoman por los lados.
 * [seleccionada] marca la tarjeta elegida; tocar una tarjeta la elige.
 */
@Composable
fun <T> Carrusel(
    estado: PagerState,
    elementos: List<T>,
    seleccionada: (T) -> Boolean,
    onElegir: (T) -> Unit,
    modifier: Modifier = Modifier,
    clave: ((T) -> Any)? = null,
    contenido: @Composable ColumnScope.(T) -> Unit
) {
    Column(modifier) {
        HorizontalPager(
            state = estado,
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 12.dp,
            key = clave?.let { k -> { i: Int -> k(elementos[i]) } }
        ) { i ->
            val e = elementos[i]
            val elegida = seleccionada(e)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onElegir(e) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (elegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                ),
                border = BorderStroke(if (elegida) 2.dp else 1.dp, if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { contenido(e) }
            }
        }
        // Posición: puntos si son pocas tarjetas, contador si son muchas
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (elementos.size > 15) {
                Text(
                    "${estado.currentPage + 1} / ${elementos.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Row
            }
            elementos.forEachIndexed { i, e ->
                val actual = i == estado.currentPage
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (actual) 9.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                seleccionada(e) -> MaterialTheme.colorScheme.primary
                                actual -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }
    }
}

/** Pareja etiqueta/valor compacta para las tarjetas. */
@Composable
fun DatoTarjeta(etiqueta: String, valor: String) {
    Row {
        Text("$etiqueta ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
