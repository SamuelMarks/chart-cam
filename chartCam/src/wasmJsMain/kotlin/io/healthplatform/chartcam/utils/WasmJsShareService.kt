/**
 * @file ShareService.wasmJs.kt
 * Provides the WebAssembly (WasmJs) specific implementation of [ShareService],
 * enabling file saving notifications and text clipboard sharing via browser APIs.
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.healthplatform.chartcam.utils

import kotlinx.browser.window

private const val TRIGGER_DOWNLOAD_JS =
    "(filePath, storedData) => { " +
        "const fileName = filePath.split('/').pop() || 'download'; " +
        "let href = storedData; " +
        "if (!href) { " +
        "  href = 'data:text/plain;charset=utf-8,' + encodeURIComponent(filePath); " +
        "} else if (!href.startsWith('data:')) { " +
        "  href = 'data:application/octet-stream;base64,' + href; " +
        "} " +
        "const a = document.createElement('a'); " +
        "a.href = href; " +
        "a.download = fileName; " +
        "document.body.appendChild(a); " +
        "a.click(); " +
        "document.body.removeChild(a); " +
        "}"

@JsFun(TRIGGER_DOWNLOAD_JS)
private external fun triggerDownloadWasmJs(
    filePath: String,
    storedData: String?,
)

/**
 * WebAssembly implementation of [ShareService].
 */
class WasmJsShareService : ShareService {
    /**
     * Shares a file on the web by triggering an automatic browser download.
     *
     * @param filePath The local path or identifier of the file to share.
     * @return A [Result] indicating success or failure.
     */
    override fun shareFile(filePath: String): Result<Unit> =
        runCatching {
            if (filePath.isBlank()) {
                return Result.failure(ExportFileNotFoundException(filePath))
            }
            val stored = window.localStorage.getItem(filePath)
            triggerDownloadWasmJs(filePath, stored)
        }

    /**
     * Shares the given text by copying it to the user's system clipboard.
     * Displays a browser alert on success or failure.
     *
     * @param text The text content to be copied to the clipboard.
     * @return A [Result] indicating success or failure.
     */
    override fun shareText(text: String): Result<Unit> =
        runCatching {
            window.navigator.clipboard
                .writeText(text)
                .then(
                    onFulfilled = {
                        window.alert("Text copied to clipboard")
                        null
                    },
                    onRejected = {
                        window.alert("Failed to copy text")
                        null
                    },
                )
        }.mapCatching { }
}

/**
 * Creates and returns a new instance of [WasmJsShareService] tailored for the WebAssembly target.
 *
 * @return An implementation of [ShareService] for web environments.
 */
actual fun createShareService(): ShareService = WasmJsShareService()
