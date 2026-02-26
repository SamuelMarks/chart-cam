package io.healthplatform.chartcam.files

import kotlinx.browser.localStorage
import okio.ByteString.Companion.toByteString
import okio.ByteString.Companion.decodeBase64

/**
 * Web implementation of [FileStorage].
 * Uses localStorage with Base64 encoding to persist files synchronously.
 */
class JsFileStorage : FileStorage {

    override fun saveImage(fileName: String, bytes: ByteArray): String {
        val virtualPath = "mem://path/$fileName"
        val base64 = bytes.toByteString().base64()
        localStorage.setItem(virtualPath, base64)
        return virtualPath
    }

    override fun readImage(path: String): ByteArray {
        val base64 = localStorage.getItem(path) ?: return ByteArray(0)
        return base64.decodeBase64()?.toByteArray() ?: ByteArray(0)
    }

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

actual fun createFileStorage(): FileStorage = JsFileStorage()
