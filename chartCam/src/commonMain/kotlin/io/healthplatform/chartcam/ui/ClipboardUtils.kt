/**
 * @file ClipboardUtils.kt
 * Contains declarations for ClipboardUtils.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.platform.Clipboard

/**
 * Extension function to retrieve plain text from the clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @return The plain text string currently stored in the clipboard, or null if empty or not plain text.
 */
expect suspend fun Clipboard.getPlainText(): String?

/**
 * Extension function to set plain text content into the clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @param text The plain text string to be copied to the clipboard.
 */
expect suspend fun Clipboard.setPlainText(text: String)
