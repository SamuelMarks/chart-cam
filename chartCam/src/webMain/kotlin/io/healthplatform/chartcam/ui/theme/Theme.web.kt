/**
 * @file Theme.web.kt
 * Shared Web (JS and WasmJS) color scheme resolution.
 */
package io.healthplatform.chartcam.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Resolves the Web platform color scheme for JS and WasmJS targets.
 *
 * **State & Side Effects:**
 * Purely resolves the static [DarkColors] or [LightColors] palette based on [darkTheme].
 *
 * @param darkTheme Whether to use dark theme colors.
 * @param dynamicColor Ignored on Web platforms.
 * @return The resolved [ColorScheme].
 */
@Composable
actual fun resolvePlatformColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme = if (darkTheme) DarkColors else LightColors
