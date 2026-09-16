/**
 * @file ClipboardUtils.wasmJs.kt
 * Contains declarations for ClipboardUtils.wasmJs.kt.
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.healthplatform.chartcam.ui

import androidx.compose.ui.platform.Clipboard
import io.healthplatform.chartcam.utils.runSuspendCatching
import kotlinx.coroutines.await
import kotlin.js.Promise

private const val READ_CLIPBOARD_JS =
    "() => (typeof window !== 'undefined' && window.navigator && window.navigator.clipboard ? " +
        "window.navigator.clipboard.readText() : Promise.resolve(''))"

private const val WRITE_CLIPBOARD_JS =
    "(text) => (typeof window !== 'undefined' && window.navigator && window.navigator.clipboard ? " +
        "window.navigator.clipboard.writeText(text) : Promise.resolve())"

@JsFun(READ_CLIPBOARD_JS)
private external fun readClipboardWasmJs(): Promise<JsString>

@JsFun(WRITE_CLIPBOARD_JS)
private external fun writeClipboardWasmJs(text: String): Promise<JsAny?>

/**
 * Retrieves plain text from the WasmJS system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @return The plain text string currently stored in the clipboard, or null if empty or failed.
 */
actual suspend fun Clipboard.getPlainText(): String? =
    runSuspendCatching {
        val jsStr = readClipboardWasmJs().await()
        val text = jsStr.toString()
        text.ifEmpty { null }
    }.getOrNull()

/**
 * Sets plain text content into the WasmJS system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @param text The plain text string to be copied.
 */
actual suspend fun Clipboard.setPlainText(text: String) {
    runSuspendCatching {
        writeClipboardWasmJs(text).await()
    }
}
