package io.healthplatform.chartcam.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.getPlainText(): String? {
    val clipEntry = this.getClipEntry()
    return clipEntry?.getPlainText()
}

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.setPlainText(text: String) {
    this.setClipEntry(ClipEntry.withPlainText(text))
}
