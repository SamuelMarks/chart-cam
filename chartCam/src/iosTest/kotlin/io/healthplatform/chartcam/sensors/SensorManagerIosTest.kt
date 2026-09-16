/**
 * @file SensorManagerIosTest.kt
 * Contains declarations for SensorManagerIosTest.kt.
 */
package io.healthplatform.chartcam.sensors

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Functional tests for SensorManager on iOS.
 */
class SensorManagerIosTest {
    /**
     * Verifies multiple start/stop cycles do not fail.
     */
    @Test
    fun testRepeatedSensorListening() {
        val manager = IosSensorManager()
        manager.startListening()
        manager.startListening()
        manager.stopListening()
        manager.stopListening()
        assertNotNull(manager)
    }
}
