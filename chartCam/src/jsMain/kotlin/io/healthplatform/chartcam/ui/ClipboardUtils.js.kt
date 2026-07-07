package io.healthplatform.chartcam.ui

import androidx.compose.ui.platform.Clipboard

actual suspend fun Clipboard.getPlainText(): String? = null

actual suspend fun Clipboard.setPlainText(text: String) {}
