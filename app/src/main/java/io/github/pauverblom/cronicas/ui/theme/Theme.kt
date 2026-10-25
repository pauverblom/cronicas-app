package io.github.pauverblom.cronicas.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.materialkolor.dynamicColorScheme
import io.github.pauverblom.cronicas.data.AjustesApp
import io.github.pauverblom.cronicas.data.Modo
import io.github.pauverblom.cronicas.data.Tema
import io.github.pauverblom.cronicas.rules.Atributo

// ── Paleta "pergamino y oro" (tema oscuro) ──
private val Parchment = Color(0xFFF5E6C8)
private val ParchmentDark = Color(0xFF2A2318)
private val ParchmentSurface = Color(0xFF342B1E)
private val ParchmentSurfaceVariant = Color(0xFF3E3428)
private val GoldAccent = Color(0xFFD4A853)
private val GoldLight = Color(0xFFE8C97A)
private val GoldDim = Color(0xFF8B7340)
private val ArcaneBlue = Color(0xFF7BA4DB)
private val ArcanePurple = Color(0xFFB48EDB)
private val BloodRed = Color(0xFFCC3333)
private val SilverText = Color(0xFFD4D0C8)

private val PergaminoOscuro = darkColorScheme(
    primary = GoldAccent,
    onPrimary = ParchmentDark,
    primaryContainer = Color(0xFF4A3D20),
    onPrimaryContainer = GoldLight,
    secondary = ArcaneBlue,
    onSecondary = ParchmentDark,
    secondaryContainer = Color(0xFF2A3A4D),
    onSecondaryContainer = ArcaneBlue,
    tertiary = ArcanePurple,
    onTertiary = ParchmentDark,
    tertiaryContainer = Color(0xFF3D2F4D),
    onTertiaryContainer = Color(0xFFE2CCF5),
    background = ParchmentDark,
    onBackground = Parchment,
    surface = ParchmentDark,
    onSurface = Parchment,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = SilverText,
    surfaceContainerLowest = Color(0xFF1F1A12),
    surfaceContainerLow = Color(0xFF2F271B),
    surfaceContainer = ParchmentSurface,
    surfaceContainerHigh = Color(0xFF3A3022),
    surfaceContainerHighest = Color(0xFF443828),
    error = BloodRed,
    onError = Parchment,
    errorContainer = Color(0xFF4A1A1A),
    onErrorContainer = Color(0xFFFFAAAA),
    outline = GoldDim,
    outlineVariant = Color(0xFF4A4030)
)

// ── Pergamino claro: papel crema, tinta marrón y acentos rojos, como la ficha impresa ──
private val Ink = Color(0xFF2E2216)
private val Cream = Color(0xFFF6EBD3)

private val PergaminoClaro = lightColorScheme(
    primary = Color(0xFF8E2A1C),
    onPrimary = Color(0xFFFFF8EE),
    primaryContainer = Color(0xFFF3D2C4),
    onPrimaryContainer = Color(0xFF3B0A03),
    secondary = Color(0xFF6B4E2E),
    onSecondary = Color(0xFFFFF8EE),
    secondaryContainer = Color(0xFFEBD9BC),
    onSecondaryContainer = Color(0xFF2A1A08),
    tertiary = Color(0xFF3D5A78),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3E2F2),
    onTertiaryContainer = Color(0xFF0E2236),
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8D9BC),
    onSurfaceVariant = Color(0xFF52432F),
    surfaceContainerLowest = Color(0xFFFFFAF0),
    surfaceContainerLow = Color(0xFFFBF2DF),
    surfaceContainer = Color(0xFFF1E4C8),
    surfaceContainerHigh = Color(0xFFEBDCBE),
    surfaceContainerHighest = Color(0xFFE4D3B3),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF8A7558),
    outlineVariant = Color(0xFFD2C0A0)
)

/** Colores de la ficha que no forman parte de Material: PG y características. */
@Immutable
data class ColoresHoja(
    val pgBien: Color,
    val pgMedio: Color,
    val pgMal: Color,
    val pgCero: Color,
    val atributos: Map<Atributo, Color>
)

private val ColoresOscuro = ColoresHoja(
    pgBien = Color(0xFF6ABF6A),
    pgMedio = Color(0xFFE8B84A),
    pgMal = Color(0xFFE05050),
    pgCero = Color(0xFF6B6560),
    atributos = mapOf(
        Atributo.FUE to Color(0xFFE07A5A),
        Atributo.DES to Color(0xFF6CC06C),
        Atributo.CON to Color(0xFFD9A441),
        Atributo.INT to Color(0xFF6F9BE0),
        Atributo.SAB to Color(0xFFB3B3E6),
        Atributo.CAR to Color(0xFFD86CB8)
    )
)

private val ColoresClaro = ColoresHoja(
    pgBien = Color(0xFF2E7D32),
    pgMedio = Color(0xFFB26A00),
    pgMal = Color(0xFFC62828),
    pgCero = Color(0xFF757575),
    atributos = mapOf(
        Atributo.FUE to Color(0xFFB0472A),
        Atributo.DES to Color(0xFF2F7D32),
        Atributo.CON to Color(0xFF9A6A00),
        Atributo.INT to Color(0xFF2F5FA8),
        Atributo.SAB to Color(0xFF5A5AA0),
        Atributo.CAR to Color(0xFFA0367F)
    )
)

val LocalColoresHoja = staticCompositionLocalOf { ColoresOscuro }

val CronicasTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.15.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp)
)

/** Los temas pergamino son fijos; el resto sigue el modo elegido (o el del sistema). */
@Composable
fun esTemaOscuro(ajustes: AjustesApp): Boolean {
    val sistema = isSystemInDarkTheme()
    return when (ajustes.tema) {
        Tema.PERGAMINO_OSCURO -> true
        Tema.PERGAMINO_CLARO -> false
        else -> when (ajustes.modo) {
            Modo.SISTEMA -> sistema
            Modo.CLARO -> false
            Modo.OSCURO -> true
        }
    }
}

/**
 * Tema de la app. Con [Tema.RETRATO], [semilla] es el color sacado del retrato del personaje;
 * sin retrato (o fuera de una ficha) se usa el pergamino que corresponda al modo.
 */
@Composable
fun CronicasMarcaTheme(
    ajustes: AjustesApp = AjustesApp(),
    semilla: Int? = null,
    content: @Composable () -> Unit
) {
    val oscuro = esTemaOscuro(ajustes)
    val context = LocalContext.current
    val pergamino = if (oscuro) PergaminoOscuro else PergaminoClaro
    val scheme: ColorScheme = when (ajustes.tema) {
        Tema.PERGAMINO_OSCURO -> PergaminoOscuro
        Tema.PERGAMINO_CLARO -> PergaminoClaro
        Tema.MATERIAL_YOU ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (oscuro) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else pergamino
        Tema.RETRATO -> semilla?.let { seed ->
            remember(seed, oscuro) { dynamicColorScheme(seedColor = Color(seed), isDark = oscuro, isAmoled = false) }
        } ?: pergamino
    }
    CompositionLocalProvider(LocalColoresHoja provides if (oscuro) ColoresOscuro else ColoresClaro) {
        MaterialTheme(colorScheme = scheme, typography = CronicasTypography, content = content)
    }
}
