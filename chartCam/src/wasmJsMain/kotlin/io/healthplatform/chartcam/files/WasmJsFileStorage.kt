/**
 * @file FileStorage.wasmJs.kt
 * @file FileStorage.wasmJs.kt
 *
 * Provides the WebAssembly (WasmJs) specific implementation of [FileStorage],
 * utilizing the browser's `localStorage` to simulate a file system by storing Base64-encoded strings.
 */
package io.healthplatform.chartcam.files

import kotlinx.browser.localStorage
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

/**
 * WebAssembly (WasmJs) implementation of [FileStorage].
 * Uses `localStorage` with Base64 encoding to persist files synchronously within the browser.
 */
class WasmJsFileStorage : FileStorage {
    /**
     * Saves raw image bytes to `localStorage` as a Base64-encoded string.
     * Generates a virtual file path in the format `mem://path/[fileName]`.
     *
     * @param fileName The desired name for the saved image file.
     * @param bytes The raw byte array containing the image data.
     * @return The virtual path (string identifier) where the image data was stored.
     */
    override fun saveImage(
        fileName: String,
        bytes: ByteArray,
    ): String {
        val virtualPath = "mem://path/$fileName"
        val base64 = bytes.toByteString().base64()
        localStorage.setItem(virtualPath, base64)
        return virtualPath
    }

    /**
     * Reads image data from `localStorage` using its virtual path.
     * Decodes the Base64 string back into a byte array.
     *
     * @param path The virtual path (string identifier) of the stored image data.
     * @return A byte array containing the image data, or an empty array if the path is invalid or missing.
     */
    override fun readImage(path: String): ByteArray {
        val base64 = localStorage.getItem(path) ?: return ByteArray(0)
        return base64.decodeBase64()?.toByteArray() ?: ByteArray(0)
    }

    /**
     * Clears all cached image files stored in `localStorage` by removing any keys
     * that match the `mem://path/` virtual prefix.
     */
    override fun clearCache() {
        val keysToRemove = mutableListOf<String>()
        // Iterate through all localStorage items to find our virtual files
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i)
            if (key != null && key.startsWith("mem://path/")) {
                keysToRemove.add(key)
            }
        }
        // Remove all identified virtual files
        for (key in keysToRemove) {
            localStorage.removeItem(key)
        }
    }
}

/**
 * Factory function to create a [WasmJsFileStorage] instance for the Wasm target.
 *
 * @return A new instance of [WasmJsFileStorage] implementing [FileStorage].
 */
actual fun createFileStorage(): FileStorage = WasmJsFileStorage()
