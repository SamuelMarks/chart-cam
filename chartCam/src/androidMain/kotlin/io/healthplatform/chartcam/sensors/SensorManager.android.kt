/**
 * @file SensorManager.android.kt
 * Contains declarations for SensorManager.android.kt.
 *
 * File containing the Android implementation of [SensorManager] and its composable factory function.
 */
package io.healthplatform.chartcam.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.healthplatform.chartcam.AndroidAppInit
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Android-specific implementation of the [SensorManager] interface.
 * Listens to device accelerometer events to calculate orientation data (pitch and roll).
 *
 * @param context The application or activity [Context] used to access the Android Sensor Service.
 */
class AndroidSensorManager(
    context: Context,
) : SensorManager,
    SensorEventListener {
    /**
     * The internal Android [android.hardware.SensorManager] used to register sensor listeners.
     */
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager

    /**
     * The default accelerometer [Sensor] retrieved from the system.
     */
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /**
     * Backing [MutableSharedFlow] for the orientation data. Emits the latest [OrientationData].
     */
    private val _orientation =
        MutableSharedFlow<OrientationData>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    /**
     * A [Flow] of [OrientationData] that provides real-time pitch and roll information.
     */
    override val orientation: Flow<OrientationData> = _orientation

    /**
     * Starts listening to accelerometer sensor updates if available.
     */
    override fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Stops listening to sensor updates by unregistering this listener.
     */
    override fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Called when sensor values have changed. Calculates the pitch and roll based on accelerometer values
     * and emits the calculated data to [_orientation].
     *
     * @param event The [SensorEvent] containing the new sensor data.
     */
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                // Simple Pitch/Roll calculation from Accelerometer
                val roll = atan2(x.toDouble(), sqrt(y.toDouble().pow(2) + z.toDouble().pow(2))) * (180 / Math.PI)
                val pitch = atan2(y.toDouble(), sqrt(x.toDouble().pow(2) + z.toDouble().pow(2))) * (180 / Math.PI)

                _orientation.tryEmit(OrientationData(pitch, roll))
            }
        }
    }

    /**
     * Called when the accuracy of the registered sensor has changed.
     * Currently a no-op implementation.
     *
     * @param sensor The [Sensor] whose accuracy changed.
     * @param accuracy The new accuracy of this sensor.
     */
    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {
        // no-op
    }
}

/**
 * A composable function that remembers an instance of [SensorManager] tailored for the Android platform.
 * Automatically starts listening to sensors when composed and stops when disposed.
 *
 * @return An instance of [SensorManager] (specifically [AndroidSensorManager]).
 */
@Composable
actual fun rememberSensorManager(): SensorManager {
    val context = AndroidAppInit.getContext()
    val manager = remember { AndroidSensorManager(context) }

    DisposableEffect(Unit) {
        manager.startListening()
        onDispose { manager.stopListening() }
    }
    return manager
}
