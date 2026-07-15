/**
 * @file BrowserHistorySetup.js.kt
 * Navigation utility for browser history integration.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavController
import androidx.navigation.bindToBrowserNavigation

/**
 * Binds the provided [NavController] to the web browser's history API.
 * This ensures the browser's back/forward buttons work correctly with compose navigation.
 *
 * @param navController The navigation controller to bind to the browser history.
 */
@OptIn(ExperimentalBrowserHistoryApi::class)
@Composable
actual fun SetupBrowserHistory(navController: NavController) {
    LaunchedEffect(navController) {
        navController.bindToBrowserNavigation()
    }
}
