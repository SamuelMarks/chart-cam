package io.healthplatform.chartcam.files

import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import io.healthplatform.chartcam.AndroidAppInit
import java.io.File

/**
 * Android implementation of FileStorage.
 * Ensures that patient photos are encrypted at rest using Jetpack Security's [EncryptedFile].
 */
class AndroidFileStorage : FileStorage {
    private val context = AndroidAppInit.getContext()
    private val cacheDir = context.cacheDir

    override fun saveImage(fileName: String, bytes: ByteArray): String {
        val file = File(cacheDir, fileName)
        if (file.exists()) {
            file.delete()
        }

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { fos ->
            fos.write(bytes)
        }

        return file.absolutePath
    }

    override fun readImage(path: String): ByteArray {
        val file = File(path)
        if (!file.exists()) return ByteArray(0)

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return encryptedFile.openFileInput().use { fis ->
            fis.readBytes()
        }
    }

    override fun clearCache() {
        // Safe cache clear for demo purposes.
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}

actual fun createFileStorage(): FileStorage = AndroidFileStorage()
