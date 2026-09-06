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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.noto_sans_jp
import io.healthplatform.chartcam.ui.currentLanguageState
import io.healthplatform.chartcam.ui.isRtlLanguage
import org.jetbrains.compose.resources.Font
import kotlin.math.pow

private const val SRGB_CUTOFF = 0.04045
private const val SRGB_LOW_DIVISOR = 12.92
private const val SRGB_OFFSET = 0.055
private const val SRGB_DIVISOR = 1.055
private const val SRGB_GAMMA = 2.4
private const val LUMINANCE_RED_WEIGHT = 0.2126
private const val LUMINANCE_GREEN_WEIGHT = 0.7152
private const val LUMINANCE_BLUE_WEIGHT = 0.0722
private const val CONTRAST_OFFSET = 0.05

/** Theme colors used across the app */
private object ThemeColors {
    const val CRIMSON = 0xFFA51C30
    const val BLACK = 0xFF1E1E1E
    const val PARCHMENT = 0xFFF3F3F1
    const val SLATE = 0xFF4E5860
    const val SHADE = 0xFFBAC5C6
    const val INDIGO = 0xFF293352
    const val BLUE_BONNET = 0xFF4E84C4
    const val LIGHTER_CRIMSON = 0xFFFF6B81
    const val LIGHTER_BLUE_BONNET = 0xFF5A92D0
    const val SURFACE_DARK = 0xFF2C2C2C
    const val LIGHT_SURFACE_VARIANT = 0xFFE8E8E6
    const val LIGHT_OUTLINE = 0xFF74777F
    const val LIGHT_ERROR_CONTAINER = 0xFFFFDAD6
    const val LIGHT_ON_ERROR_CONTAINER = 0xFF410002
    const val DARK_SURFACE_VARIANT = 0xFF3C3E3D
    const val DARK_ON_SURFACE_VARIANT = 0xFFDEE0DE
    const val DARK_OUTLINE = 0xFF8E918F
    const val DARK_ERROR_CONTAINER = 0xFF690005
    const val DARK_ON_ERROR_CONTAINER = 0xFFFFDAD6
}

/** Harvard Crimson brand color used as primary color. */
internal val HarvardCrimson = Color(ThemeColors.CRIMSON)

/** Harvard Black brand color used for text and dark elements. */
internal val HarvardBlack = Color(ThemeColors.BLACK)

/** Harvard Parchment brand color used for backgrounds and light surfaces. */
internal val HarvardParchment = Color(ThemeColors.PARCHMENT)

/** Harvard Slate brand color used for secondary elements and borders. */
internal val HarvardSlate = Color(ThemeColors.SLATE)

/** Harvard Shade brand color used as an alternative secondary color. */
internal val HarvardShade = Color(ThemeColors.SHADE)

/** Harvard Indigo brand accent color used for tertiary elements. */
internal val HarvardIndigo = Color(ThemeColors.INDIGO)

/** Harvard BlueBonnet brand accent color used for tertiary elements. */
internal val HarvardBlueBonnet = Color(ThemeColors.BLUE_BONNET)

/**
 * Converts an sRGB color component to linear luminance.
 *
 * @param component The normalized color component between 0.0 and 1.0.
 * @return The linear color value.
 */
private fun convertSrgbChannelToLinear(component: Float): Double {
    val v = component.toDouble()
    return if (v <= SRGB_CUTOFF) {
        v / SRGB_LOW_DIVISOR
    } else {
        ((v + SRGB_OFFSET) / SRGB_DIVISOR).pow(SRGB_GAMMA)
    }
}

/**
 * Calculates the relative luminance of a [Color] per WCAG 2.1 specifications.
 *
 * @param color The color to evaluate.
 * @return The relative luminance value between 0.0 and 1.0.
 */
fun calculateRelativeLuminance(color: Color): Double =
    LUMINANCE_RED_WEIGHT * convertSrgbChannelToLinear(color.red) +
        LUMINANCE_GREEN_WEIGHT * convertSrgbChannelToLinear(color.green) +
        LUMINANCE_BLUE_WEIGHT * convertSrgbChannelToLinear(color.blue)

/**
 * Calculates the contrast ratio between two [Color] instances per WCAG 2.1 specifications.
 *
 * @param foreground The foreground color.
 * @param background The background color.
 * @return The contrast ratio value (e.g. 4.5 for 4.5:1).
 */
fun calculateContrastRatio(
    foreground: Color,
    background: Color,
): Double {
    val l1 = calculateRelativeLuminance(foreground)
    val l2 = calculateRelativeLuminance(background)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + CONTRAST_OFFSET) / (darker + CONTRAST_OFFSET)
}

/** Color scheme applied when the system is in light mode. */
internal val LightColors =
    lightColorScheme(
        primary = HarvardCrimson,
        onPrimary = Color.White,
        secondary = HarvardSlate,
        onSecondary = Color.White,
        tertiary = HarvardIndigo,
        onTertiary = Color.White,
        background = HarvardParchment,
        onBackground = HarvardBlack,
        surface = Color.White,
        onSurface = HarvardBlack,
        surfaceVariant = Color(ThemeColors.LIGHT_SURFACE_VARIANT),
        onSurfaceVariant = HarvardSlate,
        outline = Color(ThemeColors.LIGHT_OUTLINE),
        errorContainer = Color(ThemeColors.LIGHT_ERROR_CONTAINER),
        onErrorContainer = Color(ThemeColors.LIGHT_ON_ERROR_CONTAINER),
    )

/** Color scheme applied when the system is in dark mode. */
internal val DarkColors =
    darkColorScheme(
        primary = Color(ThemeColors.LIGHTER_CRIMSON), // Lighter crimson for better contrast in dark mode
        onPrimary = HarvardBlack,
        secondary = HarvardShade,
        onSecondary = HarvardBlack,
        tertiary = Color(ThemeColors.LIGHTER_BLUE_BONNET),
        onTertiary = HarvardBlack,
        background = HarvardBlack,
        onBackground = HarvardParchment,
        surface = Color(ThemeColors.SURFACE_DARK), // Slightly lighter than background for card separation
        onSurface = HarvardParchment,
        surfaceVariant = Color(ThemeColors.DARK_SURFACE_VARIANT),
        onSurfaceVariant = Color(ThemeColors.DARK_ON_SURFACE_VARIANT),
        outline = Color(ThemeColors.DARK_OUTLINE),
        errorContainer = Color(ThemeColors.DARK_ERROR_CONTAINER),
        onErrorContainer = Color(ThemeColors.DARK_ON_ERROR_CONTAINER),
    )

/**
 * Provides the application's customized typography using the Noto Sans JP font or default font for RTL.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param language The BCP-47 language tag to configure typography for.
 * @return A customized [Typography] object adhering to Material 3 specifications.
 */
@Composable
fun getTypography(language: String = currentLanguageState.value): Typography {
    val defaultTypography = Typography()
    val fontFamily =
        if (isRtlLanguage(language)) {
            FontFamily.Default
        } else {
            FontFamily(Font(Res.font.noto_sans_jp))
        }
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
    val currentLang = currentLanguageState.collectAsState().value

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(currentLang),
        content = content,
    )
}
