package io.healthplatform.chartcam.viewmodel

import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.storage.SecureStorage

class FakeSecureStorage : SecureStorage {
    private val store = mutableMapOf<String, String>()

    override fun save(
        key: String,
        value: String,
    ) {
        store[key] = value
    }

    override fun getString(key: String): String? = store[key]

    override fun delete(key: String) {
        store.remove(key)
    }
}

class FakeFileStorage : FileStorage {
    private val files = mutableMapOf<String, ByteArray>()

    override fun saveImage(
        fileName: String,
        bytes: ByteArray,
    ): String {
        files[fileName] = bytes
        return fileName
    }

    override fun readImage(path: String): ByteArray = files[path] ?: ByteArray(0)

    override fun clearCache() {
        files.clear()
    }
}
