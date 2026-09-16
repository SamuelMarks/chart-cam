/**
 * @file ClipboardUtils.js.kt
 * Contains declarations for ClipboardUtils.js.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.platform.Clipboard
import io.healthplatform.chartcam.utils.runSuspendCatching
import kotlinx.browser.window
import kotlinx.coroutines.await

/**
 * Retrieves plain text from the JS system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @return The plain text string currently stored in the clipboard, or null if empty or failed.
 */
actual suspend fun Clipboard.getPlainText(): String? =
    runSuspendCatching {
        window.navigator.clipboard
            .readText()
            .await()
            .takeIf { it.isNotEmpty() }
    }.getOrNull()

/**
 * Sets plain text content into the JS system clipboard asynchronously.
 *
 * @receiver The [Clipboard] instance.
 * @param text The plain text string to be copied.
 */
actual suspend fun Clipboard.setPlainText(text: String) {
    runSuspendCatching {
        window.navigator.clipboard
            .writeText(text)
            .await()
    }
}
