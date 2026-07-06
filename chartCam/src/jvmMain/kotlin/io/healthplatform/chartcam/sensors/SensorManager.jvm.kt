/**
 * JVM (Desktop) implementation of sensor management.
 */
package io.healthplatform.chartcam.sensors

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * JVM (Desktop) implementation of [SensorManager].
 * Note: Sensor functionality is stubbed and returns fixed orientation data on desktop environments
 * because desktop computers generally lack physical orientation sensors.
 */
class JvmSensorManager : SensorManager {
    /**
     * A flow emitting fixed orientation updates (0.0 pitch, 0.0 roll) for desktop.
     *
     * @return A [Flow] containing a constant [OrientationData] value.
     */
    override val orientation: Flow<OrientationData> = flowOf(OrientationData(0.0, 0.0))

    /**
     * Starts listening to sensor updates. This is a no-op on desktop platforms.
     */
    override fun startListening() {}

    /**
     * Stops listening to sensor updates. This is a no-op on desktop platforms.
     */
    override fun stopListening() {}
}

/**
 * Remembers and creates a new instance of [SensorManager] for the JVM platform.
 *
 * @return A [SensorManager] implementation for Desktop environments.
 */
@Composable
actual fun rememberSensorManager(): SensorManager = JvmSensorManager()
