/**
 * Testing namespace for JVM-specific sensor implementations.
 *
 * Verifies the correctness of the stubbed sensor manager operations on the JVM,
 * ensuring they behave safely and return predictable mocked data without crashing.
 */
package io.healthplatform.chartcam.sensors

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Validates the core functions of [JvmSensorManager].
 *
 * Specifically checks that the start and stop operations execute without incident
 * and that orientation metrics are stable and zeroed.
 */
class SensorManagerJvmTest {
    /**
     * Executes the JVM sensor manager stub test.
     *
     * Verifies that the manager safely handles listening requests
     * and asserts that the simulated orientation yields zero pitch and roll.
     */
    @Test
    fun testJvmSensorManagerStub() =
        runBlocking {
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
