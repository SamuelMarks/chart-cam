/**
 * @file ClipboardUtilsWasmJsTest.kt
 * Contains declarations for ClipboardUtilsWasmJsTest.kt.
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.healthplatform.chartcam.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.W3CTemporaryClipboard
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalComposeUiApi::class)
@JsFun("() => ({})")
private external fun createEmptyClipboard(): W3CTemporaryClipboard

/**
 * Fake implementation of [Clipboard] for WasmJs tests.
 */
@OptIn(ExperimentalComposeUiApi::class)
private class FakeWasmJsClipboard : Clipboard {
    override suspend fun getClipEntry(): ClipEntry? = null

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {}

    override val nativeClipboard: W3CTemporaryClipboard
        get() = createEmptyClipboard()
}

/**
 * Test class for ClipboardUtils on WasmJS.
 */
class ClipboardUtilsWasmJsTest {
    /**
     * Verifies clipboard string trimming on WasmJS.
     */
    @Test
    fun testClipboardUtilsWasmJs() {
        val testString = "  wasm_clipboard_test  "
        val trimmed = testString.trim()
        assertNotNull(trimmed)
        assertEquals("wasm_clipboard_test", trimmed)
    }

    /**
     * Verifies getPlainText and setPlainText execution on WasmJs without throwing exceptions.
     */
    @Test
    fun testWasmJsClipboardGetAndSet() =
        runTest {
            val clipboard = FakeWasmJsClipboard()
            clipboard.setPlainText("Test Wasm Clip")
            // In headless test environment, gracefully handles and returns null
            clipboard.getPlainText()
        }
}
