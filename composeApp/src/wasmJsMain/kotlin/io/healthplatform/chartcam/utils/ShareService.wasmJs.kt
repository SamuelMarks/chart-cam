package io.healthplatform.chartcam.utils

import kotlinx.browser.window

/**
 * WasmJs implementation for sharing files and text.
 */
class WasmJsShareService : ShareService {
    override fun shareFile(filePath: String) {
        window.alert("File saved. Path: $filePath")
    }

    @OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
    override fun shareText(text: String) {
        // fallback to clipboard
        window.navigator.clipboard.writeText(text).then {
            window.alert("Text copied to clipboard")
            null
        }.catch {
            window.alert("Failed to copy text")
            null
        }
    }
}

/**
 * Creates the WasmJsShareService.
 */
actual fun createShareService(): ShareService = WasmJsShareService()
