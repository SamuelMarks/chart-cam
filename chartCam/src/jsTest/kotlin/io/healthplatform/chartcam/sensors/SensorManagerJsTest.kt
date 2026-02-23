package io.healthplatform.chartcam.sensors

import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class SensorManagerJsTest {

    @Test
    fun testJsSensorManagerStub() = runTest {
        val manager = JsSensorManager()

        // Test start/stop don't throw
        manager.startListening()
        manager.stopListening()

        // Test fixed orientation
        val orientation = manager.orientation.first()
        assertEquals(0.0, orientation.pitch)
        assertEquals(0.0, orientation.roll)
    }
}
