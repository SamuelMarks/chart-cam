/**
 * iOS implementation of the FileStorage interface.
 * Uses okio and native Foundation APIs to manage files in the iOS document directory.
 */
package io.healthplatform.chartcam.files

import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * iOS-specific implementation for managing file storage.
 */
class IosFileStorage : FileStorage {
    /**
     * The file system instance from Okio.
     */
    private val fileSystem = FileSystem.SYSTEM

    /**
     * The path to the user's document directory on iOS.
     */
    private val documentDir by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val docDir = paths.first() as String
        docDir.toPath()
    }

    /**
     * Saves an image represented as a byte array to the document directory.
     *
     * @param fileName The name of the file to save the image as.
     * @param bytes The image data as a byte array.
     * @return The absolute path to the saved image file as a String.
     */
    override fun saveImage(
        fileName: String,
        bytes: ByteArray,
    ): String {
        val path = documentDir / fileName
        fileSystem.write(path) {
            write(bytes)
        }
        return path.toString()
    }

    /**
     * Reads an image file from the specified path and returns its contents as a byte array.
     *
     * @param path The absolute path to the image file to read.
     * @return The contents of the image file as a byte array.
     */
    override fun readImage(path: String): ByteArray =
        fileSystem.read(path.toPath()) {
            readByteArray()
        }

    /**
     * Clears cached files or specific directory contents.
     * Currently not implemented for iOS.
     */
    override fun clearCache() {
        // Implementation would clear logic specific files
    }
}

/**
 * Creates and returns the iOS-specific instance of [FileStorage].
 *
 * @return An instance of [IosFileStorage].
 */
actual fun createFileStorage(): FileStorage = IosFileStorage()
