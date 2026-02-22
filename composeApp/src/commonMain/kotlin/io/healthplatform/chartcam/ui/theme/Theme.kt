package io.healthplatform.chartcam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    secondary = Color(0xFF009688),
    onSecondary = Color.White,
    background = Color(0xFFF5F5F6),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF004D40),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
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
