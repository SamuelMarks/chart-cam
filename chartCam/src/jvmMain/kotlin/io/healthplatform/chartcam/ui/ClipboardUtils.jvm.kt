/**
 * @file ClipboardUtils.jvm.kt
 * @file ClipboardUtils.jvm.kt
 * Contains declarations for ClipboardUtils.jvm.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.platform.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/**
 * Retrieves plain text from the JVM system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @return The plain text string currently stored in the JVM clipboard, or null if empty or not plain text.
 */
actual suspend fun Clipboard.getPlainText(): String? {
    val clipboard: java.awt.datatransfer.Clipboard = this.nativeClipboard as java.awt.datatransfer.Clipboard
    return try {
        val transferable = clipboard.getContents(null)
        if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            transferable.getTransferData(DataFlavor.stringFlavor) as? String
        } else {
            null
        }
    } catch (
        e: java.awt.datatransfer.UnsupportedFlavorException,
    ) {
        println(e.message)
        null
    } catch (e: java.io.IOException) {
        println(e.message)
        null
    } catch (e: IllegalStateException) {
        println(e.message)
        null
    }
}

/**
 * Sets plain text content into the JVM system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @param text The plain text string to be copied to the JVM clipboard.
 */
actual suspend fun Clipboard.setPlainText(text: String) {
    val clipboard: java.awt.datatransfer.Clipboard = this.nativeClipboard as java.awt.datatransfer.Clipboard
    clipboard.setContents(StringSelection(text), null)
}
