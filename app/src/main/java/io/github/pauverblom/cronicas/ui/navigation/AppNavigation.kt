package io.github.pauverblom.cronicas.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.ui.MainViewModel
import io.github.pauverblom.cronicas.ui.combate.CombateScreen
import io.github.pauverblom.cronicas.ui.conjuros.ConjurosScreen
import io.github.pauverblom.cronicas.ui.dados.DadosSheet
import io.github.pauverblom.cronicas.ui.equipo.EquipoScreen
import io.github.pauverblom.cronicas.ui.mesa.MesaScreen
import io.github.pauverblom.cronicas.ui.notas.NotasScreen
import io.github.pauverblom.cronicas.ui.theme.GoldAccent
import io.github.pauverblom.cronicas.ui.theme.GoldDim
import io.github.pauverblom.cronicas.ui.theme.HpRed
import io.github.pauverblom.cronicas.ui.theme.Parchment
import io.github.pauverblom.cronicas.ui.theme.ParchmentDark
import io.github.pauverblom.cronicas.ui.theme.ParchmentSurface
import io.github.pauverblom.cronicas.ui.theme.ParchmentSurfaceVariant
import io.github.pauverblom.cronicas.ui.theme.SilverDim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: MainViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(Screen.Mesa) }
    var showDice by remember { mutableStateOf(false) }
    var showCharMenu by remember { mutableStateOf(false) }
    var showNewChar by remember { mutableStateOf(false) }
    var showEditChar by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Personaje?>(null) }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = ParchmentDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { showCharMenu = true }
                        ) {
                            Text(
                                "CRÓNICAS DE LA MARCA",
                                color = GoldDim,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 3.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    state.personaje?.nombre ?: if (state.loading) "…" else "Sin personaje",
                                    color = GoldAccent,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = GoldAccent
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showCharMenu,
                            onDismissRequest = { showCharMenu = false },
                            containerColor = ParchmentSurfaceVariant
                        ) {
                            state.allPersonajes.forEach { pj ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    pj.nombre,
                                                    color = Parchment,
                                                    fontWeight = if (state.personaje?.id == pj.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (pj.clase.isNotBlank()) {
                                                    Text(pj.clase, color = SilverDim, fontSize = 12.sp)
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    showCharMenu = false
                                                    pendingDelete = pj
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Eliminar",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = SilverDim.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchCharacter(pj.id)
                                        showCharMenu = false
                                    }
                                )
                            }
                            if (state.allPersonajes.isNotEmpty()) {
                                HorizontalDivider(color = GoldDim.copy(alpha = 0.2f))
                            }
                            if (state.personaje != null) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = GoldAccent
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Editar personaje", color = GoldAccent, fontWeight = FontWeight.SemiBold)
                                        }
                                    },
                                    onClick = {
                                        showCharMenu = false
                                        showEditChar = true
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = GoldAccent
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Nuevo personaje", color = GoldAccent, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = {
                                    showCharMenu = false
                                    showNewChar = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ParchmentSurface,
                    titleContentColor = GoldAccent
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ParchmentSurface,
                tonalElevation = 0.dp
            ) {
                Screen.entries.forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(screen.icon, contentDescription = screen.label, modifier = Modifier.size(22.dp))
                        },
                        label = {
                            Text(
                                screen.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldAccent,
                            selectedTextColor = GoldAccent,
                            indicatorColor = GoldAccent.copy(alpha = 0.1f),
                            unselectedIconColor = SilverDim,
                            unselectedTextColor = SilverDim,
                            disabledIconColor = SilverDim.copy(alpha = 0.3f),
                            disabledTextColor = SilverDim.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDice = true },
                shape = CircleShape,
                containerColor = GoldAccent.copy(alpha = 0.15f),
                contentColor = GoldAccent
            ) {
                Icon(Icons.Default.Casino, contentDescription = "Dados", modifier = Modifier.size(26.dp))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (!state.loading && state.allPersonajes.isEmpty()) {
                NoCharacterScreen(onCreate = { showNewChar = true })
            } else when (currentScreen) {
                Screen.Mesa -> MesaScreen(viewModel, onEditCharacter = { showEditChar = true })
                Screen.Conjuros -> ConjurosScreen(viewModel)
                Screen.Combate -> CombateScreen(viewModel)
                Screen.Equipo -> EquipoScreen(viewModel)
                Screen.Notas -> NotasScreen(viewModel)
            }

            if (showDice) {
                DadosSheet(viewModel = viewModel, onDismiss = { showDice = false })
            }

            if (showNewChar) {
                CharacterDialog(
                    title = "Nuevo personaje",
                    confirmLabel = "Crear",
                    onDismiss = { showNewChar = false },
                    onConfirm = { form ->
                        viewModel.createCharacter(form)
                        showNewChar = false
                    }
                )
            }

            if (showEditChar) {
                val initial = viewModel.currentCharacterForm()
                if (initial == null) {
                    showEditChar = false
                } else {
                    CharacterDialog(
                        title = "Editar personaje",
                        confirmLabel = "Guardar",
                        initial = initial,
                        onDismiss = { showEditChar = false },
                        onConfirm = { form ->
                            viewModel.updateCharacter(form)
                            showEditChar = false
                        }
                    )
                }
            }

            pendingDelete?.let { pj ->
                AlertDialog(
                    onDismissRequest = { pendingDelete = null },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = ParchmentSurface,
                    title = { Text("¿Eliminar a ${pj.nombre}?", color = GoldAccent, fontWeight = FontWeight.Bold) },
                    text = { Text("Se borrarán su ficha, conjuros, equipo y notas. No se puede deshacer.", color = Parchment) },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteCharacter(pj)
                            pendingDelete = null
                        }) {
                            Text("Eliminar", color = HpRed, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDelete = null }) {
                            Text("Cancelar", color = SilverDim)
                        }
                    }
                )
            }
        }
    }
}

/** Pantalla de bienvenida cuando todavía no hay ningún personaje. */
@Composable
private fun NoCharacterScreen(onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = GoldDim
            )
            Spacer(Modifier.height(12.dp))
            Text("Aún no hay personajes", color = GoldAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Crea tu personaje para empezar a llevar su ficha.",
                color = SilverDim,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onCreate,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent.copy(alpha = 0.2f),
                    contentColor = GoldAccent
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Crear personaje", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Construction,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = SilverDim.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(8.dp))
            Text(text, color = SilverDim, fontSize = 16.sp)
        }
    }
}
