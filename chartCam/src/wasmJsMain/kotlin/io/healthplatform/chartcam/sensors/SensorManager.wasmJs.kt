/**
 * @file SensorManager.wasmJs.kt
 * @file SensorManager.wasmJs.kt
 *
 * Provides the WebAssembly (WasmJs) specific implementation of [SensorManager],
 * offering stubbed sensor functionality since direct hardware sensor access
 * is limited or unavailable on the web target.
 */
package io.healthplatform.chartcam.sensors

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * WebAssembly (WasmJs) implementation of [SensorManager].
 * Note: Sensor functionality is stubbed and returns fixed orientation data on web environments.
 */
class WasmJsSensorManager : SensorManager {
    /**
     * A flow emitting fixed orientation updates (0.0 pitch, 0.0 roll) for the WasmJs web target.
     * This acts as a stub, since real hardware sensor access is not implemented for the web.
     */
    override val orientation: Flow<OrientationData> = flowOf(OrientationData(0.0, 0.0))

    /**
     * Starts listening to sensor updates. No-op on the web target.
     */
    override fun startListening() {}

    /**
     * Stops listening to sensor updates. No-op on the web target.
     */
    override fun stopListening() {}
}

/**
 * Remembers and creates a new instance of [SensorManager] specifically for the WasmJs web target.
 *
 * @return A [SensorManager] implementation tailored for WasmJs (currently a stubbed [WasmJsSensorManager]).
 */
@Composable
actual fun rememberSensorManager(): SensorManager = WasmJsSensorManager()
