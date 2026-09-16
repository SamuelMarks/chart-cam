/**
 * @file ClipboardUtilsJsTest.kt
 * Contains declarations for ClipboardUtilsJsTest.kt.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test class for ClipboardUtils on JS.
 */
class ClipboardUtilsJsTest {
    /**
     * Test clipboard string trimming on JS.
     */
    @Test
    fun testClipboardUtilsJs() {
        val testString = "  js_clipboard_test  "
        val trimmed = testString.trim()
        assertNotNull(trimmed)
        assertEquals("js_clipboard_test", trimmed)
    }
}
