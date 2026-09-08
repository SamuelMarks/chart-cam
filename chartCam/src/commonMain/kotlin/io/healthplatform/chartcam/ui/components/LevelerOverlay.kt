/**
 * @file LevelerOverlay.kt
 * Contains declarations for LevelerOverlay.kt.
 *
 * Contains the visual overlay for the camera leveler tool.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_leveler_status
import chartcam.chartcam.generated.resources.level_status_level
import chartcam.chartcam.generated.resources.level_status_tilted
import io.healthplatform.chartcam.sensors.SensorManager
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/**
 * A UI overlay that draws a crosshair and a bubble level based on real-time device sensor data.
 * Turns Green when perfectly level (pitch and roll < 3 degrees).
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param sensorManager The sensor manager that emits orientation data.
 */
@Composable
fun LevelerOverlay(sensorManager: SensorManager) {
    val orientation by sensorManager.orientation.collectAsState(
        initial =
            io.healthplatform.chartcam.sensors
                .OrientationData(0.0, 0.0),
    )

    LevelerOverlay(pitch = orientation.pitch.toFloat(), roll = orientation.roll.toFloat())
}

/**
 * Stateless implementation of the leveler overlay.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param pitch Device pitch in degrees.
 * @param roll Device roll in degrees.
 * @param tolerance Degrees within which the device is considered level.
 */
@Composable
fun LevelerOverlay(
    pitch: Float,
    roll: Float,
    tolerance: Float = 3.0f,
) {
    val isLevel = abs(pitch) < tolerance && abs(roll) < tolerance
    val color =
        if (isLevel) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val statusText =
        if (isLevel) {
            stringResource(Res.string.level_status_level)
        } else {
            stringResource(Res.string.level_status_tilted)
        }
    val cdStatus = stringResource(Res.string.cd_leveler_status, statusText)

    var lastAnnouncedLevel by remember { mutableStateOf<Boolean?>(null) }
    var currentAnnouncement by remember { mutableStateOf(cdStatus) }
    val surfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(isLevel) {
        delay(LevelerConstants.DEBOUNCE_DELAY_MS)
        if (lastAnnouncedLevel != isLevel) {
            lastAnnouncedLevel = isLevel
            currentAnnouncement = cdStatus
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Crosshair
        Canvas(
            modifier =
                Modifier.size(200.dp).semantics(mergeDescendants = true) {
                    contentDescription = currentAnnouncement
                    liveRegion = LiveRegionMode.Polite
                },
        ) {
            val center = center
            val lineLength = 50.dp.toPx()
            val strokeWidth =
                if (isLevel) {
                    LevelerConstants.LEVEL_STROKE_WIDTH
                } else {
                    LevelerConstants.TILTED_STROKE_WIDTH
                }
            val lineAlpha =
                if (isLevel) {
                    LevelerConstants.LEVEL_LINE_ALPHA
                } else {
                    LevelerConstants.TILTED_LINE_ALPHA
                }
            val outlineColor = surfaceColor.copy(alpha = LevelerConstants.OUTLINE_ALPHA)
            val outlineStrokeWidth = strokeWidth + LevelerConstants.OUTLINE_EXTRA_STROKE

            // High-contrast backing outline for crosshair lines
            drawLine(
                color = outlineColor,
                start = Offset(center.x - lineLength, center.y),
                end = Offset(center.x + lineLength, center.y),
                strokeWidth = outlineStrokeWidth,
            )
            drawLine(
                color = outlineColor,
                start = Offset(center.x, center.y - lineLength),
                end = Offset(center.x, center.y + lineLength),
                strokeWidth = outlineStrokeWidth,
            )

            // High-contrast backing outline for outer circle
            drawCircle(
                color = outlineColor,
                radius = 40.dp.toPx(),
                style = Stroke(width = outlineStrokeWidth),
            )

            // Horizontal Line
            drawLine(
                color = color.copy(alpha = lineAlpha),
                start = Offset(center.x - lineLength, center.y),
                end = Offset(center.x + lineLength, center.y),
                strokeWidth = strokeWidth,
            )

            // Vertical Line
            drawLine(
                color = color.copy(alpha = lineAlpha),
                start = Offset(center.x, center.y - lineLength),
                end = Offset(center.x, center.y + lineLength),
                strokeWidth = strokeWidth,
            )

            // Outer Circle
            drawCircle(
                color = color.copy(alpha = if (isLevel) 1.0f else 0.8f),
                radius = 40.dp.toPx(),
                style = Stroke(width = strokeWidth),
            )

            // Secondary non-color geometric cue: inner concentric lock ring appears when level
            if (isLevel) {
                drawCircle(
                    color = color.copy(alpha = LevelerConstants.LEVEL_LOCK_RING_ALPHA),
                    radius = 20.dp.toPx(),
                    style = Stroke(width = LevelerConstants.LEVEL_LOCK_RING_STROKE_WIDTH),
                )
            }

            // The "Bubble"
            // We map pitch/roll to X/Y offset limited to the circle radius
            val maxDeflection = LevelerConstants.MAX_DEFLECTION // Degrees that map to edge of circle
            val radiusPx = 40.dp.toPx()

            val offsetX = (roll / maxDeflection).coerceIn(-1.0, 1.0) * radiusPx
            val offsetY = (pitch / maxDeflection).coerceIn(-1.0, 1.0) * radiusPx
            val bubbleCenter = Offset(center.x + offsetX.toFloat(), center.y - offsetY.toFloat())

            drawCircle(
                color = outlineColor,
                radius = LevelerConstants.BUBBLE_OUTLINE_RADIUS_DP.dp.toPx(),
                center = bubbleCenter,
            )
            drawCircle(
                color = color,
                radius = 10.dp.toPx(),
                center = bubbleCenter,
                // Subtract pitch for Y because screen coordinates Y goes down
            )
        }
    }
}

/**
 * Constants used in LevelerOverlay.
 */
private object LevelerConstants {
    const val LEVEL_COLOR_HEX = 0xFF52854C
    const val MAX_DEFLECTION = 20.0
    const val DEBOUNCE_DELAY_MS = 500L
    const val LEVEL_STROKE_WIDTH = 8f
    const val TILTED_STROKE_WIDTH = 4f
    const val LEVEL_LINE_ALPHA = 0.9f
    const val TILTED_LINE_ALPHA = 0.5f
    const val LEVEL_LOCK_RING_ALPHA = 0.7f
    const val LEVEL_LOCK_RING_STROKE_WIDTH = 3f
    const val OUTLINE_ALPHA = 0.85f
    const val OUTLINE_EXTRA_STROKE = 4f
    const val BUBBLE_OUTLINE_RADIUS_DP = 12
}
