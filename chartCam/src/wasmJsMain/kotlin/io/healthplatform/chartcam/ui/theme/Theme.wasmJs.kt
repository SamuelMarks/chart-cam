/**
 * @file Theme.wasmJs.kt
 * Contains declarations for Theme.wasmJs.kt.
 *
 * WebAssembly-specific color scheme resolution.
 */
package io.healthplatform.chartcam.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Resolves the WebAssembly platform color scheme.
 *
 * @param darkTheme Whether to use dark theme colors.
 * @param dynamicColor Ignored on WebAssembly.
 * @return The resolved [ColorScheme].
 */
@Composable
actual fun resolvePlatformColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme = if (darkTheme) DarkColors else LightColors
