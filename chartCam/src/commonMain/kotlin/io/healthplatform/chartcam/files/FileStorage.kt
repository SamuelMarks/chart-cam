/**
 * @file FileStorage.kt
 * Contains declarations for FileStorage.kt.
 */
package io.healthplatform.chartcam.files

/**
 * Interface for platform-agnostic file storage operations.
 * Allows saving byte arrays to a temporary app-specific directory.
 */
interface FileStorage {
    /**
     * Saves the given bytes to a file.
     *
     * @param fileName The name of the file (e.g., "image_01.jpg").
     * @param bytes The data to write.
     * @return The absolute path of the saved file.
     */
    fun saveImage(
        fileName: String,
        bytes: ByteArray,
    ): String

    /**
     * Reads the given file into a byte array.
     *
     * @param path The absolute path of the saved file.
     * @return The bytes read from the file.
     */
    fun readImage(path: String): ByteArray

    /**
     * Deletes all temporary files in the capture cache.
     */
    fun clearCache()
}

/**
 * Factory function to create a [FileStorage] instance.
 *
 * @return A platform-specific implementation of [FileStorage].
 */
expect fun createFileStorage(): FileStorage
