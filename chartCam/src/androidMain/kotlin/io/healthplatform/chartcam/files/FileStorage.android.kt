/**
 * File defining the Android-specific implementation of the [FileStorage] interface.
 */
package io.healthplatform.chartcam.files

import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import io.healthplatform.chartcam.AndroidAppInit
import java.io.File

/**
 * Android implementation of [FileStorage].
 * Ensures that patient photos and sensitive files are encrypted at rest
 * using Android Jetpack Security's [EncryptedFile].
 */
class AndroidFileStorage : FileStorage {
    /**
     * Application context fetched from the globally initialized [AndroidAppInit].
     */
    private val context = AndroidAppInit.getContext()

    /**
     * The application's cache directory where temporary encrypted files are stored.
     */
    private val cacheDir = context.cacheDir

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
        val file = File(cacheDir, fileName)
        if (file.exists()) {
            file.delete()
        }

        val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        val encryptedFile =
            EncryptedFile
                .Builder(
                    context,
                    file,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
                ).build()

        encryptedFile.openFileOutput().use { fos ->
            fos.write(bytes)
        }

        return file.absolutePath
    }

    /**
     * Reads and decrypts an encrypted file back into a byte array.
     *
     * @param path The absolute path to the encrypted file.
     * @return The decrypted byte array, or an empty byte array if the file doesn't exist.
     */
    override fun readImage(path: String): ByteArray {
        val file = File(path)
        if (!file.exists()) return ByteArray(0)

        val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        val encryptedFile =
            EncryptedFile
                .Builder(
                    context,
                    file,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
                ).build()

        return encryptedFile.openFileInput().use { fis ->
            fis.readBytes()
        }
    }

    /**
     * Deletes all files currently stored in the cache directory.
     * Provides a safe cache clear mechanism.
     */
    override fun clearCache() {
        // Safe cache clear for demo purposes.
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}

/**
 * Creates and returns an instance of the Android [FileStorage].
 * Note: Requires [AndroidAppInit.init] to have been called beforehand.
 *
 * @return An instance of [FileStorage].
 */
actual fun createFileStorage(): FileStorage = AndroidFileStorage()
