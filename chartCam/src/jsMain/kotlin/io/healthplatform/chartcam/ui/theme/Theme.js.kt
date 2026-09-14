/**
 * @file Theme.js.kt
 * Contains declarations for Theme.js.kt.
 *
 * JS-specific color scheme resolution.
 */
package io.healthplatform.chartcam.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Resolves the JS platform color scheme.
 *
 * @param darkTheme Whether to use dark theme colors.
 * @param dynamicColor Ignored on JS.
 * @return The resolved [ColorScheme].
 */
@Composable
actual fun resolvePlatformColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme = if (darkTheme) DarkColors else LightColors
