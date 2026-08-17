/**
 * @file TestFakes.kt
 * Contains declarations for TestFakes.kt.
 */
package io.healthplatform.chartcam.viewmodel

import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.storage.SecureStorage

/**
 * Fake implementation of [SecureStorage] for testing.
 */
class FakeSecureStorage : SecureStorage {
    /** The internal map storing data. */
    private val store = mutableMapOf<String, String>()

    /**
     * Save a key-value pair.
     * @param key The key.
     * @param value The value.
     */
    override fun save(
        key: String,
        value: String,
    ) {
        store[key] = value
    }

    /**
     * Retrieve a value by key.
     * @param key The key.
     * @return The value or null.
     */
    override fun getString(key: String): String? = store[key]

    /**
     * Delete a value by key.
     * @param key The key.
     */
    override fun delete(key: String) {
        store.remove(key)
    }
}

/**
 * Fake implementation of [FileStorage] for testing.
 */
class FakeFileStorage : FileStorage {
    /** The internal map storing files as byte arrays. */
    private val files = mutableMapOf<String, ByteArray>()

    /**
     * Save a byte array to a pseudo-filename path.
     * @param fileName The name.
     * @param bytes The bytes.
     * @return The path.
     */
    override fun saveImage(
        fileName: String,
        bytes: ByteArray,
    ): String {
        files[fileName] = bytes
        return fileName
    }

    /**
     * Read a byte array given a pseudo-filename path.
     * @param path The path.
     * @return The bytes.
     */
    override fun readImage(path: String): ByteArray = files[path] ?: ByteArray(0)

    /** Clear the entire file cache. */
    override fun clearCache() {
        files.clear()
    }
}
