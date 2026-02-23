package io.healthplatform.chartcam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Harvard Medical School Brand Colors
private val HarvardCrimson = Color(0xFFA51C30)
private val HarvardBlack = Color(0xFF1E1E1E)
private val HarvardParchment = Color(0xFFF3F3F1)
private val HarvardSlate = Color(0xFF8996A0)
private val HarvardShade = Color(0xFFBAC5C6)

// Accents
private val HarvardIndigo = Color(0xFF293352)
private val HarvardBlueBonnet = Color(0xFF4E84C4)

private val LightColors = lightColorScheme(
    primary = HarvardCrimson,
    onPrimary = Color.White,
    secondary = HarvardSlate,
    onSecondary = HarvardBlack,
    tertiary = HarvardIndigo,
    onTertiary = Color.White,
    background = HarvardParchment,
    onBackground = HarvardBlack,
    surface = Color.White,
    onSurface = HarvardBlack
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC9364C), // Lighter crimson for better contrast in dark mode
    onPrimary = Color.White,
    secondary = HarvardShade,
    onSecondary = HarvardBlack,
    tertiary = HarvardBlueBonnet,
    onTertiary = HarvardBlack,
    background = HarvardBlack,
    onBackground = HarvardParchment,
    surface = Color(0xFF2C2C2C), // Slightly lighter than background for card separation
    onSurface = HarvardParchment
)

/**
 * Main application theme defining the colors, typography, and shapes.
 * This ensures consistency with Material Design 3 guidelines.
 * 
 * @param darkTheme Whether to use the dark theme (defaults to system setting).
 * @param content The composable content to apply the theme to.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
