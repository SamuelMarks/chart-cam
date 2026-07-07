package io.healthplatform.chartcam.ui

import androidx.compose.ui.platform.Clipboard

expect suspend fun Clipboard.getPlainText(): String?

expect suspend fun Clipboard.setPlainText(text: String)
