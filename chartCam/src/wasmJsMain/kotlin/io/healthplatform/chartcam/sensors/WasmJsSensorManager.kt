/**
 * @file SensorManager.wasmJs.kt
 * Provides the WebAssembly (WasmJs) specific implementation of [SensorManager].
 */
package io.healthplatform.chartcam.sensors

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val START_ORIENTATION_JS =
    "() => { " +
        "if (typeof window !== 'undefined' && window.addEventListener) { " +
        "  window._chartcam_dev_orient = (e) => { " +
        "    window._chartcam_pitch = e.beta || 0.0; " +
        "    window._chartcam_roll = e.gamma || 0.0; " +
        "  }; " +
        "  window.addEventListener('deviceorientation', window._chartcam_dev_orient); " +
        "} " +
        "}"

private const val STOP_ORIENTATION_JS =
    "() => { " +
        "if (typeof window !== 'undefined' && window._chartcam_dev_orient && window.removeEventListener) { " +
        "  window.removeEventListener('deviceorientation', window._chartcam_dev_orient); " +
        "  delete window._chartcam_dev_orient; " +
        "} " +
        "}"

private const val GET_PITCH_JS =
    "() => (typeof window !== 'undefined' && window._chartcam_pitch !== undefined) ? " +
        "Number(window._chartcam_pitch) : 0.0"

private const val GET_ROLL_JS =
    "() => (typeof window !== 'undefined' && window._chartcam_roll !== undefined) ? " +
        "Number(window._chartcam_roll) : 0.0"

private const val SENSOR_POLL_INTERVAL_MS = 32L

@JsFun(START_ORIENTATION_JS)
private external fun startOrientationJs()

@JsFun(STOP_ORIENTATION_JS)
private external fun stopOrientationJs()

@JsFun(GET_PITCH_JS)
private external fun getPitchJs(): Double

@JsFun(GET_ROLL_JS)
private external fun getRollJs(): Double

/**
 * WebAssembly (WasmJs) implementation of [SensorManager].
 * Hooks into HTML5 device orientation events when supported and emits real-time updates.
 */
class WasmJsSensorManager : SensorManager {
    private val _orientation = MutableStateFlow(OrientationData(0.0, 0.0))
    private val scope = CoroutineScope(Dispatchers.Default)
    private var pollingJob: Job? = null

    /**
     * A flow emitting orientation updates for the WasmJs web target.
     */
    override val orientation: Flow<OrientationData> = _orientation.asStateFlow()

    /**
     * Starts listening to sensor updates on web target.
     */
    override fun startListening() {
        runCatching {
            startOrientationJs()
            pollingJob?.cancel()
            pollingJob =
                scope.launch {
                    while (isActive) {
                        val p = getPitchJs()
                        val r = getRollJs()
                        if (p != _orientation.value.pitch || r != _orientation.value.roll) {
                            _orientation.value = OrientationData(p, r)
                        }
                        delay(SENSOR_POLL_INTERVAL_MS)
                    }
                }
        }
    }

    /**
     * Stops listening to sensor updates on web target.
     */
    override fun stopListening() {
        runCatching {
            pollingJob?.cancel()
            pollingJob = null
            stopOrientationJs()
        }
    }

    /**
     * Explicitly updates orientation state for manual updates or test assertions.
     *
     * @param pitch The pitch angle in degrees.
     * @param roll The roll angle in degrees.
     */
    fun updateOrientation(
        pitch: Double,
        roll: Double,
    ) {
        _orientation.value = OrientationData(pitch, roll)
    }
}

/**
 * Remembers and creates a new instance of [SensorManager] specifically for the WasmJs web target.
 *
 * @return A [SensorManager] implementation tailored for WasmJs.
 */
@Composable
actual fun rememberSensorManager(): SensorManager = WasmJsSensorManager()
