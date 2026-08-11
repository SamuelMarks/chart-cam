/**
 * @file SensorManagerJvmTest.kt
 * Contains declarations for SensorManagerJvmTest.kt.
 */
package io.healthplatform.chartcam.sensors

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for SensorManager on JVM.
 */
class SensorManagerJvmTest {
    /**
     * Tests SensorManager on JVM.
     */
    @Test
    fun testSensorManagerJvm() =
        runTest {
            val manager = JvmSensorManager()
            manager.startListening()

            val orientation = manager.orientation.first()
            assertEquals(0.0, orientation.pitch)
            assertEquals(0.0, orientation.roll)

            manager.stopListening()
        }
}
