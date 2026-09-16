/**
 * @file FileStorage.ios.kt
 * Contains declarations for FileStorage.ios.kt.
 *
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
     * Deletes the specified file from storage.
     *
     * @param path The absolute path of the file to delete.
     * @return A [Result] indicating success or failure of the deletion.
     */
    override fun deleteImage(path: String): Result<Unit> {
        val okPath = path.toPath()
        return if (fileSystem.exists(okPath)) {
            fileSystem.delete(okPath)
            Result.success(Unit)
        } else {
            Result.failure(Exception("File not found: $path"))
        }
    }

    /**
     * Deletes temporary files ending with .tmp or starting with temp_ in the given directory.
     *
     * @param directory The Okio path of the directory to scan.
     */
    private fun deleteTemporaryFiles(directory: okio.Path) {
        if (!fileSystem.exists(directory)) return
        val list = fileSystem.list(directory)
        for (file in list) {
            val isTemp = file.name.endsWith(".tmp") || file.name.startsWith("temp_")
            if (isTemp) {
                runCatching { fileSystem.delete(file) }
            }
        }
    }

    /**
     * Clears cached temporary files in the documents and caches directories.
     */
    override fun clearCache() {
        deleteTemporaryFiles(documentDir)
        val cachePaths =
            NSSearchPathForDirectoriesInDomains(
                platform.Foundation.NSCachesDirectory,
                NSUserDomainMask,
                true,
            )
        val cacheDirStr = cachePaths.firstOrNull() as? String ?: return
        deleteTemporaryFiles(cacheDirStr.toPath())
    }
}

/**
 * Creates and returns the iOS-specific instance of [FileStorage].
 *
 * @return An instance of [IosFileStorage].
 */
actual fun createFileStorage(): FileStorage = IosFileStorage()
