/**
 * Provides tests for Wasm-JavaScript camera capture functionalities.
 */
package io.healthplatform.chartcam.camera

import kotlinx.browser.window
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test class for Wasm-JS specific camera capture utilities.
 *
 * Contains tests verifying standard browser APIs like base64 decoding (atob)
 * that are utilized in camera capture processing flows.
 */
class CameraCaptureWasmJsTest {
    /**
     * Tests the `window.atob` function to ensure correct base64 decoding.
     *
     * Validates that a known base64 encoded string is correctly decoded into
     * its raw byte array representation and can be restored to the original string.
     */
    @Test
    fun testAtob() {
        val encoded = "SGVsbG8="
        val decoded = window.atob(encoded)
        val bytes = ByteArray(decoded.length)
        for (i in 0 until decoded.length) {
            bytes[i] = decoded[i].code.toByte()
        }
        val text = bytes.decodeToString()
        assertTrue(text == "Hello", "Expected 'Hello', got '$text'")
    }
}
