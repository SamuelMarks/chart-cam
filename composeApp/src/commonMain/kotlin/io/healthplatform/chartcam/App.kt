package io.healthplatform.chartcam

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.healthplatform.chartcam.navigation.AppNavigation

/**
 * The Root Composable Configurator.
 */
@Composable
@Preview
fun App() { 
    MaterialTheme { 
        AppNavigation()
    } 
}