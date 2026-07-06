/**
 * File storage functionality for the JS platform.
 */
package io.healthplatform.chartcam.files

import kotlinx.browser.localStorage
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

/**
 * Web implementation of [FileStorage].
 * Uses localStorage with Base64 encoding to persist files synchronously.
 */
class JsFileStorage : FileStorage {
    /**
     * Saves an image represented as a byte array into `localStorage`.
     * The bytes are encoded to a Base64 string before storing.
     *
     * @param fileName The name of the file to save.
     * @param bytes The image data as a byte array.
     * @return A virtual file path starting with `mem://path/`.
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
     * Retrieves an image from `localStorage` given its virtual path.
     *
     * @param path The virtual path assigned when the image was saved.
     * @return The decoded image data as a byte array, or an empty array if not found.
     */
    override fun readImage(path: String): ByteArray {
        val base64 = localStorage.getItem(path) ?: return ByteArray(0)
        return base64.decodeBase64()?.toByteArray() ?: ByteArray(0)
    }

    /**
     * Clears all cached images stored by this [JsFileStorage] instance.
     * It scans `localStorage` and removes keys starting with the virtual path prefix.
     */
    override fun clearCache() {
        val keysToRemove = mutableListOf<String>()
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i)
            if (key != null && key.startsWith("mem://path/")) {
                keysToRemove.add(key)
            }
        }
        for (key in keysToRemove) {
            localStorage.removeItem(key)
        }
    }
}

/**
 * Creates and returns an instance of [JsFileStorage].
 *
 * @return A [FileStorage] implementation tailored for the web using `localStorage`.
 */
actual fun createFileStorage(): FileStorage = JsFileStorage()
