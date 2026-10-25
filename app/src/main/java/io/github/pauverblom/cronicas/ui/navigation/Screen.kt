package io.github.pauverblom.cronicas.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val label: String, val icon: ImageVector) {
    Mesa("Mesa", Icons.Default.Dashboard),
    Conjuros("Conjuros", Icons.Default.AutoStories),
    Combate("Combate", Icons.Default.Gavel),
    Equipo("Equipo", Icons.Default.Backpack),
    Notas("Notas", Icons.AutoMirrored.Filled.StickyNote2)
}
