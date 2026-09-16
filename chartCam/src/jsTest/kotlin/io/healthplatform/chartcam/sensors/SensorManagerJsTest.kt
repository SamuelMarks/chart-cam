/**
 * @file SensorManagerJsTest.kt
 * Contains declarations for SensorManagerJsTest.kt.
 */
package io.healthplatform.chartcam.sensors

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for SensorManager on JS.
 */
class SensorManagerJsTest {
    /**
     * Test sensor manager initialization on JS.
     */
    @Test
    fun testSensorManagerJs() {
        val manager = JsSensorManager()
        assertNotNull(manager.orientation)
    }
}
