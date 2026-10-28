package io.github.pauverblom.cronicas.ui.foto

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.pauverblom.cronicas.ui.common.llamaDe
import io.github.pauverblom.cronicas.ui.hud.Retrato
import io.github.pauverblom.cronicas.ui.plataforma.modeloRetrato

/**
 * Retrato del personaje en su anillo; sin foto, su inicial sobre el fuego de su clase ([claseId]).
 * Con [anillo], el aro arde con ese fuego.
 */
@Composable
fun RetratoAvatar(
    path: String?,
    nombre: String,
    size: Dp,
    modifier: Modifier = Modifier,
    claseId: String? = null,
    anillo: Boolean = false
) {
    // Un retrato cuyo archivo ya no existe muestra la inicial, no un hueco
    val modelo = remember(path) { modeloRetrato(path) }
    Retrato(modelo, nombre, llamaDe(claseId), size, modifier, anillo)
}
