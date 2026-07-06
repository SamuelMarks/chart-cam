/**
 * Provides interfaces and data structures for managing device sensors (e.g., orientation).
 */
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
    val roll: Double,
)

/**
 * Interface for accessing device sensors and monitoring device orientation.
 */
interface SensorManager {
    /**
     * A flow emitting continuous orientation updates.
     */
    val orientation: Flow<OrientationData>

    /**
     * Starts listening to sensor updates.
     */
    fun startListening()

    /**
     * Stops listening to sensor updates, conserving battery when not in use.
     */
    fun stopListening()
}

/**
 * Factory composable to retrieve a platform-specific instance of the [SensorManager].
 *
 * @return A [SensorManager] instance.
 */
@Composable
expect fun rememberSensorManager(): SensorManager
