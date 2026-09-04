/**
 * @file AndroidFileStorage.kt
 * Contains declarations for AndroidFileStorage.kt.
 *
 * File defining the Android-specific implementation of the [FileStorage] interface.
 */
package io.healthplatform.chartcam.files

import io.healthplatform.chartcam.AndroidAppInit
import io.healthplatform.chartcam.storage.CryptoHelper
import java.io.File

/**
 * Android implementation of [FileStorage].
 * Ensures that patient photos and sensitive files are encrypted at rest
 * using Android Jetpack Security's equivalents.
 */
class AndroidFileStorage : FileStorage {
    /**
     * Application context fetched from the globally initialized [AndroidAppInit].
     */
    private val context = AndroidAppInit.getContext()

    /**
     * The application's internal files directory where persistent encrypted files are stored.
     */
    private val filesDir = context.filesDir

    /**
     * Saves raw byte data as an encrypted file.
     *
     * @param fileName The desired name for the saved file.
     * @param bytes The raw byte array data to encrypt and save.
     * @return The absolute path to the encrypted file.
     */
    override fun saveImage(
        fileName: String,
        bytes: ByteArray,
    ): String {
        val file = File(filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }

        val encryptedBytes = CryptoHelper.encrypt(bytes)
        file.writeBytes(encryptedBytes)

        return file.absolutePath
    }

    /**
     * Reads and decrypts an encrypted file back into a byte array.
     * Supports resolving paths pointing to cache directory or persistent files directory,
     * maintaining compatibility with files migrated between directories.
     *
     * @param path The absolute path to the encrypted file.
     * @return The decrypted byte array, or an empty byte array if the file doesn't exist.
     */
    override fun readImage(path: String): ByteArray {
        val file = resolveImageFile(path) ?: return ByteArray(0)

        val encryptedBytes = file.readBytes()
        return try {
            CryptoHelper.decrypt(encryptedBytes)
        } catch (e: java.security.GeneralSecurityException) {
            println("Failed to decrypt file: ${e.message}")
            ByteArray(0)
        } catch (e: IllegalArgumentException) {
            println("Invalid encrypted file data: ${e.message}")
            ByteArray(0)
        }
    }

    /**
     * Resolves the target file from the given path, falling back to persistent files directory
     * or cache directory if necessary.
     *
     * @param path The initial file path to resolve.
     * @return The resolved [File], or null if not found.
     */
    private fun resolveImageFile(path: String): File? {
        val file = File(path)
        if (file.exists()) return file

        val fileName = file.name
        val fallbackFilesDir = File(filesDir, fileName)
        val fallbackCacheDir = File(context.cacheDir, fileName)
        return when {
            fallbackFilesDir.exists() -> fallbackFilesDir
            fallbackCacheDir.exists() -> tryPromoteCacheFile(fallbackCacheDir, fileName)
            else -> null
        }
    }

    /**
     * Attempts to promote a cached image file to persistent files directory.
     *
     * @param cacheFile The source cache file.
     * @param fileName The name of the file to promote.
     * @return The promoted [File] in files directory or the original cache [File].
     */
    private fun tryPromoteCacheFile(
        cacheFile: File,
        fileName: String,
    ): File =
        try {
            val destFile = File(filesDir, fileName)
            if (!destFile.exists()) {
                cacheFile.copyTo(destFile, overwrite = true)
            }
            cacheFile.delete()
            destFile
        } catch (e: java.io.IOException) {
            println("Failed to promote cache file $fileName: ${e.message}")
            cacheFile
        }

    /**
     * Deletes all files currently stored in the internal files directory and cache directory.
     */
    override fun clearCache() {
        // Safe clear for demo purposes.
        filesDir.listFiles()?.forEach { it.delete() }
        context.cacheDir.listFiles()?.forEach { if (it.isFile) it.delete() }
    }
}

/**
 * Creates and returns an instance of the Android [FileStorage].
 * Note: Requires [AndroidAppInit.init] to have been called beforehand.
 *
 * @return An instance of [FileStorage].
 */
actual fun createFileStorage(): FileStorage = AndroidFileStorage()
