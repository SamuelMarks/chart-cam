package io.healthplatform.chartcam

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.healthplatform.chartcam.navigation.AppNavigation
import io.healthplatform.chartcam.ui.theme.AppTheme

/**
 * The Root Composable Configurator.
 * Applies the AppTheme for Material Design 3 styling.
 */
@Composable
@Preview
fun App() { 
    val currentLang by io.healthplatform.chartcam.ui.currentLanguageState.collectAsState()
    AppTheme {
        androidx.compose.runtime.key(currentLang) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                AppNavigation()
            }
        }
    } 
}
