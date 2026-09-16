/**
 * @file SetupPlatformPrivacy.jvm.kt
 * Contains declarations for SetupPlatformPrivacy.jvm.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.runtime.Composable

/**
 * Desktop JVM implementation of [SetupPlatformPrivacy].
 * Window focus tracking is handled by the root Desktop Window in Main.kt.
 */
@Composable
actual fun SetupPlatformPrivacy() {
    // Handled at Desktop Window level in Main.kt
}
