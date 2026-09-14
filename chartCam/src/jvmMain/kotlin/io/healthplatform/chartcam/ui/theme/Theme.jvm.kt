/**
 * @file Theme.jvm.kt
 * Contains declarations for Theme.jvm.kt.
 *
 * JVM-specific color scheme resolution.
 */
package io.healthplatform.chartcam.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Resolves the JVM platform color scheme.
 *
 * @param darkTheme Whether to use dark theme colors.
 * @param dynamicColor Ignored on JVM.
 * @return The resolved [ColorScheme].
 */
@Composable
actual fun resolvePlatformColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme = if (darkTheme) DarkColors else LightColors
