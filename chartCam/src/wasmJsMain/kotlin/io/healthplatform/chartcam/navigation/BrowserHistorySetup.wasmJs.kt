/**
 * @file BrowserHistorySetup.wasmJs.kt
 *
 * Provides WebAssembly (WasmJs) specific functionality for integrating Compose Multiplatform
 * navigation with the browser's history API.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavController
import androidx.navigation.bindToBrowserNavigation

/**
 * Binds the provided [NavController] to the browser's navigation history.
 * Allows the browser's forward and backward buttons to interact seamlessly
 * with the application's internal Compose navigation state.
 *
 * @param navController The [NavController] instance managing the application's navigation stack.
 */
@OptIn(ExperimentalBrowserHistoryApi::class)
@Composable
actual fun SetupBrowserHistory(navController: NavController) {
    LaunchedEffect(navController) {
        navController.bindToBrowserNavigation()
    }
}
