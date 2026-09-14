/**
 * @file Theme.android.kt
 * Contains declarations for Theme.android.kt.
 *
 * Android-specific color scheme resolution supporting Material You Dynamic Color on Android 12+.
 */
package io.healthplatform.chartcam.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Resolves the Android platform color scheme with optional dynamic color support on Android 12+ (API 31+).
 *
 * @param darkTheme Whether to use dark theme colors.
 * @param dynamicColor Whether to apply Android 12+ dynamic theming.
 * @return The resolved [ColorScheme].
 */
@Composable
actual fun resolvePlatformColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme =
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }
