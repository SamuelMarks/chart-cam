/**
 * @file App.kt
 * Contains declarations for App.kt.
 *
 * Contains the root composable for the ChartCam application.
 * Bootstraps the UI theme and application navigation graph.
 */
package io.healthplatform.chartcam

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.navigation.AppNavigation
import io.healthplatform.chartcam.ui.AppPrivacyState
import io.healthplatform.chartcam.ui.SetupPlatformPrivacy
import io.healthplatform.chartcam.ui.currentAppPrivacyManager
import io.healthplatform.chartcam.ui.currentLanguageState
import io.healthplatform.chartcam.ui.getLayoutDirectionForLanguage
import io.healthplatform.chartcam.ui.theme.AppTheme

/**
 * The Root Composable Configurator.
 * Applies the AppTheme for Material Design 3 styling and sets up the primary
 * surface which fills the entire screen, serving as the container for the
 * main [AppNavigation] graph. Dynamically observes the current language state
 * to apply the appropriate [LocalLayoutDirection] (LTR or RTL).
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param darkTheme Whether to render the application UI in dark theme. Defaults to system setting.
 */
@Composable
@Preview
fun App(darkTheme: Boolean = isSystemInDarkTheme()) {
    SetupPlatformPrivacy()
    val currentLang by currentLanguageState.collectAsState()
    val layoutDirection = getLayoutDirectionForLanguage(currentLang)
    val privacyState by currentAppPrivacyManager.privacyState.collectAsState()
    val isLocked by currentAppPrivacyManager.isLocked.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        AppTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()

                    if (privacyState == AppPrivacyState.BACKGROUND_OBSCURED || isLocked) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Session Locked",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    val shieldText =
                                        if (isLocked) "Session Locked Due to Inactivity" else "ChartCam Security Shield"
                                    Text(
                                        text = shieldText,
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                    if (isLocked) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { currentAppPrivacyManager.unlock() }) {
                                            Text("Unlock")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
