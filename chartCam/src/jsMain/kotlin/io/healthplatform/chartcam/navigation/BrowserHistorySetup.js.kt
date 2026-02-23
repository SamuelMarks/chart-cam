package io.healthplatform.chartcam.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation

@OptIn(ExperimentalBrowserHistoryApi::class)
@Composable
actual fun SetupBrowserHistory(navController: NavController) {
    LaunchedEffect(navController) {
        navController.bindToBrowserNavigation()
    }
}
