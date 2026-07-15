/**
 * @file CryptoHelper.kt
 * Contains declarations for CryptoHelper.kt.
 */
package io.healthplatform.chartcam.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Helper object providing symmetric encryption and decryption using the Android KeyStore.
 * It manages an AES key stored securely to protect sensitive data on the device.
 */
internal object CryptoHelper {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "ChartCamKeyAlias"

    private var robolectricKey: SecretKey? = null

    /**
     * Retrieves the AES secret key from the Android KeyStore, creating it if it does not exist.
     * Provides a fallback key for Robolectric environments.
     *
     * @return The symmetric AES [SecretKey].
     */
    private fun getSecretKey(): SecretKey {
        if (android.os.Build.FINGERPRINT == "robolectric") {
            if (robolectricKey == null) {
                val keyGenerator = KeyGenerator.getInstance("AES")
                keyGenerator.init(128)
                robolectricKey = keyGenerator.generateKey()
            }
            return robolectricKey!!
        }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        keyStore.getKey(ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec =
            KeyGenParameterSpec
                .Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts the provided byte array using AES/GCM/NoPadding.
     *
     * @param data The plaintext data to encrypt.
     * @return The encrypted byte array, prepended with the IV length and the IV itself.
     */
    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return byteArrayOf(iv.size.toByte()) + iv + encrypted
    }

    /**
     * Decrypts the provided byte array using AES/GCM/NoPadding.
     * It extracts the IV from the beginning of the data before decrypting the rest.
     *
     * @param data The encrypted byte array (including the IV).
     * @return The decrypted plaintext byte array.
     */
    fun decrypt(data: ByteArray): ByteArray {
        val ivSize = data[0].toInt()
        val iv = data.copyOfRange(1, 1 + ivSize)
        val encrypted = data.copyOfRange(1 + ivSize, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }
}
