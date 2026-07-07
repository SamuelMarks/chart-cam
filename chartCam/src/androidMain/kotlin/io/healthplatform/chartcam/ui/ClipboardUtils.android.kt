package io.healthplatform.chartcam.ui

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

actual suspend fun Clipboard.getPlainText(): String? {
    val clipEntry = this.getClipEntry()
    return clipEntry
        ?.clipData
        ?.getItemAt(0)
        ?.text
        ?.toString()
}

actual suspend fun Clipboard.setPlainText(text: String) {
    val clipData = ClipData.newPlainText("label", text)
    this.setClipEntry(ClipEntry(clipData))
}
