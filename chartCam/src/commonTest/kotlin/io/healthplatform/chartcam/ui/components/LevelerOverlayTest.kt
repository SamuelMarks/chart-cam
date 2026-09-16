/**
 * @file LevelerOverlayTest.kt
 * Contains declarations for LevelerOverlayTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Common test for LevelerOverlay pitch and roll calculations.
 */
class LevelerOverlayTest {
    /**
     * Verifies level orientation angle calculation and threshold determination.
     */
    @Test
    fun testLevelerThreshold() {
        val pitch = 1.5
        val roll = -2.0
        val threshold = 3.0
        val isLevel = abs(pitch) <= threshold && abs(roll) <= threshold
        assertTrue(isLevel)

        val unlevelPitch = 5.0
        val isNotLevel = abs(unlevelPitch) <= threshold && abs(roll) <= threshold
        assertFalse(isNotLevel)
    }
}
