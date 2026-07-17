/**
 * @file BrowserHistorySetup.android.kt
 * Contains declarations for BrowserHistorySetup.android.kt.
 *
 * File defining the Android-specific implementation for setting up browser history navigation.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

/**
 * Sets up browser history tracking or similar web-specific navigation features.
 *
 * This is a no-operation (no-op) implementation on Android since native Android
 * handles back stack and history management inherently through the OS and [NavController].
 *
 * @param navController The [NavController] instance managing the app's navigation graph.
 */
@Composable
actual fun SetupBrowserHistory(navController: NavController) {
    // No-op on Android
}
