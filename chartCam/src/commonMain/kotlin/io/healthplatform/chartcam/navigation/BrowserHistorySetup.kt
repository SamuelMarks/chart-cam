/**
 * @file BrowserHistorySetup.kt
 * Contains declarations for BrowserHistorySetup.kt.
 *
 * Provides setup for browser history integration across different platforms.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

/**
 * Sets up browser history integration for Web targets, acting as a no-op on other platforms.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
