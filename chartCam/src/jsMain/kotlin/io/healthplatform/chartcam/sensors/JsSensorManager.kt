/**
 * @file SensorManager.js.kt
 * Sensor management classes and functions for the JS platform.
 */
package io.healthplatform.chartcam.sensors

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * JS (Web) implementation of [SensorManager] utilizing HTML5 DeviceOrientation API.
 */
class JsSensorManager : SensorManager {
    private val _orientation = MutableStateFlow(OrientationData(0.0, 0.0))

    /**
     * A flow emitting orientation updates from HTML5 DeviceOrientationEvent.
     */
    override val orientation: Flow<OrientationData> = _orientation.asStateFlow()

    private val listener: (dynamic) -> Unit = { event ->
        val pitch = (event.beta as? Double) ?: 0.0
        val roll = (event.gamma as? Double) ?: 0.0
        _orientation.value = OrientationData(pitch, roll)
    }

    /**
     * Starts listening to HTML5 device orientation events.
     */
    override fun startListening() {
        runCatching {
            kotlinx.browser.window.addEventListener("deviceorientation", listener)
        }
    }

    /**
     * Stops listening to HTML5 device orientation events.
     */
    override fun stopListening() {
        runCatching {
            kotlinx.browser.window.removeEventListener("deviceorientation", listener)
        }
    }
}

/**
 * Remembers and creates a new instance of [SensorManager] for JS Web.
 *
 * @return A [SensorManager] implementation for JS Web.
 */
@Composable
actual fun rememberSensorManager(): SensorManager = JsSensorManager()
