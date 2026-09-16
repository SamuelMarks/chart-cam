/**
 * @file IosSensorManagerTest.kt
 * Contains declarations for IosSensorManagerTest.kt.
 */
package io.healthplatform.chartcam.sensors

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Platform specific sensor initialization test coverage for iOS.
 */
class IosSensorManagerTest {
    /**
     * Verifies that IosSensorManager initializes and manages listeners safely.
     */
    @Test
    fun testSensorLifecycle() {
        val manager = IosSensorManager()
        assertNotNull(manager.orientation, "Orientation flow must be available")
        manager.startListening()
        manager.stopListening()
    }
}
