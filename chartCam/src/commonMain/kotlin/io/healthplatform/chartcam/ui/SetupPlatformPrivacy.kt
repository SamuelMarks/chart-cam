/**
 * @file SetupPlatformPrivacy.kt
 * Contains declarations for SetupPlatformPrivacy.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.runtime.Composable

/**
 * Sets up platform-specific lifecycle observers to update [currentAppPrivacyManager]
 * when the application transitions between the active foreground and background.
 */
@Composable
expect fun SetupPlatformPrivacy()
