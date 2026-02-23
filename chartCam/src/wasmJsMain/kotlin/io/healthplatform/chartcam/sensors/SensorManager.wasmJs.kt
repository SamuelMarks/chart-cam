package io.healthplatform.chartcam.sensors

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * WasmJs (Web) implementation of [SensorManager].
 * Note: Sensor functionality is stubbed and returns fixed orientation data on web environments.
 */
class WasmJsSensorManager : SensorManager {
    /**
     * A flow emitting fixed orientation updates (0.0 pitch, 0.0 roll) for WasmJs web target.
     */
    override val orientation: Flow<OrientationData> = flowOf(OrientationData(0.0, 0.0))

    /**
     * Starts listening to sensor updates. No-op on web.
     */
    override fun startListening() {}

    /**
     * Stops listening to sensor updates. No-op on web.
     */
    override fun stopListening() {}
}

/**
 * Remembers and creates a new instance of [SensorManager] for WasmJs Web.
 *
 * @return A [SensorManager] implementation for WasmJs Web.
 */
@Composable
actual fun rememberSensorManager(): SensorManager = WasmJsSensorManager()
