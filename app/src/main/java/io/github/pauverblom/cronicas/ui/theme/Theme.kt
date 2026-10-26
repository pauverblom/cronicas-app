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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamicColorScheme
import io.github.pauverblom.cronicas.data.AjustesApp
import io.github.pauverblom.cronicas.data.Modo
import io.github.pauverblom.cronicas.data.Tema
import io.github.pauverblom.cronicas.rules.Atributo

/**
 * Colores del sistema de diseño «Crónicas de la Marca» (tokens.json). Los nombres siguen los del sistema:
 * fondos de atrás hacia delante (noche → vitela 100 → 200 → 300), tintas, el metal dorado y el hierro de los
 * marcos, y los colores con significado (PG, características, avisos, magia).
 */
@Immutable
data class Paleta(
    val oscuro: Boolean,
    /** Fondo de la página, detrás de todo. */
    val noche: Color,
    /** Resplandor translúcido que se acumula sobre la noche. */
    val bruma: Color,
    /** Motas sobre la noche: estrellas en oscuro, fibras del papel en claro. */
    val estrella: Color,
    /** Cristal oscuro dentro de ventanas y diálogos. */
    val vitela100: Color,
    /** Elementos elevados dentro de una ventana; arriba del degradado del cristal. */
    val vitela200: Color,
    /** Pozos: pistas de barras, campos, interiores de placas, la bandeja de monedas. */
    val vitela300: Color,
    /** Filete que se desvanece entre filas. Solo decorativo. */
    val regla: Color,
    /** Línea dorada plana donde no cabe un degradado. */
    val dorado: Color,
    val metalAlto: Color,
    val metalMedio: Color,
    val metalBajo: Color,
    val hierroAlto: Color,
    val hierro: Color,
    val hierroBajo: Color,
    val tinta: Color,
    val tintaSuave: Color,
    /** Solo para deshabilitado y textos de ejemplo (no llega a 4.5:1 a propósito). */
    val tintaTenue: Color,
    /** El oro como texto y marca: títulos de placa, estrellas, pestaña activa, BA. */
    val oro: Color,
    val oroBrillo: Color,
    val oroRelleno: Color,
    val sobreOro: Color,
    val sangre: Color,
    val sangreRelleno: Color,
    val sobreSangre: Color,
    val azulArcano: Color,
    /** Reservado para la marca de valor fijado a mano. */
    val violetaArcano: Color,
    val alerta: Color,
    val alertaSuave: Color,
    val peligroSuave: Color,
    val foco: Color,
    val grabadoAlto: Color,
    val grabadoBajo: Color,
    val brilloCristal: Color,
    val velo: Color,
    val pgSano: Color,
    val pgHerido: Color,
    val pgCritico: Color,
    val pgNinguno: Color,
    val atributos: Map<Atributo, Color>
) {
    fun atributo(a: Atributo): Color = atributos.getValue(a)
}

/** Colores iguales en los dos temas: papel de las ilustraciones, caras de las monedas. */
object ColoresFijos {
    val papelArte = Color(0xFFF2E6CB)
    val tintaArte = Color(0xFF3B2A1A)
    val arteFaltante = Color(0xFF9C2A1E)
    val monedaPlatino = Color(0xFFDFE3EA)
    val monedaOro = Color(0xFFE2B84A)
    val monedaElectro = Color(0xFFCDBF78)
    val monedaPlata = Color(0xFFB9BDC2)
    val monedaCobre = Color(0xFFC4743F)
}

/** Fuerza de la textura grabada (tex-runes, tex-grain, tex-glow). */
object Textura {
    const val RUNAS = 0.12f
    const val GRANO = 0.04f
    const val BRILLO = 0.07f
}

