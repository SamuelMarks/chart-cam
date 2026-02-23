package io.healthplatform.chartcam.sensors

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/**
 * Data class representing device orientation for leveling.
 *
 * @property pitch The pitch angle in degrees (up/down tilt).
 * @property roll The roll angle in degrees (left/right tilt).
 */
data class OrientationData(
    val pitch: Double,
    val roll: Double
)

/**
 * Interface for accessing device sensors.
 */
interface SensorManager {
    /**
     * A flow emitting orientation updates.
     */
    val orientation: Flow<OrientationData>

    /**
     * Starts listening to sensor updates.
     */
    fun startListening()

    /**
     * Stops listening to sensor updates.
     */
    fun stopListening()
}

/**
 * Factory to retrieve the sensor manager.
 */
@Composable
expect fun rememberSensorManager(): SensorManager