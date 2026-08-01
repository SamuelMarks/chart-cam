/**
 * @file Theme.kt
 * Contains declarations for Theme.kt.
 *
 * Defines the main application theme, including colors, typography, and material design shapes.
 */
package io.healthplatform.chartcam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.noto_sans_jp
import org.jetbrains.compose.resources.Font

/** Theme colors used across the app */
private object ThemeColors {
    const val CRIMSON = 0xFFA51C30
    const val BLACK = 0xFF1E1E1E
    const val PARCHMENT = 0xFFF3F3F1
    const val SLATE = 0xFF8996A0
    const val SHADE = 0xFFBAC5C6
    const val INDIGO = 0xFF293352
    const val BLUE_BONNET = 0xFF4E84C4
    const val LIGHTER_CRIMSON = 0xFFC9364C
    const val SURFACE_DARK = 0xFF2C2C2C
}

/** Harvard Crimson brand color used as primary color. */
private val HarvardCrimson = Color(ThemeColors.CRIMSON)

/** Harvard Black brand color used for text and dark elements. */
private val HarvardBlack = Color(ThemeColors.BLACK)

/** Harvard Parchment brand color used for backgrounds and light surfaces. */
private val HarvardParchment = Color(ThemeColors.PARCHMENT)

/** Harvard Slate brand color used for secondary elements and borders. */
private val HarvardSlate = Color(ThemeColors.SLATE)

/** Harvard Shade brand color used as an alternative secondary color. */
private val HarvardShade = Color(ThemeColors.SHADE)

/** Harvard Indigo brand accent color used for tertiary elements. */
private val HarvardIndigo = Color(ThemeColors.INDIGO)

/** Harvard BlueBonnet brand accent color used for tertiary elements. */
private val HarvardBlueBonnet = Color(ThemeColors.BLUE_BONNET)

/** Color scheme applied when the system is in light mode. */
private val LightColors =
    lightColorScheme(
        primary = HarvardCrimson,
        onPrimary = Color.White,
        secondary = HarvardSlate,
        onSecondary = HarvardBlack,
        tertiary = HarvardIndigo,
        onTertiary = Color.White,
        background = HarvardParchment,
        onBackground = HarvardBlack,
        surface = Color.White,
        onSurface = HarvardBlack,
    )

/** Color scheme applied when the system is in dark mode. */
private val DarkColors =
    darkColorScheme(
        primary = Color(ThemeColors.LIGHTER_CRIMSON), // Lighter crimson for better contrast in dark mode
        onPrimary = Color.White,
        secondary = HarvardShade,
        onSecondary = HarvardBlack,
        tertiary = HarvardBlueBonnet,
        onTertiary = HarvardBlack,
        background = HarvardBlack,
        onBackground = HarvardParchment,
        surface = Color(ThemeColors.SURFACE_DARK), // Slightly lighter than background for card separation
        onSurface = HarvardParchment,
    )

/**
 * Provides the application's customized typography using the Noto Sans JP font.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @return A customized [Typography] object adhering to Material 3 specifications.
 */
@Composable
fun getTypography(): Typography {
    val defaultTypography = Typography()
    val fontFamily = FontFamily(Font(Res.font.noto_sans_jp))
    return Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily),
    )
}

/**
 * Main application theme defining the colors, typography, and shapes.
 * This ensures consistency with Material Design 3 guidelines.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param darkTheme Whether to use the dark theme. Defaults to the system setting.
 * @param content The composable content to apply the theme to.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(),
        content = content,
    )
}
