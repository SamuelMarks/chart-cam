/**
 * @file ShareService.js.kt
 * Sharing capabilities for the JS platform.
 */
package io.healthplatform.chartcam.utils

import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement

/**
 * JS implementation for sharing files and text.
 */
class JsShareService : ShareService {
    /**
     * Shares a file on the web by triggering an automatic browser download.
     *
     * @param filePath The path of the file to share.
     * @return A [Result] indicating success or failure.
     */
    override fun shareFile(filePath: String): Result<Unit> =
        runCatching {
            if (filePath.isBlank()) {
                return Result.failure(ExportFileNotFoundException(filePath))
            }
            val fileName = filePath.substringAfterLast('/').ifEmpty { "download" }
            val storedData = window.localStorage.getItem(filePath)
            val href =
                when {
                    storedData == null -> "data:text/plain;charset=utf-8,$filePath"
                    storedData.startsWith("data:") -> storedData
                    else -> "data:application/octet-stream;base64,$storedData"
                }
            val doc = window.document
            val anchor = doc.createElement("a") as HTMLAnchorElement
            anchor.href = href
            anchor.download = fileName
            doc.body?.appendChild(anchor)
            anchor.click()
            doc.body?.removeChild(anchor)
        }.mapCatching { }

    /**
     * Shares text content by copying it to the user's system clipboard.
     * Alerts the user upon success or failure.
     *
     * @param text The text content to share or copy.
     * @return A [Result] indicating success or failure.
     */
    override fun shareText(text: String): Result<Unit> =
        runCatching {
            window.navigator.clipboard
                .writeText(text)
                .then(
                    onFulfilled = {
                        window.alert("Text copied to clipboard")
                    },
                    onRejected = {
                        window.alert("Failed to copy text")
                    },
                )
        }.mapCatching { }
}

/**
 * Creates and returns an instance of [JsShareService].
 *
 * @return A [ShareService] capable of web-based sharing.
 */
actual fun createShareService(): ShareService = JsShareService()
