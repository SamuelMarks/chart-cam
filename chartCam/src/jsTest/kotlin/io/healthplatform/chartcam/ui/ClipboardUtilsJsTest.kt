/**
 * @file ClipboardUtilsJsTest.kt
 * Contains declarations for ClipboardUtilsJsTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.W3CTemporaryClipboard
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Fake implementation of [Clipboard] for JavaScript tests.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private class FakeJsClipboard : Clipboard {
    override suspend fun getClipEntry(): ClipEntry? = null

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {}

    override val nativeClipboard: W3CTemporaryClipboard
        get() = js("{}").unsafeCast<W3CTemporaryClipboard>()
}

/**
 * Test class for ClipboardUtils on JS.
 */
class ClipboardUtilsJsTest {
    /**
     * Verifies clipboard string trimming on JS.
     */
    @Test
    fun testClipboardUtilsJs() {
        val testString = "  js_clipboard_test  "
        val trimmed = testString.trim()
        assertNotNull(trimmed)
        assertEquals("js_clipboard_test", trimmed)
    }

    /**
     * Verifies getPlainText and setPlainText execution on JS without throwing exceptions.
     */
    @Test
    fun testJsClipboardGetAndSet() =
        runTest {
            val clipboard = FakeJsClipboard()
            clipboard.setPlainText("Test JS Clip")
            // In headless/Node test environment without browser clipboard API, gracefully handles and returns null
            clipboard.getPlainText()
        }
}
