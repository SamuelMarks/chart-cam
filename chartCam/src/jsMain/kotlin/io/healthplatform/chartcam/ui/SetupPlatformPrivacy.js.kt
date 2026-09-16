/**
 * @file SetupPlatformPrivacy.js.kt
 * Contains declarations for SetupPlatformPrivacy.js.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * JS implementation of [SetupPlatformPrivacy] listening to page visibility and blur events.
 */
@Composable
actual fun SetupPlatformPrivacy() {
    DisposableEffect(Unit) {
        val visibilityListener: (Event) -> Unit = {
            val state = document.asDynamic().visibilityState as? String
            if (state == "hidden") {
                currentAppPrivacyManager.onAppMovedToBackground()
            } else if (state == "visible") {
                currentAppPrivacyManager.onAppMovedToForeground()
            }
        }
        val blurListener: (Event) -> Unit = {
            currentAppPrivacyManager.onAppMovedToBackground()
        }
        val focusListener: (Event) -> Unit = {
            currentAppPrivacyManager.onAppMovedToForeground()
        }

        document.addEventListener("visibilitychange", visibilityListener)
        window.addEventListener("blur", blurListener)
        window.addEventListener("focus", focusListener)

        onDispose {
            document.removeEventListener("visibilitychange", visibilityListener)
            window.removeEventListener("blur", blurListener)
            window.removeEventListener("focus", focusListener)
        }
    }
}
