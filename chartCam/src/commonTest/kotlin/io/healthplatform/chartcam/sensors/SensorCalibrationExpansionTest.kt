/**
 * @file SensorCalibrationExpansionTest.kt
 * Contains declarations for SensorCalibrationExpansionTest.kt.
 */
package io.healthplatform.chartcam.sensors

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests covering Section 4: Sensor Calibration, Drifts & Leveler Overlay.
 */
class SensorCalibrationExpansionTest {
    /**
     * Test low-pass filter algorithms against high-frequency accelerometer and gyroscope fluctuations.
     */
    @Test
    fun testSensorNoiseAndJitterFilter() {
        val filter = LowPassFilter(alpha = 0.15)

        // Simulate high-frequency noise oscillating +/- 6.0 degrees around a mean of 10.0 degrees
        val noisySamples =
            listOf(
                OrientationData(16.0, 16.0),
                OrientationData(4.0, 4.0),
                OrientationData(15.5, 15.5),
                OrientationData(4.5, 4.5),
                OrientationData(16.0, 16.0),
                OrientationData(5.0, 5.0),
                OrientationData(15.0, 15.0),
                OrientationData(5.0, 5.0),
                OrientationData(14.0, 14.0),
                OrientationData(6.0, 6.0),
            )

        var lastFiltered = OrientationData(0.0, 0.0)
        for (sample in noisySamples) {
            lastFiltered = filter.filter(sample)
        }

        // Filtered value should have dampened oscillations towards mean (~10.0)
        assertTrue(abs(lastFiltered.pitch - 10.0) < 3.0, "Filtered pitch should converge near the mean")
        assertTrue(abs(lastFiltered.roll - 10.0) < 3.0, "Filtered roll should converge near the mean")

        // Reset filter
        filter.reset()
        val afterReset = filter.filter(OrientationData(25.0, 25.0))
        assertEquals(25.0, afterReset.pitch)
        assertEquals(25.0, afterReset.roll)
    }

    /**
     * Verify pitch and roll calculations at 90-degree inclinations and inversion scenarios (gimbal lock).
     */
    @Test
    fun testExtremeAngleAndGimbalLockEdgeCases() {
        // Flat face-up: ax = 0, ay = 0, az = 9.8
        val flat = calculateOrientation(0.0, 0.0, 9.8)
        assertEquals(0.0, flat.pitch, 0.001)
        assertEquals(0.0, flat.roll, 0.001)

        // 90-degree pitch inclination: ax = 9.8, ay = 0, az = 0
        val pitch90 = calculateOrientation(9.8, 0.0, 0.0)
        assertEquals(90.0, pitch90.pitch, 0.001)
        assertFalse(pitch90.pitch.isNaN())
        assertFalse(pitch90.pitch.isInfinite())

        // -90-degree pitch inclination: ax = -9.8, ay = 0, az = 0
        val pitchMinus90 = calculateOrientation(-9.8, 0.0, 0.0)
        assertEquals(-90.0, pitchMinus90.pitch, 0.001)

        // 90-degree roll inclination: ax = 0, ay = 9.8, az = 0
        val roll90 = calculateOrientation(0.0, 9.8, 0.0)
        assertEquals(90.0, roll90.roll, 0.001)

        // Complete inversion (face-down): ax = 0, ay = 0, az = -9.8
        val inverted = calculateOrientation(0.0, 0.0, -9.8)
        assertFalse(inverted.pitch.isNaN())
        assertFalse(inverted.roll.isNaN())

        // Singularity / zero-g free fall: ax = 0, ay = 0, az = 0
        val singularity = calculateOrientation(0.0, 0.0, 0.0)
        assertFalse(singularity.pitch.isNaN())
        assertFalse(singularity.roll.isNaN())
    }

    /**
     * Test leveler UI behavior when hardware sensors are missing, disabled, or denied access by system policy.
     */
    @Test
    fun testSensorUnavailabilityAndPermissionDenied() =
        runTest {
            val unavailableManager = UnavailableSensorManager()
            assertFalse(unavailableManager.isAvailable)

            // Safe lifecycle methods
            unavailableManager.startListening()
            unavailableManager.stopListening()

            // Safe fallback orientation emitted
            val initialOrientation = unavailableManager.orientation.first()
            assertEquals(0.0, initialOrientation.pitch)
            assertEquals(0.0, initialOrientation.roll)
        }

    /**
     * Test sensor manager relying on default isAvailable implementation.
     */
    private class DefaultSensorManager : SensorManager {
        override val orientation: kotlinx.coroutines.flow.Flow<OrientationData> =
            kotlinx.coroutines.flow.flowOf(OrientationData(0.0, 0.0))

        override fun startListening() {}

        override fun stopListening() {}
    }

    /**
     * Test that the default implementation of isAvailable on SensorManager returns true.
     */
    @Test
    fun testDefaultSensorManagerIsAvailable() {
        val defaultManager = DefaultSensorManager()
        assertTrue(defaultManager.isAvailable)
    }

    /**
     * Ensure sensor matrices reorient correctly between portrait, landscape left, and landscape right orientations.
     */
    @Test
    fun testDynamicOrientationChange() {
        val ax = 5.0
        val ay = 2.0
        val az = 8.0

        val portrait = calculateOrientation(ax, ay, az, ScreenOrientation.PORTRAIT)
        val landscapeLeft = calculateOrientation(ax, ay, az, ScreenOrientation.LANDSCAPE_LEFT)
        val landscapeRight = calculateOrientation(ax, ay, az, ScreenOrientation.LANDSCAPE_RIGHT)
        val reversePortrait = calculateOrientation(ax, ay, az, ScreenOrientation.REVERSE_PORTRAIT)

        // Landscape left rotates 90 degrees counter-clockwise -> mappedX becomes -ay (-2.0), mappedY becomes ax (5.0)
        assertTrue(landscapeLeft.pitch < 0, "Landscape Left pitch should invert Y into negative X")
        assertTrue(landscapeLeft.roll > 0, "Landscape Left roll should map X into positive Y")

        // Landscape right rotates 90 degrees clockwise -> mappedX becomes ay (2.0), mappedY becomes -ax (-5.0)
        assertTrue(landscapeRight.pitch > 0, "Landscape Right pitch should map Y into positive X")
        assertTrue(landscapeRight.roll < 0, "Landscape Right roll should invert X into negative Y")

        // Reverse portrait inverts both axes
        assertEquals(-portrait.pitch, reversePortrait.pitch, 0.001)
        assertEquals(-portrait.roll, reversePortrait.roll, 0.001)
    }
}
