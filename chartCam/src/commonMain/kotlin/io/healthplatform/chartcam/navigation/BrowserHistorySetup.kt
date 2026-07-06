/**
 * Provides setup for browser history integration across different platforms.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

/**
 * Sets up browser history integration for Web targets, acting as a no-op on other platforms.
 *
 * @param navController The navigation controller used to listen for and manage route changes.
 */
@Composable
expect fun SetupBrowserHistory(
    /**
     * The navigation controller used to listen for and manage route changes.
     */
    navController: NavController,
)
