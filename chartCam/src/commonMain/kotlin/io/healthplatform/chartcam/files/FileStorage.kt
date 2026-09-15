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
     * Deletes the given file from storage.
     *
     * @param path The absolute path of the saved file.
     * @return A [Result] indicating success or failure of the deletion.
     */
    fun deleteImage(path: String): Result<Unit>

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

/**
 * Safely saves image bytes to storage, encapsulating any platform I/O exceptions in a [Result].
 *
 * @param fileName The name of the file.
 * @param bytes The data to write.
 * @return A [Result] enclosing the saved file path or failure.
 */
fun FileStorage.saveImageCatching(
    fileName: String,
    bytes: ByteArray,
): Result<String> = runCatching { saveImage(fileName, bytes) }

/**
 * Safely reads image bytes from storage, encapsulating any platform I/O exceptions in a [Result].
 *
 * @param path The absolute or virtual file path.
 * @return A [Result] enclosing the byte array or failure.
 */
fun FileStorage.readImageCatching(path: String): Result<ByteArray> = runCatching { readImage(path) }
