/**
 * Provides tests for the Wasm-JavaScript specific implementation of the SensorManager.
 */
package io.healthplatform.chartcam.sensors

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for [WasmJsSensorManager].
 *
 * Verifies that the Wasm-JS implementation provides appropriate stubbed functionality
 * since actual hardware sensors might not be available in standard browser Wasm contexts.
 */
class SensorManagerWasmJsTest {
    /**
     * Tests the basic functionality of the [WasmJsSensorManager] stub.
     *
     * Ensures that starting and stopping the listener does not throw exceptions,
     * and that the sensor flow provides a fixed orientation with 0 pitch and roll.
     */
    @Test
    fun testWasmJsSensorManagerStub() =
        runTest {
            val manager = WasmJsSensorManager()

            // Test start/stop don't throw
            manager.startListening()
            manager.stopListening()

            // Test fixed orientation
            val orientation = manager.orientation.first()
            assertEquals(0.0, orientation.pitch)
            assertEquals(0.0, orientation.roll)
        }
}
