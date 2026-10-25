package com.example.cronicasmarca.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cronicasmarca.ui.MainViewModel
import com.example.cronicasmarca.ui.combate.CombateScreen
import com.example.cronicasmarca.ui.conjuros.ConjurosScreen
import com.example.cronicasmarca.ui.dados.DadosSheet
import com.example.cronicasmarca.ui.equipo.EquipoScreen
import com.example.cronicasmarca.ui.mesa.MesaScreen
import com.example.cronicasmarca.ui.notas.NotasScreen
import com.example.cronicasmarca.ui.theme.GoldAccent
import com.example.cronicasmarca.ui.theme.GoldDim
import com.example.cronicasmarca.ui.theme.Parchment
import com.example.cronicasmarca.ui.theme.ParchmentDark
import com.example.cronicasmarca.ui.theme.ParchmentSurface
import com.example.cronicasmarca.ui.theme.ParchmentSurfaceVariant
import com.example.cronicasmarca.ui.theme.SilverDim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: MainViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(Screen.Mesa) }
    var showDice by remember { mutableStateOf(false) }
    var showCharMenu by remember { mutableStateOf(false) }
    var showNewChar by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Cada mensaje se muestra con la acción "Deshacer".
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undo()
            }
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        containerColor = ParchmentDark,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = ParchmentSurfaceVariant,
                    contentColor = Parchment,
                    actionColor = GoldAccent
                )
            }
        },
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
                                    state.personaje?.nombre ?: "…",
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
                                            if (state.allPersonajes.size > 1) {
                                                IconButton(
                                                    onClick = {
                                                        showCharMenu = false
                                                        viewModel.deleteCharacter(pj)
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
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchCharacter(pj.id)
                                        showCharMenu = false
                                    }
                                )
                            }
                            HorizontalDivider(color = GoldDim.copy(alpha = 0.2f))
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
            when (currentScreen) {
                Screen.Mesa -> MesaScreen(viewModel)
                Screen.Conjuros -> ConjurosScreen(viewModel)
                Screen.Combate -> CombateScreen(viewModel)
                Screen.Equipo -> EquipoScreen(viewModel)
                Screen.Notas -> NotasScreen(viewModel)
            }

            if (showDice) {
                DadosSheet(viewModel = viewModel, onDismiss = { showDice = false })
            }

            if (showNewChar) {
                NewCharacterDialog(
                    onDismiss = { showNewChar = false },
                    onCreate = { nombre, clase, especie, nivel, fue, des, con, int_, sab, car, pgMax, dadoGolpe ->
                        viewModel.createCharacter(nombre, clase, especie, nivel, fue, des, con, int_, sab, car, pgMax, dadoGolpe)
                        showNewChar = false
                    }
                )
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
