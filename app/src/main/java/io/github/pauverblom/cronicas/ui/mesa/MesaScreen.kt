package io.github.pauverblom.cronicas.ui.mesa

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pauverblom.cronicas.data.Arma
import io.github.pauverblom.cronicas.data.Caracteristicas
import io.github.pauverblom.cronicas.data.EfectoActivo
import io.github.pauverblom.cronicas.data.EstadoCombate
import io.github.pauverblom.cronicas.rules.Combat
import io.github.pauverblom.cronicas.rules.Modifiers
import io.github.pauverblom.cronicas.ui.DiceRollResult
import io.github.pauverblom.cronicas.ui.MainViewModel
import io.github.pauverblom.cronicas.ui.theme.ArcaneBlue
import io.github.pauverblom.cronicas.ui.theme.ArcanePurple
import io.github.pauverblom.cronicas.ui.theme.ForestGreen
import io.github.pauverblom.cronicas.ui.theme.GoldAccent
import io.github.pauverblom.cronicas.ui.theme.GoldDim
import io.github.pauverblom.cronicas.ui.theme.HpAmber
import io.github.pauverblom.cronicas.ui.theme.HpGrey
import io.github.pauverblom.cronicas.ui.theme.HpNormal
import io.github.pauverblom.cronicas.ui.theme.HpRed
import io.github.pauverblom.cronicas.ui.theme.Parchment
import io.github.pauverblom.cronicas.ui.theme.ParchmentSurface
import io.github.pauverblom.cronicas.ui.theme.SilverDim
import io.github.pauverblom.cronicas.ui.theme.SilverText

@Composable
fun MesaScreen(viewModel: MainViewModel, onEditCharacter: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val combate = state.combate
    val caract = state.caracteristicas
    var saveRollResult by remember { mutableStateOf<DiceRollResult?>(null) }

    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GoldAccent)
        }
        return
    }
    if (combate == null) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 700.dp

        if (isWide) {
            // Tablet / horizontal: dos columnas
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    state.personaje?.let {
                        CharacterHeader(it.nombre, it.clase, it.especie, it.nivel, onEdit = onEditCharacter)
                    }
                    HpBlock(combate, viewModel)
                    if (caract != null) {
                        AbilityScoresBlock(caract, viewModel)
                    }
                    if (state.armas.isNotEmpty()) {
                        ArmasBlock(state.armas)
                    }
                    Spacer(Modifier.height(60.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CombatStatsBlock(combate, viewModel)
                    SavingThrowsBlock(viewModel, caract, onResult = { saveRollResult = it })
                    if (state.efectos.isNotEmpty()) {
                        EffectsBlock(state.efectos)
                    }
                    Spacer(Modifier.height(60.dp))
                }
            }
        } else {
            // Móvil: una sola columna
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.personaje?.let {
                    CharacterHeader(it.nombre, it.clase, it.especie, it.nivel, onEdit = onEditCharacter)
                }
                HpBlock(combate, viewModel)
                CombatStatsBlock(combate, viewModel)
                if (caract != null) {
                    AbilityScoresBlock(caract, viewModel)
                }
                SavingThrowsBlock(viewModel, caract, onResult = { saveRollResult = it })
                if (state.armas.isNotEmpty()) {
                    ArmasBlock(state.armas)
                }
                if (state.efectos.isNotEmpty()) {
                    EffectsBlock(state.efectos)
                }
                Spacer(Modifier.height(60.dp))
            }
        }
    }

    // Resultado de una tirada de salvación
    saveRollResult?.let { result ->
        AlertDialog(
            onDismissRequest = { saveRollResult = null },
            confirmButton = {
                TextButton(onClick = { saveRollResult = null }) {
                    Text("Cerrar", color = GoldAccent)
                }
            },
            title = { Text("Tirada de salvación", color = GoldAccent, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(result.breakdown, color = SilverText, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${result.total}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            result.total >= 15 -> HpNormal
                            result.total >= 10 -> HpAmber
                            else -> HpRed
                        }
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = ParchmentSurface
        )
    }
}

// ── Cabecera ──

@Composable
fun CharacterHeader(nombre: String, clase: String, especie: String, nivel: Int, onEdit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                nombre,
                modifier = Modifier.weight(1f, fill = false),
                textAlign = TextAlign.Center,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent,
                letterSpacing = 2.sp
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar personaje",
                    modifier = Modifier.size(18.dp),
                    tint = GoldDim
                )
            }
        }
        Text(
            "$clase  ·  $especie  ·  Nivel $nivel",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverDim
        )
    }
}

