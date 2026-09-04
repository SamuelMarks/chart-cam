/**
 * @file SensorManager.kt
 * Contains declarations for SensorManager.kt.
 *
 * Provides interfaces and data structures for managing device sensors (e.g., orientation).
 */
package io.healthplatform.chartcam.sensors

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Supported screen display orientations for sensor coordinate mapping.
 */
enum class ScreenOrientation {
    /** Standard portrait orientation. */
    PORTRAIT,

    /** Landscape orientation rotated 90 degrees counter-clockwise. */
    LANDSCAPE_LEFT,

    /** Landscape orientation rotated 90 degrees clockwise. */
    LANDSCAPE_RIGHT,

    /** Inverted upside-down portrait orientation. */
    REVERSE_PORTRAIT,
}

/**
 * Data class representing device orientation for leveling.
 *
 * @property pitch The pitch angle in degrees (up/down tilt).
 * @property roll The roll angle in degrees (left/right tilt).
 */
data class OrientationData(
    val pitch: Double,
    val roll: Double,
)

/**
 * Exponential moving average (low-pass) filter to eliminate high-frequency noise and jitter
 * from raw accelerometer and gyroscope samples.
 *
 * @property alpha Smoothing factor between 0.0 (maximum smoothing) and 1.0 (no filtering).
 */
class LowPassFilter(
    val alpha: Double = 0.2,
) {
    private var prevPitch: Double? = null
    private var prevRoll: Double? = null

    /**
     * Filters an individual scalar value with low-pass smoothing.
     *
     * @param current The current raw sample.
     * @param previous The previous filtered sample or null.
     * @return The filtered sample value.
     */
    fun filterScalar(
        current: Double,
        previous: Double?,
    ): Double =
        if (previous == null) {
            current
        } else {
            alpha * current + (1.0 - alpha) * previous
        }

    /**
     * Filters continuous device orientation data.
     *
     * @param raw The raw unsmoothed [OrientationData].
     * @return The smoothed [OrientationData].
     */
    fun filter(raw: OrientationData): OrientationData {
        val newPitch = filterScalar(raw.pitch, prevPitch)
        val newRoll = filterScalar(raw.roll, prevRoll)
        prevPitch = newPitch
        prevRoll = newRoll
        return OrientationData(pitch = newPitch, roll = newRoll)
    }

    /**
     * Resets the filter internal state.
     */
    fun reset() {
        prevPitch = null
        prevRoll = null
    }
}

/**
 * Computes calibrated pitch and roll angles from 3-axis accelerometer values,
 * applying gimbal lock protection and screen orientation coordinate re-mapping.
 *
 * @param ax Accelerometer X-axis value in m/s^2.
 * @param ay Accelerometer Y-axis value in m/s^2.
 * @param az Accelerometer Z-axis value in m/s^2.
 * @param screenOrientation Current visual screen orientation.
 * @return Calibrated [OrientationData] in degrees.
 */
fun calculateOrientation(
    ax: Double,
    ay: Double,
    az: Double,
    screenOrientation: ScreenOrientation = ScreenOrientation.PORTRAIT,
): OrientationData {
    // Coordinate transformation based on display rotation
    val (mappedX, mappedY) =
        when (screenOrientation) {
            ScreenOrientation.PORTRAIT -> Pair(ax, ay)
            ScreenOrientation.LANDSCAPE_LEFT -> Pair(-ay, ax)
            ScreenOrientation.LANDSCAPE_RIGHT -> Pair(ay, -ax)
            ScreenOrientation.REVERSE_PORTRAIT -> Pair(-ax, -ay)
        }

    val pitchDenom = sqrt(mappedY * mappedY + az * az)
    val rollDenom = sqrt(mappedX * mappedX + az * az)

    // Handle extreme angles and gimbal lock (denom near 0)
    val rawPitch =
        if (pitchDenom < OrientationConstants.EPSILON) {
            if (mappedX > 0) OrientationConstants.RIGHT_ANGLE else -OrientationConstants.RIGHT_ANGLE
        } else {
            atan2(mappedX, pitchDenom) * OrientationConstants.RAD_TO_DEG / PI
        }

    val rawRoll =
        if (rollDenom < OrientationConstants.EPSILON) {
            if (mappedY > 0) OrientationConstants.RIGHT_ANGLE else -OrientationConstants.RIGHT_ANGLE
        } else {
            atan2(mappedY, rollDenom) * OrientationConstants.RAD_TO_DEG / PI
        }

    return OrientationData(pitch = rawPitch, roll = rawRoll)
}

/**
 * Constants used for orientation angle calculations.
 */
private object OrientationConstants {
    const val EPSILON = 1e-6
    const val RIGHT_ANGLE = 90.0
    const val RAD_TO_DEG = 180.0
}

/**
 * Interface for accessing device sensors and monitoring device orientation.
 */
interface SensorManager {
    /**
     * Indicates whether hardware sensors are available and permitted on this device.
     */
    val isAvailable: Boolean
        get() = true

    /**
     * A flow emitting continuous orientation updates.
     */
    val orientation: Flow<OrientationData>

    /**
     * Starts listening to sensor updates.
     */
    fun startListening()

    /**
     * Stops listening to sensor updates, conserving battery when not in use.
     */
    fun stopListening()
}

/**
 * Fallback [SensorManager] implementation for when hardware sensors are missing, disabled,
 * or permission is denied by system policy.
 */
class UnavailableSensorManager : SensorManager {
    /**
     * Always returns false for unavailable hardware.
     */
    override val isAvailable: Boolean = false

    /**
     * Emits a neutral level orientation state so the UI does not crash or hang.
     */
    override val orientation: Flow<OrientationData> =
        flowOf(OrientationData(pitch = 0.0, roll = 0.0))

    /** No-op for unavailable hardware. */
    override fun startListening() {
        // No-op for unavailable hardware
    }

    /** No-op for unavailable hardware. */
    override fun stopListening() {
        // No-op for unavailable hardware
    }
}

/**
 * Factory composable to retrieve a platform-specific instance of the [SensorManager].
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @return A [SensorManager] instance.
 */
@Composable
expect fun rememberSensorManager(): SensorManager
