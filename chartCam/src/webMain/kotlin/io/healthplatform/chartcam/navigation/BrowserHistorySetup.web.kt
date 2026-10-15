/**
 * @file BrowserHistorySetup.web.kt
 * Shared Web (JS and WasmJS) navigation utility for browser history integration.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavController
import androidx.navigation.bindToBrowserNavigation

/**
 * Binds the provided [NavController] to the web browser's history API across JS and WasmJS targets.
 *
 * **State & Side Effects:**
 * Launches a side-effect bound to [navController] that synchronizes navigation state with browser history.
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
