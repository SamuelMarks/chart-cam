/**
 * @file SensorManagerCommonTest.kt
 * Contains declarations for SensorManagerCommonTest.kt.
 */
package io.healthplatform.chartcam.sensors

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Common tests for the [SensorManager] and [OrientationData].
 */
class SensorManagerCommonTest {
    /**
     * Mock implementation of [SensorManager].
     */
    class MockSensorManager : SensorManager {
        /** Exposes the current orientation. */
        override val orientation = MutableStateFlow(OrientationData(0.0, 0.0))

        /** Indicator if it is currently listening. */
        var isListening = false

        /** Starts listening. */
        override fun startListening() {
            isListening = true
        }

        /** Stops listening. */
        override fun stopListening() {
            isListening = false
        }
    }

    /**
     * Test the properties of [OrientationData].
     */
    @Test
    fun testOrientationData() {
        val data = OrientationData(10.0, -5.0)
        assertEquals(10.0, data.pitch)
        assertEquals(-5.0, data.roll)
    }

    /**
     * Test logic utilizing the [SensorManager] interface.
     */
    @Test
    fun testSensorManagerInterface() {
        val manager = MockSensorManager()

        assertFalse(manager.isListening)
        manager.startListening()
        assertTrue(manager.isListening)

        manager.stopListening()
        assertFalse(manager.isListening)

        assertEquals(0.0, manager.orientation.value.pitch)
    }
}
