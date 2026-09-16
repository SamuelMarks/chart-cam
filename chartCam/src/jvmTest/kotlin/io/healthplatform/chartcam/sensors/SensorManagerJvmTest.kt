/**
 * @file SensorManagerJvmTest.kt
 * Contains declarations for SensorManagerJvmTest.kt.
 */
package io.healthplatform.chartcam.sensors

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
            assertFalse(manager.hasOrientationHardware)
            assertFalse(manager.isAvailable)
            manager.startListening()

            val orientation = manager.orientation.first()
            assertEquals(0.0, orientation.pitch)
            assertEquals(0.0, orientation.roll)

            manager.stopListening()

            val unavailable = UnavailableSensorManager()
            assertFalse(unavailable.hasOrientationHardware)
            assertFalse(unavailable.isAvailable)
            unavailable.startListening()
            unavailable.stopListening()
        }

    /**
     * Tests default implementation of isAvailable and hasOrientationHardware.
     */
    @Test
    fun testSensorManagerDefaultImpls() {
        val dummyManager =
            object : SensorManager {
                override val orientation = emptyFlow<OrientationData>()

                override fun startListening() {}

                override fun stopListening() {}
            }
        assertTrue(dummyManager.isAvailable)
        assertTrue(dummyManager.hasOrientationHardware)

        val defaultImplsClass = Class.forName("io.healthplatform.chartcam.sensors.SensorManager\$DefaultImpls")
        val isAvailMethod = defaultImplsClass.getMethod("isAvailable", SensorManager::class.java)
        val hasHwMethod = defaultImplsClass.getMethod("getHasOrientationHardware", SensorManager::class.java)

        val isAvailResult = isAvailMethod.invoke(null, dummyManager) as Boolean
        val hasHwResult = hasHwMethod.invoke(null, dummyManager) as Boolean
        assertTrue(isAvailResult)
        assertTrue(hasHwResult)
    }
}
