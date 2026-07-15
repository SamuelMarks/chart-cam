/**
 * @file BrowserHistorySetup.jvm.kt
 * Browser history setup implementation for the JVM platform.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

/**
 * Sets up browser history integration for the navigation controller.
 * This is a no-op on the JVM platform as browser history is not applicable.
 *
 * @param navController The [NavController] instance that manages app navigation.
 */
@Composable
actual fun SetupBrowserHistory(navController: NavController) {
    // No-op on JVM
}
