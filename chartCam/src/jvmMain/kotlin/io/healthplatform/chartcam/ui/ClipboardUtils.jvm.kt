package io.healthplatform.chartcam.ui

import androidx.compose.ui.platform.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

actual suspend fun Clipboard.getPlainText(): String? {
    val clipboard: java.awt.datatransfer.Clipboard = this.nativeClipboard as java.awt.datatransfer.Clipboard
    return try {
        val transferable = clipboard.getContents(null)
        if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            transferable.getTransferData(DataFlavor.stringFlavor) as? String
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

actual suspend fun Clipboard.setPlainText(text: String) {
    val clipboard: java.awt.datatransfer.Clipboard = this.nativeClipboard as java.awt.datatransfer.Clipboard
    clipboard.setContents(StringSelection(text), null)
}
