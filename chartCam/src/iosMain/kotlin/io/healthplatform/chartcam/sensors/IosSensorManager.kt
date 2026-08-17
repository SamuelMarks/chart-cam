/**
 * @file SensorManager.ios.kt
 * Contains declarations for SensorManager.ios.kt.
 *
 * iOS implementation of the sensor manager.
 */
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

/**
 * iOS-specific implementation of [SensorManager].
 *
 * This class uses the CoreMotion framework's [CMMotionManager] to access accelerometer
 * data and calculate device orientation (pitch and roll). It emits this data as a Flow
 * of [OrientationData].
 */
class IosSensorManager : SensorManager {
    /**
     * The iOS CoreMotion manager used to access device sensors.
     */
    private val motionManager = CMMotionManager()

    /**
     * The operation queue used to receive accelerometer updates.
     */
    private val queue = NSOperationQueue.currentQueue ?: NSOperationQueue.mainQueue

    /**
     * A private shared flow to emit orientation updates.
     */
    private val _orientation =
        MutableSharedFlow<OrientationData>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    /**
     * A public Flow emitting the latest calculated [OrientationData].
     */
    override val orientation: Flow<OrientationData> = _orientation

    /**
     * Starts listening to accelerometer updates and emitting orientation data.
     *
     * It configures the [CMMotionManager] to report updates to the specified [queue]
     * at 10Hz (0.1 second interval). Calculations are performed to convert raw
     * acceleration data (X, Y, Z) into pitch and roll angles in degrees.
     */
    @OptIn(ExperimentalForeignApi::class)
    override fun startListening() {
        if (motionManager.accelerometerAvailable) {
            motionManager.accelerometerUpdateInterval = UPDATE_INTERVAL_SECONDS
            motionManager.startAccelerometerUpdatesToQueue(queue) { data, error ->
                if (data != null) {
                    data.acceleration.useContents {
                        val x = this.x
                        val y = this.y
                        val z = this.z

                        val roll = kotlin.math.atan2(x, kotlin.math.sqrt(y * y + z * z)) * RADIANS_TO_DEGREES
                        val pitch = kotlin.math.atan2(y, kotlin.math.sqrt(x * x + z * z)) * RADIANS_TO_DEGREES

                        _orientation.tryEmit(OrientationData(pitch, roll))
                    }
                }
            }
        }
    }

    /**
     * Stops listening to accelerometer updates.
     *
     * This halts updates from the [CMMotionManager], conserving battery and resources
     * when orientation data is not needed.
     */
    override fun stopListening() {
        motionManager.stopAccelerometerUpdates()
    }

    /** Companion object */
    companion object {
        private const val UPDATE_INTERVAL_SECONDS = 0.1
        private const val RADIANS_TO_DEGREES = 180 / kotlin.math.PI
    }
}

/**
 * Creates, remembers, and manages the lifecycle of an iOS [SensorManager].
 *
 * It provides an instance of [IosSensorManager], automatically starting sensor reading
 * when this Composable enters the composition and stopping when it leaves via a
 * [DisposableEffect].
 *
 * @return An iOS-specific instance of [SensorManager] managing device sensors.
 */
@Composable
actual fun rememberSensorManager(): SensorManager {
    val manager = remember { IosSensorManager() }
    DisposableEffect(Unit) {
        manager.startListening()
        onDispose { manager.stopListening() }
    }
    return manager
}
