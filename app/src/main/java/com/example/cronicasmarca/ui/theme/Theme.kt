package com.example.cronicasmarca.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Paleta "pergamino y oro" ──
val Parchment = Color(0xFFF5E6C8)
val ParchmentDark = Color(0xFF2A2318)
val ParchmentSurface = Color(0xFF342B1E)
val ParchmentSurfaceVariant = Color(0xFF3E3428)
val GoldAccent = Color(0xFFD4A853)
val GoldLight = Color(0xFFE8C97A)
val GoldDim = Color(0xFF8B7340)
val ArcaneBlue = Color(0xFF7BA4DB)
val ArcanePurple = Color(0xFFB48EDB)
val BloodRed = Color(0xFFCC3333)
val ForestGreen = Color(0xFF5A9E5A)
val SilverText = Color(0xFFD4D0C8)
val SilverDim = Color(0xFF8A8478)

// ── Colores de la barra de PG ──
val HpNormal = Color(0xFF6ABF6A)
val HpAmber = Color(0xFFE8B84A)
val HpRed = Color(0xFFCC3333)
val HpGrey = Color(0xFF6B6560)

private val DarkColorScheme = darkColorScheme(
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
    background = ParchmentDark,
    onBackground = Parchment,
    surface = ParchmentSurface,
    onSurface = Parchment,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = SilverText,
    error = BloodRed,
    onError = Parchment,
    errorContainer = Color(0xFF4A1A1A),
    onErrorContainer = Color(0xFFFFAAAA),
    outline = GoldDim,
    outlineVariant = Color(0xFF4A4030)
)

val CronicasTypography = Typography(
    displayLarge = TextStyle(
        color = GoldAccent,
        fontSize = 57.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    ),
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

@Composable
fun CronicasMarcaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = CronicasTypography,
        content = content
    )
}
