package io.github.pauverblom.cronicas.ui.conjuros

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pauverblom.cronicas.data.Conjuro
import io.github.pauverblom.cronicas.data.ConjuroConDetalle
import io.github.pauverblom.cronicas.ui.MainViewModel
import io.github.pauverblom.cronicas.ui.theme.ArcaneBlue
import io.github.pauverblom.cronicas.ui.theme.ArcanePurple
import io.github.pauverblom.cronicas.ui.theme.GoldAccent
import io.github.pauverblom.cronicas.ui.theme.GoldDim
import io.github.pauverblom.cronicas.ui.theme.GoldLight
import io.github.pauverblom.cronicas.ui.theme.HpAmber
import io.github.pauverblom.cronicas.ui.theme.HpNormal
import io.github.pauverblom.cronicas.ui.theme.HpRed
import io.github.pauverblom.cronicas.ui.theme.Parchment
import io.github.pauverblom.cronicas.ui.theme.ParchmentSurface
import io.github.pauverblom.cronicas.ui.theme.ParchmentSurfaceVariant
import io.github.pauverblom.cronicas.ui.theme.SilverDim
import io.github.pauverblom.cronicas.ui.theme.SilverText

@Composable
fun ConjurosScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val allConjuros by viewModel.allConjuros.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showRestConfirm by remember { mutableStateOf(false) }
    var filterLevel by remember { mutableStateOf<Int?>(null) }
    var searchText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            if (content != null) {
                viewModel.importCsv(content)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Pestañas
        Surface(color = ParchmentSurface, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val prepCount = state.conjurosPersonaje.count { it.preparadoVeces > 0 }
                val grimCount = state.conjurosPersonaje.size
                val catCount = allConjuros.size

                listOf(
                    "Preparados ($prepCount)",
                    "Grimorio ($grimCount)",
                    "Catálogo ($catCount)"
                ).forEachIndexed { i, label ->
                    val selected = selectedTab == i
                    Surface(
                        onClick = { selectedTab = i },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) GoldAccent.copy(alpha = 0.15f) else ParchmentSurfaceVariant,
                        border = if (selected) BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)) else null
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) GoldAccent else SilverDim
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> PreparedSpellsTab(
                viewModel,
                state.conjurosPersonaje,
                onRest = { showRestConfirm = true }
            )
            1 -> GrimoireTab(viewModel, state.conjurosPersonaje)
            2 -> CatalogTab(
                viewModel = viewModel,
                allConjuros = allConjuros,
                grimoireIds = state.conjurosPersonaje.map { it.conjuroId }.toSet(),
                onImportCsv = { csvLauncher.launch(arrayOf("text/*", "application/csv")) },
                filterLevel = filterLevel,
                onFilterLevel = { filterLevel = it },
                searchText = searchText,
                onSearchText = { searchText = it }
            )
        }
    }

    if (showRestConfirm) {
        AlertDialog(
            onDismissRequest = { showRestConfirm = false },
            icon = { Icon(Icons.Default.Bedtime, contentDescription = null, tint = ArcaneBlue) },
            title = { Text("Descanso", color = GoldAccent) },
            text = { Text("¿Restablecer todos los conjuros gastados?", color = SilverText) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.descansar()
                    showRestConfirm = false
                }) {
                    Text("Descansar", color = GoldAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestConfirm = false }) {
                    Text("Cancelar", color = SilverDim)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = ParchmentSurface
        )
    }
}

// ── Pestaña: preparados ──

