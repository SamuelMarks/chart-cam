/**
 * @file ShareService.wasmJs.kt
 * @file ShareService.wasmJs.kt
 *
 * Provides the WebAssembly (WasmJs) specific implementation of [ShareService],
 * enabling file saving notifications and text clipboard sharing via browser APIs.
 */
package io.healthplatform.chartcam.utils

import kotlinx.browser.window

/**
 * WebAssembly (WasmJs) implementation for sharing files and text.
 * Utilizes standard browser APIs like `window.alert` and `navigator.clipboard`.
 */
class WasmJsShareService : ShareService {
    /**
     * Simulates sharing a file on the web by displaying an alert containing the file path.
     *
     * @param filePath The local path or identifier of the file to share.
     */
    override fun shareFile(filePath: String) {
        window.alert("File saved. Path: $filePath")
    }

    /**
     * Shares the given text by copying it to the user's system clipboard.
     * Displays a browser alert on success or failure.
     *
     * @param text The text content to be copied to the clipboard.
     */
    @OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
    override fun shareText(text: String) {
        // fallback to clipboard
        window.navigator.clipboard
            .writeText(text)
            .then {
                window.alert("Text copied to clipboard")
                null
            }.catch {
                window.alert("Failed to copy text")
                null
            }
    }
}

/**
 * Creates and returns a new instance of [WasmJsShareService] tailored for the WebAssembly target.
 *
 * @return An implementation of [ShareService] for web environments.
 */
actual fun createShareService(): ShareService = WasmJsShareService()
