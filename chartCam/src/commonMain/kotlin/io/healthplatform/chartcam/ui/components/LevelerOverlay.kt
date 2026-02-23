package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.sensors.SensorManager
import kotlin.math.abs

/**
 * A UI overlay that draws a crosshair and a bubble level based on sensor data.
 * Turns Green when perfectly level.
 */
@Composable
fun LevelerOverlay(sensorManager: SensorManager) {
    val orientation by sensorManager.orientation.collectAsState(
        initial = io.healthplatform.chartcam.sensors.OrientationData(0.0, 0.0)
    )

    // Threshold for "Green" level
    val isLevel = abs(orientation.pitch) < 3.0 && abs(orientation.roll) < 3.0
    val color = if (isLevel) Color(0xFF52854C) else Color.White

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Crosshair
        Canvas(modifier = Modifier.size(200.dp)) {
            val center = center
            val lineLength = 50.dp.toPx()

            // Horizontal Line
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(center.x - lineLength, center.y),
                end = Offset(center.x + lineLength, center.y),
                strokeWidth = 4f
            )

            // Vertical Line
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(center.x, center.y - lineLength),
                end = Offset(center.x, center.y + lineLength),
                strokeWidth = 4f
            )
            
            // Outer Circle
            drawCircle(
                color = color.copy(alpha = 0.8f),
                radius = 40.dp.toPx(),
                style = Stroke(width = 4f)
            )

            // The "Bubble"
            // We map pitch/roll to X/Y offset limited to the circle radius
            val maxDeflection = 20.0 // Degrees that map to edge of circle
            val radiusPx = 40.dp.toPx()
            
            val offsetX = (orientation.roll / maxDeflection).coerceIn(-1.0, 1.0) * radiusPx
            val offsetY = (orientation.pitch / maxDeflection).coerceIn(-1.0, 1.0) * radiusPx

            drawCircle(
                color = color,
                radius = 10.dp.toPx(),
                center = Offset(center.x + offsetX.toFloat(), center.y - offsetY.toFloat()) // Subtract pitch for Y because screen coordinates Y goes down
            )
        }
    }
}