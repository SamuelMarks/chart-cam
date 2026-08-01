/**
 * @file ClipboardUtils.wasmJs.kt
 * @file ClipboardUtils.wasmJs.kt
 * Contains declarations for ClipboardUtils.wasmJs.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.platform.Clipboard

/**
 * Retrieves plain text from the WasmJS system clipboard asynchronously.
 * Currently not implemented for WasmJS.
 *
 * @receiver The [Clipboard] instance.
 * @return Always returns null in the WasmJS implementation.
 */
actual suspend fun Clipboard.getPlainText(): String? = null

/**
 * Sets plain text content into the WasmJS system clipboard asynchronously.
 * Currently not implemented for WasmJS.
 *
 * @receiver The [Clipboard] instance.
 * @param text The plain text string to be copied.
 */
actual suspend fun Clipboard.setPlainText(text: String) { /* no-op */ }
