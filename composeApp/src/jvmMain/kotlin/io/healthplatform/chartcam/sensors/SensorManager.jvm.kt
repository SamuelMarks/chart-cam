package io.healthplatform.chartcam.sensors

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * JVM (Desktop) implementation of [SensorManager].
 * Note: Sensor functionality is stubbed and returns fixed orientation data on desktop environments.
 */
class JvmSensorManager : SensorManager {
    /**
     * A flow emitting fixed orientation updates (0.0 pitch, 0.0 roll) for desktop.
     */
    override val orientation: Flow<OrientationData> = flowOf(OrientationData(0.0, 0.0))

    /**
     * Starts listening to sensor updates. No-op on desktop.
     */
    override fun startListening() {}

    /**
     * Stops listening to sensor updates. No-op on desktop.
     */
    override fun stopListening() {}
}

/**
 * Remembers and creates a new instance of [SensorManager] for JVM.
 *
 * @return A [SensorManager] implementation for Desktop.
 */
@Composable
actual fun rememberSensorManager(): SensorManager = JvmSensorManager()
