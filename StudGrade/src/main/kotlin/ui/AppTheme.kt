package ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Primary Palette ──────────────────────────────────────────────
val GreenPrimary     = Color(0xFF2E7D32)
val GreenLight       = Color(0xFF4CAF50)
val GreenContainer   = Color(0xFFC8E6C9)
val BluePrimary      = Color(0xFF1565C0)
val BlueLight        = Color(0xFF42A5F5)
val BlueContainer    = Color(0xFFBBDEFB)
val WhiteBackground  = Color(0xFFF8FFF8)

// ── Secondary / Accent ───────────────────────────────────────────
val OrangeAccent     = Color(0xFFE65100)   // thick orange for Delete/Calculate
val OrangeLight      = Color(0xFFFF6D00)

// ── Dark Mode ────────────────────────────────────────────────────
val DarkSurface      = Color(0xFF1B2420)
val DarkBackground   = Color(0xFF121A17)
val DarkContainer    = Color(0xFF1E3329)

private val LightColorScheme = lightColorScheme(
    primary          = GreenPrimary,
    onPrimary        = Color.White,
    primaryContainer = GreenContainer,
    secondary        = BluePrimary,
    onSecondary      = Color.White,
    secondaryContainer = BlueContainer,
    tertiary         = OrangeAccent,
    onTertiary       = Color.White,
    background       = WhiteBackground,
    surface          = Color.White,
    onBackground     = Color(0xFF1A1A1A),
    onSurface        = Color(0xFF1A1A1A),
    error            = Color(0xFFB00020)
)

private val DarkColorScheme = darkColorScheme(
    primary          = GreenLight,
    onPrimary        = Color(0xFF003300),
    primaryContainer = Color(0xFF1B5E20),
    secondary        = BlueLight,
    onSecondary      = Color(0xFF001B40),
    secondaryContainer = Color(0xFF0D3B6E),
    tertiary         = OrangeLight,
    onTertiary       = Color(0xFF3E1500),
    background       = DarkBackground,
    surface          = DarkSurface,
    onBackground     = Color(0xFFE0F2E9),
    onSurface        = Color(0xFFE0F2E9),
    error            = Color(0xFFCF6679)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography(),
        content = content
    )
}