// ── Puntos de golpe ──

@Composable
fun HpBlock(combate: EstadoCombate, viewModel: MainViewModel) {
    val hpColorEnum = Combat.hpColor(combate.pgActuales, combate.pgMaximos)
    val targetColor = when (hpColorEnum) {
        Combat.HpColor.NORMAL -> HpNormal
        Combat.HpColor.AMBER -> HpAmber
        Combat.HpColor.RED -> HpRed
        Combat.HpColor.GREY -> HpGrey
    }
    val hpColor by animateColorAsState(targetColor, animationSpec = tween(400), label = "hp")
    val ratio = if (combate.pgMaximos > 0) combate.pgActuales.toFloat() / combate.pgMaximos else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "PUNTOS DE GOLPE",
                style = MaterialTheme.typography.labelMedium,
                color = GoldDim,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "${combate.pgActuales}",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = hpColor,
                    lineHeight = 72.sp
                )
                Text(
                    " / ${combate.pgMaximos}",
                    fontSize = 28.sp,
                    color = SilverDim,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            // Barra de PG
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1A1510))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(hpColor.copy(alpha = 0.7f), hpColor)))
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canLose = combate.pgActuales > 0
                OutlinedButton(
                    onClick = { viewModel.changePg(-1) },
                    enabled = canLose,
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, HpRed.copy(alpha = if (canLose) 0.6f else 0.2f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("-1", color = HpRed.copy(alpha = if (canLose) 1f else 0.35f), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Spacer(Modifier.weight(1f))

                val canHeal = combate.pgActuales < combate.pgMaximos
                OutlinedButton(
                    onClick = { viewModel.changePg(1) },
                    enabled = canHeal,
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, HpNormal.copy(alpha = if (canHeal) 0.6f else 0.2f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+1", color = HpNormal.copy(alpha = if (canHeal) 1f else 0.35f), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

// ── Estadísticas de combate ──

@Composable
fun CombatStatsBlock(combate: EstadoCombate, viewModel: MainViewModel) {
    val slotSummary = viewModel.getSlotSummary()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "COMBATE",
                style = MaterialTheme.typography.labelMedium,
                color = GoldDim,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BigStatBox("CA", "${combate.caOverride ?: combate.caBase}", ArcaneBlue, Modifier.weight(1f))
                BigStatBox("BA", "+${combate.ba}", ArcanePurple, Modifier.weight(1f))
                BigStatBox("DG", combate.dadoGolpe, GoldAccent, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BigStatBox("Marchar", "${combate.movMarchar} m", ForestGreen, Modifier.weight(1f))
                BigStatBox("Correr", "${combate.movCorrer} m", ForestGreen, Modifier.weight(1f))
            }

            // Espacios de conjuro
            if (slotSummary.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "ESPACIOS DE CONJURO",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldDim,
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slotSummary.forEach { (nivel, slots) ->
                        val (libres, total) = slots
                        val color = if (libres == 0) HpRed.copy(alpha = 0.7f) else GoldAccent
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = color.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "N$nivel",
                                    fontSize = 10.sp,
                                    color = color.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    "$libres/$total",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BigStatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                fontSize = 10.sp,
                color = color.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ── Características ──

@Composable
fun AbilityScoresBlock(caract: Caracteristicas, viewModel: MainViewModel) {
    data class AbilityInfo(
        val abbr: String,
        val fullName: String,
        val value: Int,
        val primary: Boolean,
        val overrideMod: Int?,
        val color: Color
    )

    val abilities = listOf(
        AbilityInfo("FUE", "Fuerza", caract.fue, caract.fuePrimaria, caract.fueOverrideMod, Color(0xFFCC6644)),
        AbilityInfo("DES", "Destreza", caract.des, caract.desPrimaria, caract.desOverrideMod, Color(0xFF55AA55)),
        AbilityInfo("CON", "Constitución", caract.con, caract.conPrimaria, caract.conOverrideMod, Color(0xFFCC9933)),
        AbilityInfo("INT", "Inteligencia", caract.int_, caract.intPrimaria, caract.intOverrideMod, Color(0xFF5588CC)),
        AbilityInfo("SAB", "Sabiduría", caract.sab, caract.sabPrimaria, caract.sabOverrideMod, Color(0xFFAAAADD)),
        AbilityInfo("CAR", "Carisma", caract.car, caract.carPrimaria, caract.carOverrideMod, Color(0xFFCC55AA))
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "CARACTERÍSTICAS",
                style = MaterialTheme.typography.labelMedium,
                color = GoldDim,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(10.dp))

            abilities.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { ab ->
                        val mod = ab.overrideMod ?: Modifiers.modifier(ab.value)
                        val modStr = if (mod >= 0) "+$mod" else "$mod"
                        val isOverride = ab.overrideMod != null

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = ab.color.copy(alpha = 0.06f),
                            border = BorderStroke(
                                if (ab.primary) 2.dp else 1.dp,
                                if (ab.primary) ab.color.copy(alpha = 0.5f) else ab.color.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        ab.abbr,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ab.color,
                                        letterSpacing = 1.sp
                                    )
                                    if (ab.primary) {
                                        Spacer(Modifier.width(3.dp))
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Primaria",
                                            modifier = Modifier.size(12.dp),
                                            tint = GoldAccent
                                        )
                                    }
                                }
                                Text(
                                    "${ab.value}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Parchment
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        modStr,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ab.color.copy(alpha = 0.8f)
                                    )
                                    if (isOverride) {
                                        Spacer(Modifier.width(2.dp))
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Override",
                                            modifier = Modifier.size(10.dp),
                                            tint = GoldDim
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Armas ──

@Composable
fun ArmasBlock(armas: List<Arma>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "ARMAS",
                style = MaterialTheme.typography.labelMedium,
                color = GoldDim,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(8.dp))

            armas.forEach { arma ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (arma.tipo == "distancia") Icons.Default.GpsFixed else Icons.Default.Gavel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = SilverDim
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(arma.nombre, color = Parchment, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (arma.notas.isNotBlank()) {
                            Text(arma.notas, color = SilverDim, fontSize = 11.sp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = HpRed.copy(alpha = 0.1f)
                    ) {
                        Text(
                            arma.formulaDano,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = HpRed.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("VE ${arma.ve}", color = SilverDim, fontSize = 11.sp)
                }
            }
        }
    }
}

// ── Tiradas de salvación ──

private val saveDescriptions = mapOf(
    "FUE" to "Parálisis, constricción, aplastamiento",
    "DES" to "Trampas, ataques de aliento",
    "CON" to "Enfermedad, drenaje de energía, veneno",
    "INT" to "Magia arcana, ilusiones",
    "SAB" to "Magia divina, confusión, mirada, polimorfización, petrificación, parálisis",
    "CAR" to "Ataque de muerte, encantamiento, miedo"
)

private val saveColors = mapOf(
    "FUE" to Color(0xFFCC6644),
    "DES" to Color(0xFF55AA55),
    "CON" to Color(0xFFCC9933),
    "INT" to Color(0xFF5588CC),
    "SAB" to Color(0xFFAAAADD),
    "CAR" to Color(0xFFCC55AA)
)

@Composable
fun SavingThrowsBlock(
    viewModel: MainViewModel,
    caract: Caracteristicas?,
    onResult: (DiceRollResult) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, GoldDim.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "SALVACIONES",
                style = MaterialTheme.typography.labelMedium,
                color = GoldDim,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(10.dp))

            val saves = listOf("FUE", "DES", "CON", "INT", "SAB", "CAR")
            saves.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { save ->
                        val mod = if (caract != null) viewModel.getModForStat(save, caract) else 0
                        val modStr = if (mod >= 0) "+$mod" else "$mod"
                        val color = saveColors[save] ?: GoldAccent
                        val desc = saveDescriptions[save] ?: ""

                        Surface(
                            onClick = { onResult(viewModel.tirarSalvacion(save)) },
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = color.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    save,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    modStr,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color.copy(alpha = 0.8f)
                                )
                                Text(
                                    desc.take(25) + if (desc.length > 25) "…" else "",
                                    fontSize = 8.sp,
                                    color = color.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                if (row != saves.chunked(3).last()) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── Efectos activos ──

@Composable
fun EffectsBlock(efectos: List<EfectoActivo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
        border = BorderStroke(1.dp, ArcanePurple.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "EFECTOS ACTIVOS",
                style = MaterialTheme.typography.labelMedium,
                color = ArcanePurple,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(8.dp))

            efectos.forEach { efecto ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ArcanePurple.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(efecto.nombre, color = Parchment, modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ArcanePurple.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${efecto.restante} ${efecto.unidad}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = ArcanePurple,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

