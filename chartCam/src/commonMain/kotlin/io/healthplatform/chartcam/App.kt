/**
 * @file App.kt
 * Contains declarations for App.kt.
 *
 * Contains the root composable for the ChartCam application.
 * Bootstraps the UI theme and application navigation graph.
 */
package io.healthplatform.chartcam

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.healthplatform.chartcam.navigation.AppNavigation
import io.healthplatform.chartcam.ui.theme.AppTheme

/**
 * The Root Composable Configurator.
 * Applies the AppTheme for Material Design 3 styling and sets up the primary
 * surface which fills the entire screen, serving as the container for the
 * main [AppNavigation] graph.
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param darkTheme Whether to render the application UI in dark theme. Defaults to system setting.
 */
@Composable
@Preview
fun App(darkTheme: Boolean = isSystemInDarkTheme()) {
    AppTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
        ) {
            AppNavigation()
        }
    }
}
