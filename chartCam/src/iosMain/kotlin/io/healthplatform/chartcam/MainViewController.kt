/**
 * @file MainViewController.kt
 * Contains declarations for MainViewController.kt.
 *
 * Entry point for the iOS application UI.
 */
package io.healthplatform.chartcam

import androidx.compose.ui.window.ComposeUIViewController

/**
 * Creates the main iOS view controller hosting the Compose Multiplatform UI.
 *
 * This function returns a [platform.UIKit.UIViewController] configured by
 * [ComposeUIViewController] that displays the root [App] composable. It acts as
 * the bridge between the iOS native view hierarchy and the Compose UI.
 *
 * @return A UIViewController containing the Compose Multiplatform application.
 */
fun mainViewController() = ComposeUIViewController { App() }
