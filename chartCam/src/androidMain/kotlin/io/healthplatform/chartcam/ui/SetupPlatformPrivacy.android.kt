/**
 * @file SetupPlatformPrivacy.android.kt
 * Contains declarations for SetupPlatformPrivacy.android.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Sets up Android lifecycle observer to update [currentAppPrivacyManager] on pause and resume.
 */
@Composable
actual fun SetupPlatformPrivacy() {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> currentAppPrivacyManager.onAppMovedToBackground()
                    Lifecycle.Event.ON_RESUME -> currentAppPrivacyManager.onAppMovedToForeground()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
