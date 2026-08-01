/**
 * @file FileStorage.jvm.kt
 * File storage implementation for the JVM platform using Okio.
 */
package io.healthplatform.chartcam.files

import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * JVM-specific implementation of [FileStorage].
 * Utilizes the Okio library for efficient and idiomatic file I/O operations
 * against the system's temporary directory.
 */
class JvmFileStorage : FileStorage {
    /**
     * The Okio [FileSystem] instance representing the host operating system's local file system.
     */
    private val fileSystem = FileSystem.SYSTEM

    /**
     * The resolved path to the system's default temporary directory.
     */
    private val tempDir = System.getProperty("java.io.tmpdir").toPath()

    /**
     * Saves a byte array as an image file in the system's temporary directory.
     *
     * @param fileName The desired name for the file (e.g., "image.png").
     * @param bytes The binary contents of the image to be saved.
     * @return The absolute path to the newly saved file as a string.
     */
    override fun saveImage(
        fileName: String,
        bytes: ByteArray,
    ): String {
        val path = tempDir / fileName
        fileSystem.write(path) {
            write(bytes)
        }
        return path.toString()
    }

    /**
     * Reads the binary contents of an image file from the specified path.
     *
     * @param path The absolute path to the file to be read.
     * @return A [ByteArray] containing the raw bytes of the file.
     */
    override fun readImage(path: String): ByteArray {
        val okPath = path.toPath()
        if (!fileSystem.exists(okPath)) return ByteArray(0)

        return fileSystem.read(okPath) {
            readByteArray()
        }
    }

    /**
     * Clears the file cache. This is currently a no-op on the JVM platform.
     */
    override fun clearCache() { /* no-op */ }
}

/**
 * Creates and returns a new instance of [FileStorage] for the JVM platform.
 *
 * @return A new [JvmFileStorage] instance.
 */
actual fun createFileStorage(): FileStorage = JvmFileStorage()
