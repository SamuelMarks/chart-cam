package io.healthplatform.chartcam.sensors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

class IosSensorManager : SensorManager {
    private val motionManager = CMMotionManager()
    private val queue = NSOperationQueue.currentQueue ?: NSOperationQueue.mainQueue
    
    private val _orientation = MutableSharedFlow<OrientationData>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val orientation: Flow<OrientationData> = _orientation

    @OptIn(ExperimentalForeignApi::class)
    override fun startListening() {
        if (motionManager.accelerometerAvailable) {
            motionManager.accelerometerUpdateInterval = 0.1
            motionManager.startAccelerometerUpdatesToQueue(queue) { data, error ->
                if (data != null) {
                    data.acceleration.useContents {
                        val x = this.x
                        val y = this.y
                        val z = this.z
                        
                        val roll = kotlin.math.atan2(x, kotlin.math.sqrt(y * y + z * z)) * (180 / kotlin.math.PI)
                        val pitch = kotlin.math.atan2(y, kotlin.math.sqrt(x * x + z * z)) * (180 / kotlin.math.PI)
                        
                        _orientation.tryEmit(OrientationData(pitch, roll))
                    }
                }
            }
        }
    }

    override fun stopListening() {
        motionManager.stopAccelerometerUpdates()
    }
}

@Composable
actual fun rememberSensorManager(): SensorManager {
    val manager = remember { IosSensorManager() }
    DisposableEffect(Unit) {
        manager.startListening()
        onDispose { manager.stopListening() }
    }
    return manager
}
