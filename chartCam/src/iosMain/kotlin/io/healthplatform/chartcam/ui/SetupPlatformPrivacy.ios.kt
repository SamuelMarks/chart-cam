/**
 * @file SetupPlatformPrivacy.ios.kt
 * Contains declarations for SetupPlatformPrivacy.ios.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

/**
 * Sets up iOS NSNotificationCenter observers to update [currentAppPrivacyManager] on active/resign.
 */
@Composable
actual fun SetupPlatformPrivacy() {
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val resignObserver =
            center.addObserverForName(
                name = UIApplicationWillResignActiveNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) {
                currentAppPrivacyManager.onAppMovedToBackground()
            }
        val activeObserver =
            center.addObserverForName(
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) {
                currentAppPrivacyManager.onAppMovedToForeground()
            }
        onDispose {
            center.removeObserver(resignObserver)
            center.removeObserver(activeObserver)
        }
    }
}
