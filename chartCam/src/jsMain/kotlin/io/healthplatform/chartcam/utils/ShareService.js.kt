/**
 * Sharing capabilities for the JS platform.
 */
package io.healthplatform.chartcam.utils

import kotlinx.browser.window

/**
 * JS implementation for sharing files and text.
 */
class JsShareService : ShareService {
    /**
     * Simulates sharing a file on the web platform.
     * Current implementation alerts the user since local file sharing is limited in JS.
     *
     * @param filePath The path of the file to share.
     */
    override fun shareFile(filePath: String) {
        // Not a standard JS feature without File API / share API for local paths.
        // Alerting for simplicity or we can trigger download.
        window.alert("File saved. Download mechanism needed for web. Path: $filePath")
    }

    /**
     * Shares text content by copying it to the user's system clipboard.
     * Alerts the user upon success or failure.
     *
     * @param text The text content to share or copy.
     */
    override fun shareText(text: String) {
        // fallback to clipboard
        window.navigator.clipboard
            .writeText(text)
            .then {
                window.alert("Text copied to clipboard")
            }.catch {
                window.alert("Failed to copy text")
            }
    }
}

/**
 * Creates and returns an instance of [JsShareService].
 *
 * @return A [ShareService] capable of web-based sharing.
 */
actual fun createShareService(): ShareService = JsShareService()
