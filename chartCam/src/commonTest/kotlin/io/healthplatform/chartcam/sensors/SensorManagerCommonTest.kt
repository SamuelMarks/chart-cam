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

class SensorManagerCommonTest {
    class MockSensorManager : SensorManager {
        override val orientation = MutableStateFlow(OrientationData(0.0, 0.0))
        var isListening = false

        override fun startListening() {
            isListening = true
        }

        override fun stopListening() {
            isListening = false
        }
    }

    @Test
    fun testOrientationData() {
        val data = OrientationData(10.0, -5.0)
        assertEquals(10.0, data.pitch)
        assertEquals(-5.0, data.roll)
    }

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
