/**
 * @file SensorManagerWasmJsTest.kt
 * Unit tests verifying WasmJs sensor manager lifecycle and orientation updates.
 */
package io.healthplatform.chartcam.sensors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test suite for [WasmJsSensorManager].
 */
class SensorManagerWasmJsTest {
    /**
     * Verifies that start and stop listening complete without throwing exceptions.
     */
    @Test
    fun testStartAndStopListening() {
        val manager = WasmJsSensorManager()
        assertTrue(manager.isAvailable)

        manager.startListening()
        manager.stopListening()
    }

    /**
     * Verifies that manual orientation updates correctly propagate to the flow.
     */
    @Test
    fun testUpdateOrientationPropagation() {
        val manager = WasmJsSensorManager()
        val expectedPitch = 12.5
        val expectedRoll = -4.2

        manager.updateOrientation(expectedPitch, expectedRoll)

        val flow = manager.orientation
        var current: OrientationData? = null
        val sub =
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).run {
                // Read current value
                current = (flow as? kotlinx.coroutines.flow.StateFlow<OrientationData>)?.value
            }
        assertEquals(expectedPitch, current?.pitch)
        assertEquals(expectedRoll, current?.roll)
    }
}