val PergaminoOscuro = Paleta(
    oscuro = true,
    noche = Color(0xFF07080E),
    bruma = Color(0x4D2848A8),
    estrella = Color(0xFFD7E0FF),
    vitela100 = Color(0xFF0F1019),
    vitela200 = Color(0xFF171824),
    vitela300 = Color(0xFF040509),
    regla = Color(0xFF2C2D3C),
    dorado = Color(0xFFA07C3C),
    metalAlto = Color(0xFFF6DC9A),
    metalMedio = Color(0xFFD4A553),
    metalBajo = Color(0xFF7D5A22),
    hierroAlto = Color(0xFFA3A9B6),
    hierro = Color(0xFF6A707E),
    hierroBajo = Color(0xFF2A2D36),
    tinta = Color(0xFFF1E8D4),
    tintaSuave = Color(0xFFADA391),
    tintaTenue = Color(0xFF6B6A74),
    oro = Color(0xFFDDAC4C),
    oroBrillo = Color(0xFFF6D382),
    oroRelleno = Color(0xFFC99634),
    sobreOro = Color(0xFF1A0F04),
    sangre = Color(0xFFEA6253),
    sangreRelleno = Color(0xFF9E2A1F),
    sobreSangre = Color(0xFFFFF1E8),
    azulArcano = Color(0xFF7EA9E6),
    violetaArcano = Color(0xFFB49CF0),
    alerta = Color(0xFFF0A640),
    alertaSuave = Color(0xFF241A0C),
    peligroSuave = Color(0xFF2A1012),
    foco = Color(0xFFF6D382),
    grabadoAlto = Color(0xFF9C7A3C),
    grabadoBajo = Color(0xFF000000),
    brilloCristal = Color(0x59FFFFFF),
    velo = Color(0xB8000000),
    pgSano = Color(0xFF9CCF6F),
    pgHerido = Color(0xFFE9B04A),
    pgCritico = Color(0xFFEE5E4B),
    pgNinguno = Color(0xFF857866),
    atributos = mapOf(
        Atributo.FUE to Color(0xFFF2804F),
        Atributo.DES to Color(0xFF7FCB7A),
        Atributo.CON to Color(0xFFE8B54C),
        Atributo.INT to Color(0xFF7AAAF2),
        Atributo.SAB to Color(0xFFBEA8F0),
        Atributo.CAR to Color(0xFFEA80C4)
    )
)

val PergaminoClaro = Paleta(
    oscuro = false,
    noche = Color(0xFFECDFC2),
    bruma = Color(0x66FFFAF0),
    estrella = Color(0xFFC9B48A),
    vitela100 = Color(0xFFF5EAD2),
    vitela200 = Color(0xFFFBF3E2),
    vitela300 = Color(0xFFE4D2AE),
    regla = Color(0xFFCDB48B),
    dorado = Color(0xFF8A6630),
    metalAlto = Color(0xFFE2C06E),
    metalMedio = Color(0xFFB07F2A),
    metalBajo = Color(0xFF5E3F0C),
    hierroAlto = Color(0xFF8D8F96),
    hierro = Color(0xFF55565E),
    hierroBajo = Color(0xFF3A3A40),
    tinta = Color(0xFF2A1C10),
    tintaSuave = Color(0xFF5C4630),
    tintaTenue = Color(0xFF94806A),
    oro = Color(0xFF72500E),
    oroBrillo = Color(0xFFA8741A),
    oroRelleno = Color(0xFFB8862C),
    sobreOro = Color(0xFF1A0F04),
    sangre = Color(0xFF9C2A1E),
    sangreRelleno = Color(0xFF9C2A1E),
    sobreSangre = Color(0xFFFFF4EE),
    azulArcano = Color(0xFF2B5B98),
    violetaArcano = Color(0xFF5B3E9A),
    alerta = Color(0xFF824700),
    alertaSuave = Color(0xFFF7E2C0),
    peligroSuave = Color(0xFFF6D9D2),
    foco = Color(0xFF1F4F94),
    grabadoAlto = Color(0xFFFFFAF0),
    grabadoBajo = Color(0xFF8A6630),
    brilloCristal = Color(0x66FFFFFF),
    velo = Color(0xA62A1C10),
    pgSano = Color(0xFF33621A),
    pgHerido = Color(0xFF7C4D00),
    pgCritico = Color(0xFFA3241A),
    pgNinguno = Color(0xFF7D7060),
    atributos = mapOf(
        Atributo.FUE to Color(0xFF963813),
        Atributo.DES to Color(0xFF276128),
        Atributo.CON to Color(0xFF784E06),
        Atributo.INT to Color(0xFF26559B),
        Atributo.SAB to Color(0xFF5D45A2),
        Atributo.CAR to Color(0xFF962A74)
    )
)

val LocalPaleta = staticCompositionLocalOf { PergaminoOscuro }

/** Acceso corto a los tokens: `Marca.colores.oro`. */
object Marca {
    val colores: Paleta
        @Composable get() = LocalPaleta.current
}

