/**
 * @file ClipboardUtils.ios.kt
 * Contains declarations for ClipboardUtils.ios.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

/**
 * Retrieves plain text from the iOS system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @return The plain text string currently stored in the iOS clipboard, or null if empty or not plain text.
 */
@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.getPlainText(): String? {
    val clipEntry = this.getClipEntry()
    return clipEntry?.getPlainText()
}

/**
 * Sets plain text content into the iOS system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @param text The plain text string to be copied to the iOS clipboard.
 */
@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.setPlainText(text: String) {
    this.setClipEntry(ClipEntry.withPlainText(text))
}
