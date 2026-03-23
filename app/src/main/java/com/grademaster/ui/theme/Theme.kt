package com.grademaster.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Brand Colors
// ─────────────────────────────────────────────────────────────────────────────
object GradeMasterColors {
    // Primary: Green
    val Green80 = Color(0xFF81C784)
    val Green40 = Color(0xFF2E7D32)
    val GreenContainer = Color(0xFFC8E6C9)

    // Secondary: Blue
    val Blue80 = Color(0xFF90CAF9)
    val Blue40 = Color(0xFF1565C0)
    val BlueContainer = Color(0xFFBBDEFB)

    // Accent: Thick Orange
    val OrangeAccent = Color(0xFFFF6D00)
    val OrangeLight = Color(0xFFFFAB40)
    val OrangeDark = Color(0xFFE65100)
    val OrangeContainer = Color(0xFFFFE0B2)

    // Neutrals
    val White = Color(0xFFFFFFFF)
    val OffWhite = Color(0xFFF9FBF9)
    val DarkSurface = Color(0xFF1A1C1A)
    val DarkBackground = Color(0xFF121412)

    // Grade Colors
    val GradeA = Color(0xFF2E7D32)
    val GradeB = Color(0xFF1565C0)
    val GradeC = Color(0xFFFF6D00)
    val GradeD = Color(0xFFFF8F00)
    val GradeF = Color(0xFFC62828)

    // Shimmer
    val ShimmerBase = Color(0xFFE0E0E0)
    val ShimmerHighlight = Color(0xFFF5F5F5)
    val ShimmerBaseDark = Color(0xFF3A3A3A)
    val ShimmerHighlightDark = Color(0xFF4A4A4A)
}

// ─────────────────────────────────────────────────────────────────────────────
// Color Schemes
// ─────────────────────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = GradeMasterColors.Green40,
    onPrimary = GradeMasterColors.White,
    primaryContainer = GradeMasterColors.GreenContainer,
    onPrimaryContainer = GradeMasterColors.Green40,

    secondary = GradeMasterColors.Blue40,
    onSecondary = GradeMasterColors.White,
    secondaryContainer = GradeMasterColors.BlueContainer,
    onSecondaryContainer = GradeMasterColors.Blue40,

    tertiary = GradeMasterColors.OrangeAccent,
    onTertiary = GradeMasterColors.White,
    tertiaryContainer = GradeMasterColors.OrangeContainer,
    onTertiaryContainer = GradeMasterColors.OrangeDark,

    background = GradeMasterColors.OffWhite,
    onBackground = Color(0xFF1A1C1A),
    surface = GradeMasterColors.White,
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFDDE5D8),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF70796E),
    error = Color(0xFFB3261E)
)

private val DarkColorScheme = darkColorScheme(
    primary = GradeMasterColors.Green80,
    onPrimary = Color(0xFF003909),
    primaryContainer = GradeMasterColors.Green40,
    onPrimaryContainer = GradeMasterColors.GreenContainer,

    secondary = GradeMasterColors.Blue80,
    onSecondary = Color(0xFF003060),
    secondaryContainer = GradeMasterColors.Blue40,
    onSecondaryContainer = GradeMasterColors.BlueContainer,

    tertiary = GradeMasterColors.OrangeLight,
    onTertiary = Color(0xFF4A2000),
    tertiaryContainer = GradeMasterColors.OrangeDark,
    onTertiaryContainer = GradeMasterColors.OrangeContainer,

    background = GradeMasterColors.DarkBackground,
    onBackground = Color(0xFFE2E3DE),
    surface = GradeMasterColors.DarkSurface,
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C0),
    outline = Color(0xFF8A938A),
    error = Color(0xFFF2B8B5)
)

// ─────────────────────────────────────────────────────────────────────────────
// Theme Composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GradeMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GradeMasterTypography,
        shapes = GradeMasterShapes,
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: Grade -> Color
// ─────────────────────────────────────────────────────────────────────────────
fun gradeColor(grade: String): Color = when (grade) {
    "A" -> GradeMasterColors.GradeA
    "B" -> GradeMasterColors.GradeB
    "C" -> GradeMasterColors.GradeC
    "D" -> GradeMasterColors.GradeD
    else -> GradeMasterColors.GradeF
}
