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
import androidx.compose.ui.graphics.Color
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
    val color = if (isLevel) Color(LevelerConstants.LEVEL_COLOR_HEX) else Color.White

    val statusText =
        if (isLevel) {
            stringResource(Res.string.level_status_level)
        } else {
            stringResource(Res.string.level_status_tilted)
        }
    val cdStatus = stringResource(Res.string.cd_leveler_status, statusText)

    var lastAnnouncedLevel by remember { mutableStateOf<Boolean?>(null) }
    var currentAnnouncement by remember { mutableStateOf(cdStatus) }

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

            // Horizontal Line
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(center.x - lineLength, center.y),
                end = Offset(center.x + lineLength, center.y),
                strokeWidth = 4f,
            )

            // Vertical Line
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(center.x, center.y - lineLength),
                end = Offset(center.x, center.y + lineLength),
                strokeWidth = 4f,
            )

            // Outer Circle
            drawCircle(
                color = color.copy(alpha = 0.8f),
                radius = 40.dp.toPx(),
                style = Stroke(width = 4f),
            )

            // The "Bubble"
            // We map pitch/roll to X/Y offset limited to the circle radius
            val maxDeflection = LevelerConstants.MAX_DEFLECTION // Degrees that map to edge of circle
            val radiusPx = 40.dp.toPx()

            val offsetX = (roll / maxDeflection).coerceIn(-1.0, 1.0) * radiusPx
            val offsetY = (pitch / maxDeflection).coerceIn(-1.0, 1.0) * radiusPx

            drawCircle(
                color = color,
                radius = 10.dp.toPx(),
                center = Offset(center.x + offsetX.toFloat(), center.y - offsetY.toFloat()),
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
}
