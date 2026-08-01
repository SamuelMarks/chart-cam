/**
 * @file ClipboardUtils.js.kt
 * @file ClipboardUtils.js.kt
 * Contains declarations for ClipboardUtils.js.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.platform.Clipboard

/**
 * Retrieves plain text from the JS system clipboard asynchronously.
 * Currently not implemented for JS.
 *
 * @receiver The [Clipboard] instance.
 * @return Always returns null in the JS implementation.
 */
actual suspend fun Clipboard.getPlainText(): String? = null

/**
 * Sets plain text content into the JS system clipboard asynchronously.
 * Currently not implemented for JS.
 *
 * @receiver The [Clipboard] instance.
 * @param text The plain text string to be copied.
 */
actual suspend fun Clipboard.setPlainText(text: String) { /* no-op */ }
