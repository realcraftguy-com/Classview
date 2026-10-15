package com.realcraftguy.classview.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Gemeinsame Farben für Schüler- und Host-App. Dieselben Werte nutzt die Lehrer-Web-App
 * (web/mockups/index.html, `:root`), damit alle drei Oberflächen zusammenpassen.
 */
@Immutable
data class ClassviewColors(
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val outline: Color,
    val text: Color,
    val textSecondary: Color,
    val online: Color,
    val offline: Color,
    val danger: Color,
    val dangerSoft: Color,
)

val LightColors = ClassviewColors(
    accent = Color(0xFF2B6BE4),
    onAccent = Color.White,
    accentSoft = Color(0xFFE6EEFD),
    background = Color(0xFFF4F5F7),
    surface = Color.White,
    surfaceMuted = Color(0xFFECEEF1),
    outline = Color(0xFFDCDFE4),
    text = Color(0xFF1B1D21),
    textSecondary = Color(0xFF686D76),
    online = Color(0xFF2F9E5C),
    offline = Color(0xFFA3A8B0),
    danger = Color(0xFFCC3D3D),
    dangerSoft = Color(0xFFFBEAEA),
)

val DarkColors = ClassviewColors(
    accent = Color(0xFF5B8EF0),
    onAccent = Color.White,
    accentSoft = Color(0xFF1E2A40),
    background = Color(0xFF111214),
    surface = Color(0xFF1C1D20),
    surfaceMuted = Color(0xFF26282C),
    outline = Color(0xFF34373C),
    text = Color(0xFFEDEEF0),
    textSecondary = Color(0xFF9A9FA8),
    online = Color(0xFF45B874),
    offline = Color(0xFF6E737B),
    danger = Color(0xFFE46A6A),
    dangerSoft = Color(0xFF3A2224),
)

val LocalClassviewColors = staticCompositionLocalOf { LightColors }

object ClassviewTheme {
    val colors: ClassviewColors
        @Composable get() = LocalClassviewColors.current
}

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    )
}

/** Großer Klassencode, monospace und gut lesbar. */
val CodeTextStyle = TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
    fontWeight = FontWeight.SemiBold,
    fontSize = 40.sp,
    letterSpacing = 4.sp,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ClassviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            primaryContainer = c.accentSoft, onPrimaryContainer = c.text,
            secondaryContainer = c.surfaceMuted, onSecondaryContainer = c.text,
            background = c.background, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceMuted, onSurfaceVariant = c.textSecondary,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.surfaceMuted,
            outline = c.outline, outlineVariant = c.outline,
            error = c.danger, errorContainer = c.dangerSoft,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            primaryContainer = c.accentSoft, onPrimaryContainer = c.text,
            secondaryContainer = c.surfaceMuted, onSecondaryContainer = c.text,
            background = c.background, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceMuted, onSurfaceVariant = c.textSecondary,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.surfaceMuted,
            outline = c.outline, outlineVariant = c.outline,
            error = c.danger, errorContainer = c.dangerSoft,
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalClassviewColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
