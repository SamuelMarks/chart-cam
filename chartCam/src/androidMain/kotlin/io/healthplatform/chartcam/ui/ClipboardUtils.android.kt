/**
 * @file ClipboardUtils.android.kt
 * Contains declarations for ClipboardUtils.android.kt.
 */
package io.healthplatform.chartcam.ui

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

/**
 * Retrieves plain text from the Android system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @return The plain text string currently stored in the Android clipboard, or null if empty or not plain text.
 */
actual suspend fun Clipboard.getPlainText(): String? {
    val clipEntry = this.getClipEntry()
    return clipEntry
        ?.clipData
        ?.getItemAt(0)
        ?.text
        ?.toString()
}

/**
 * Sets plain text content into the Android system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @param text The plain text string to be copied to the Android clipboard.
 */
actual suspend fun Clipboard.setPlainText(text: String) {
    val label = "ChartCam Clipboard Data"
    val clipData = ClipData.newPlainText(label, text)
    this.setClipEntry(ClipEntry(clipData))
}
