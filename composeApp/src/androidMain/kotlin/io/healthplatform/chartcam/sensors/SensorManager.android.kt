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

class AndroidSensorManager(context: Context) : SensorManager, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _orientation = MutableSharedFlow<OrientationData>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val orientation: Flow<OrientationData> = _orientation

    override fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun stopListening() {
        sensorManager.unregisterListener(this)
    }

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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }
}

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