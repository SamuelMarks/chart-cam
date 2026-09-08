/**
 * @file AppSpacing.kt
 * Contains declarations for AppSpacing.kt.
 *
 * Defines centralized Material 3 layout dimension and spacing tokens for ChartCam.
 */
package io.healthplatform.chartcam.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 standard layout dimension tokens.
 */
object AppSpacing {
    /** Extra small spacing token (4dp). */
    val xs: Dp = 4.dp

    /** Small spacing token (8dp). */
    val sm: Dp = 8.dp

    /** Medium spacing token (16dp). */
    val md: Dp = 16.dp

    /** Large spacing token (24dp). */
    val lg: Dp = 24.dp

    /** Extra large spacing token (32dp). */
    val xl: Dp = 32.dp

    /** Standard minimum interactive touch target dimension (48dp). */
    val minTouchTarget: Dp = 48.dp
}
