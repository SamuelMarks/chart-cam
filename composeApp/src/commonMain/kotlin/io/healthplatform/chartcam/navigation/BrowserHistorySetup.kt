package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

/**
 * Sets up browser history integration for Web targets.
 */
@Composable
expect fun SetupBrowserHistory(navController: NavController)
