/**
 * @file ClipboardUtils.jvm.kt
 * Contains declarations for ClipboardUtils.jvm.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.awtClipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/**
 * Retrieves plain text from the JVM system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @return The plain text string currently stored in the JVM clipboard, or null if empty or not plain text.
 */
@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.getPlainText(): String? {
    val clipboard = this.awtClipboard ?: return null
    return runCatching {
        val transferable = clipboard.getContents(null)
        if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            transferable.getTransferData(DataFlavor.stringFlavor) as? String
        } else {
            null
        }
    }.onFailure { println(it.message) }.getOrNull()
}

/**
 * Sets plain text content into the JVM system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @param text The plain text string to be copied to the JVM clipboard.
 */
@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.setPlainText(text: String) {
    this.awtClipboard?.setContents(StringSelection(text), null)
}
