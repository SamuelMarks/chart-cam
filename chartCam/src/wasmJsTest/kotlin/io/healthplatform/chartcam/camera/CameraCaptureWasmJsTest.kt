package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.browser.window

class CameraCaptureWasmJsTest {
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
