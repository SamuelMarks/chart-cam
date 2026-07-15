/**
 * @file Main.kt
 * Contains the main entry point for the web (Wasm/JS) target of the ChartCam application.
 */
package io.healthplatform.chartcam

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

/**
 * The main execution function for the web application.
 * Initializes the Jetpack Compose UI within the specified HTML viewport target.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("ComposeTarget") {
        App()
    }
}