@Composable
fun PreparedSpellsTab(viewModel: MainViewModel, conjuros: List<ConjuroConDetalle>, onRest: () -> Unit) {
    val prepared = conjuros.filter { it.preparadoVeces > 0 }
    val grouped = prepared.groupBy { it.nivel }

    Column(modifier = Modifier.fillMaxSize()) {
        if (prepared.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = SilverDim
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Sin conjuros preparados", color = SilverDim, fontSize = 16.sp)
                    Text(
                        "Ve al Grimorio para preparar conjuros",
                        color = SilverDim.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.toSortedMap().forEach { (nivel, spells) ->
                    item(key = "header_$nivel") {
                        SpellLevelHeader(nivel, viewModel)
                    }
                    items(spells, key = { "prep_${it.conjuroId}" }) { spell ->
                        PreparedSpellCard(spell, viewModel)
                    }
                }
            }
        }

        // Botón de descanso fijo abajo
        Surface(color = ParchmentSurface, tonalElevation = 4.dp) {
            Button(
                onClick = onRest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArcaneBlue.copy(alpha = 0.2f),
                    contentColor = ArcaneBlue
                )
            ) {
                Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Descanso", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun SpellLevelHeader(nivel: Int, viewModel: MainViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = ArcanePurple.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, ArcanePurple.copy(alpha = 0.3f))
        ) {
            Text(
                "Nivel $nivel",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = ArcanePurple,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.weight(1f))

        if (viewModel.isOverPrepared(nivel)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = HpRed.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = HpRed)
                    Spacer(Modifier.width(4.dp))
                    Text("Exceso", color = HpRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PreparedSpellCard(spell: ConjuroConDetalle, viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurfaceVariant),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            spell.nombre,
                            color = Parchment,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (spell.verificar) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = "Verificar",
                                modifier = Modifier.size(16.dp),
                                tint = HpAmber
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    // Una casilla por preparación; las gastadas se marcan en rojo
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(spell.preparadoVeces) { i ->
                            val used = i < spell.gastadoVeces
                            Surface(
                                modifier = Modifier.size(24.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = if (used) HpRed.copy(alpha = 0.3f) else GoldAccent.copy(alpha = 0.2f),
                                border = BorderStroke(
                                    1.dp,
                                    if (used) HpRed.copy(alpha = 0.5f) else GoldAccent.copy(alpha = 0.4f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (used) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = HpRed.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    Text(
                        buildString {
                            append("TL ${spell.tl}")
                            if (spell.alcance.isNotBlank()) append("  ·  ${spell.alcance}")
                            if (spell.duracion.isNotBlank()) append("  ·  ${spell.duracion}")
                            if (spell.ts.isNotBlank() && spell.ts != "No") append("  ·  TS: ${spell.ts}")
                            if (spell.rc.isNotBlank() && spell.rc != "No") append("  ·  RC: ${spell.rc}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SilverDim,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { viewModel.lanzarConjuro(spell.conjuroId) },
                    enabled = spell.gastadoVeces < spell.preparadoVeces,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcanePurple.copy(alpha = 0.25f),
                        contentColor = ArcanePurple,
                        disabledContainerColor = SilverDim.copy(alpha = 0.1f),
                        disabledContentColor = SilverDim.copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lanzar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(visible = expanded) {
                SpellDetails(spell)
            }

            ExpandToggle(expanded = expanded, onToggle = { expanded = !expanded })
        }
    }
}

// ── Pestaña: grimorio ──

@Composable
fun GrimoireTab(viewModel: MainViewModel, conjuros: List<ConjuroConDetalle>) {
    val grouped = conjuros.groupBy { it.nivel }

    if (conjuros.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Grimorio vacío", color = SilverDim)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.toSortedMap().forEach { (nivel, spells) ->
            item(key = "grim_header_$nivel") {
                SpellLevelHeader(nivel, viewModel)
            }
            items(spells, key = { "grim_${it.conjuroId}" }) { spell ->
                GrimoireSpellCard(spell, viewModel)
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun GrimoireSpellCard(spell: ConjuroConDetalle, viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurfaceVariant),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            spell.nombre,
                            color = Parchment,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (spell.verificar) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = "Verificar",
                                modifier = Modifier.size(16.dp),
                                tint = HpAmber
                            )
                        }
                    }
                    Text(
                        "Preparado ${spell.preparadoVeces}× · Gastado ${spell.gastadoVeces}×",
                        color = SilverDim,
                        fontSize = 12.sp
                    )
                }

                // Contador de preparaciones
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ParchmentSurface
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (spell.preparadoVeces > 0) {
                            IconButton(
                                onClick = { viewModel.desprepararConjuro(spell.conjuroId) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Quitar",
                                    modifier = Modifier.size(18.dp),
                                    tint = HpRed.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Text(
                            "${spell.preparadoVeces}",
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(
                            onClick = { viewModel.prepararConjuro(spell.conjuroId) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Preparar",
                                modifier = Modifier.size(18.dp),
                                tint = HpNormal
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                SpellDetails(spell)
            }

            ExpandToggle(expanded = expanded, onToggle = { expanded = !expanded })
        }
    }
}

/** Descripción y nota táctica desplegables (compartido por las tarjetas de preparados y grimorio). */
@Composable
private fun SpellDetails(spell: ConjuroConDetalle) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        HorizontalDivider(color = GoldDim.copy(alpha = 0.15f))
        Spacer(Modifier.height(8.dp))

        if (spell.descripcion.isNotBlank()) {
            Text(
                spell.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = SilverText,
                lineHeight = 18.sp
            )
        }

        if (spell.notaTactica.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GoldAccent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        "Cómo usarlo",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        spell.notaTactica,
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldLight.copy(alpha = 0.8f),
                        fontStyle = FontStyle.Italic,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

/** Botón "Más info / Menos" bajo cada tarjeta de conjuro. */
@Composable
private fun ExpandToggle(expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(
            onClick = onToggle,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = SilverDim
            )
            Text(if (expanded) "Menos" else "Más info", color = SilverDim, fontSize = 12.sp)
        }
    }
}

// ── Pestaña: catálogo ──

@Composable
fun CatalogTab(
    viewModel: MainViewModel,
    allConjuros: List<Conjuro>,
    grimoireIds: Set<Long>,
    onImportCsv: () -> Unit,
    filterLevel: Int?,
    onFilterLevel: (Int?) -> Unit,
    searchText: String,
    onSearchText: (String) -> Unit
) {
    val filtered = allConjuros.filter { c ->
        (filterLevel == null || c.nivel == filterLevel) &&
            (searchText.isBlank() || c.nombre.contains(searchText, ignoreCase = true))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchText,
            label = { Text("Buscar conjuro") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SilverDim) },
            trailingIcon = {
                if (searchText.isNotBlank()) {
                    IconButton(onClick = { onSearchText("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = SilverDim)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                cursorColor = GoldAccent,
                focusedBorderColor = GoldAccent,
                unfocusedBorderColor = GoldDim.copy(alpha = 0.3f),
                focusedLabelColor = GoldAccent
            )
        )

        // Filtro por nivel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = filterLevel == null,
                    onClick = { onFilterLevel(null) },
                    label = { Text("Todos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldAccent.copy(alpha = 0.15f),
                        selectedLabelColor = GoldAccent
                    )
                )
            }
            for (level in 0..9) {
                item {
                    FilterChip(
                        selected = filterLevel == level,
                        onClick = { onFilterLevel(level) },
                        label = { Text("N$level") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArcanePurple.copy(alpha = 0.15f),
                            selectedLabelColor = ArcanePurple
                        )
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filtered, key = { "cat_${it.id}" }) { conjuro ->
                val inGrimoire = conjuro.id in grimoireIds
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (inGrimoire) ArcaneBlue.copy(alpha = 0.06f) else ParchmentSurfaceVariant
                    ),
                    border = if (inGrimoire) BorderStroke(1.dp, ArcaneBlue.copy(alpha = 0.2f)) else null
                ) {
                    ListItem(
                        headlineContent = {
                            Text(conjuro.nombre, color = Parchment, fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            Text(
                                "Nv ${conjuro.nivel} · TL ${conjuro.tl}" +
                                    if (conjuro.alcance.isBlank()) "" else " · ${conjuro.alcance}",
                                color = SilverDim,
                                fontSize = 12.sp
                            )
                        },
                        trailingContent = {
                            if (inGrimoire) {
                                IconButton(onClick = { viewModel.removeFromGrimoire(conjuro.id) }) {
                                    Icon(Icons.Default.BookmarkRemove, contentDescription = "Quitar", tint = ArcaneBlue)
                                }
                            } else {
                                IconButton(onClick = { viewModel.addToGrimoire(conjuro.id) }) {
                                    Icon(Icons.Default.BookmarkAdd, contentDescription = "Añadir", tint = SilverDim)
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        Surface(color = ParchmentSurface, tonalElevation = 4.dp) {
            Button(
                onClick = onImportCsv,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldDim.copy(alpha = 0.25f),
                    contentColor = GoldLight
                )
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Importar CSV", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}
