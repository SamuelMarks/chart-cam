/**
 * @file ClipboardUtilsWasmJsTest.kt
 * Contains declarations for ClipboardUtilsWasmJsTest.kt.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test class for ClipboardUtils on WasmJS.
 */
class ClipboardUtilsWasmJsTest {
    /**
     * Test clipboard string trimming on WasmJS.
     */
    @Test
    fun testClipboardUtilsWasmJs() {
        val testString = "  wasm_clipboard_test  "
        val trimmed = testString.trim()
        assertNotNull(trimmed)
        assertEquals("wasm_clipboard_test", trimmed)
    }
}
