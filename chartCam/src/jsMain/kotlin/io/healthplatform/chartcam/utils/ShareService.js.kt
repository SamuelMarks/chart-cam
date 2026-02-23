package io.healthplatform.chartcam.utils

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement

/**
 * JS implementation for sharing files and text.
 */
class JsShareService : ShareService {
    override fun shareFile(filePath: String) {
        // Not a standard JS feature without File API / share API for local paths.
        // Alerting for simplicity or we can trigger download.
        window.alert("File saved. Download mechanism needed for web. Path: $filePath")
    }

    override fun shareText(text: String) {
        // fallback to clipboard
        window.navigator.clipboard.writeText(text).then {
            window.alert("Text copied to clipboard")
        }.catch {
            window.alert("Failed to copy text")
        }
    }
}

/**
 * Creates the JsShareService.
 */
actual fun createShareService(): ShareService = JsShareService()
