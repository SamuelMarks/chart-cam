/**
 * iOS specific browser history setup implementation.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

/**
 * Sets up browser history integration for navigation.
 *
 * On iOS, this is a no-operation (no-op) because standard browser
 * history concepts do not apply to a native iOS application context
 * in the same way they do for web targets.
 *
 * @param navController The navigation controller to hook into. (Unused on iOS).
 */
@Composable
actual fun SetupBrowserHistory(navController: NavController) {
    // No-op on iOS
}