private fun contraste(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

/**
 * Lleva un esquema generado (Material You o el color del retrato) a los papeles del sistema y conserva los
 * colores con significado del pergamino del mismo modo. Si la tinta no llega a 4.5:1 sobre algún fondo,
 * devuelve el pergamino tal cual.
 */
private fun paletaDesde(s: ColorScheme, base: Paleta): Paleta {
    val o = base.oscuro
    val p = s.primary
    val generada = base.copy(
        noche = if (o) lerp(s.surfaceContainerLowest, Color.Black, 0.35f) else s.surfaceContainerHighest,
        bruma = p.copy(alpha = if (o) 0.22f else 0.3f),
        estrella = if (o) lerp(s.onSurface, p, 0.3f) else s.outlineVariant,
        vitela100 = if (o) s.surfaceContainerLow else s.surfaceContainerLow,
        vitela200 = if (o) s.surfaceContainer else s.surfaceContainerLowest,
        vitela300 = if (o) lerp(s.surfaceContainerLowest, Color.Black, 0.6f) else s.surfaceContainerHigh,
        regla = s.outlineVariant,
        dorado = lerp(p, s.surface, 0.35f),
        metalAlto = lerp(p, Color.White, 0.45f),
        metalMedio = p,
        metalBajo = lerp(p, Color.Black, 0.55f),
        hierroAlto = lerp(s.outline, Color.White, 0.3f),
        hierro = s.outline,
        hierroBajo = lerp(s.outline, Color.Black, 0.5f),
        tinta = s.onSurface,
        tintaSuave = s.onSurfaceVariant,
        tintaTenue = s.outline,
        oro = p,
        oroBrillo = lerp(p, if (o) Color.White else Color.Black, 0.3f),
        oroRelleno = if (o) p else lerp(p, Color.White, 0.25f),
        sobreOro = if (o) s.onPrimary else lerp(s.onPrimaryContainer, Color.Black, 0.4f),
        foco = if (o) lerp(p, Color.White, 0.3f) else p,
        grabadoAlto = if (o) lerp(p, Color.Black, 0.35f) else Color(0xFFFFFAF0),
        grabadoBajo = if (o) Color.Black else lerp(p, Color.Black, 0.4f)
    )
    val fondos = listOf(generada.noche, generada.vitela100, generada.vitela200, generada.vitela300)
    val legible = fondos.all { contraste(generada.tinta, it) >= 4.5f && contraste(generada.tintaSuave, it) >= 4.5f }
    return if (legible) generada else base
}

/** Esquema de Material con los colores del sistema, para lo que aún dibuja Material (menús, hojas, selección de texto). */
private fun esquemaMaterial(c: Paleta): ColorScheme {
    val base = if (c.oscuro) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.oro,
        onPrimary = c.sobreOro,
        primaryContainer = c.vitela200,
        onPrimaryContainer = c.oro,
        secondary = c.azulArcano,
        onSecondary = c.vitela300,
        secondaryContainer = c.vitela200,
        onSecondaryContainer = c.azulArcano,
        tertiary = c.violetaArcano,
        onTertiary = c.vitela300,
        background = c.noche,
        onBackground = c.tinta,
        surface = c.vitela100,
        onSurface = c.tinta,
        surfaceVariant = c.vitela200,
        onSurfaceVariant = c.tintaSuave,
        surfaceContainerLowest = c.vitela300,
        surfaceContainerLow = c.vitela100,
        surfaceContainer = c.vitela100,
        surfaceContainerHigh = c.vitela200,
        surfaceContainerHighest = c.vitela200,
        surfaceTint = c.oro,
        inverseSurface = c.tinta,
        inverseOnSurface = c.vitela100,
        error = c.sangre,
        onError = c.sobreSangre,
        errorContainer = c.peligroSuave,
        onErrorContainer = c.sangre,
        outline = c.hierro,
        outlineVariant = c.regla,
        scrim = c.velo
    )
}

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
    val paleta: Paleta = when (ajustes.tema) {
        Tema.PERGAMINO_OSCURO -> PergaminoOscuro
        Tema.PERGAMINO_CLARO -> PergaminoClaro
        Tema.MATERIAL_YOU ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                remember(oscuro) {
                    paletaDesde(if (oscuro) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context), pergamino)
                }
            } else pergamino
        Tema.RETRATO -> semilla?.let { seed ->
            remember(seed, oscuro) {
                paletaDesde(dynamicColorScheme(seedColor = Color(seed), isDark = oscuro, isAmoled = false), pergamino)
            }
        } ?: pergamino
    }
    val esquema = remember(paleta) { esquemaMaterial(paleta) }
    CompositionLocalProvider(LocalPaleta provides paleta) {
        MaterialTheme(colorScheme = esquema, typography = TipografiaMaterial, content = content)
    }
}

/** Tipografía de Material para lo que aún dibuja Material (menús desplegables, selección de texto). */
private val TipografiaMaterial = Typography(
    bodyLarge = Tipos.ui,
    bodyMedium = Tipos.uiSm,
    bodySmall = Tipos.caption,
    labelLarge = Tipos.ui,
    titleMedium = Tipos.ui
)
