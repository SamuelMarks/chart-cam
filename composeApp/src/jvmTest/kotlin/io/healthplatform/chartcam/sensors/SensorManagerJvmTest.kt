package io.healthplatform.chartcam.sensors

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class SensorManagerJvmTest {

    @Test
    fun testJvmSensorManagerStub() = runBlocking {
        val manager = JvmSensorManager()

        // Test start/stop don't throw
        manager.startListening()
        manager.stopListening()

        // Test fixed orientation
        val orientation = manager.orientation.first()
        assertEquals(0.0, orientation.pitch, 0.0)
        assertEquals(0.0, orientation.roll, 0.0)
    }
}
