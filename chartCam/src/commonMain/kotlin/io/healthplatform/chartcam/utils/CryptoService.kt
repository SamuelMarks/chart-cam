/**
 * Provides a service for cryptographic operations, specifically encryption and decryption.
 */
package io.healthplatform.chartcam.utils

import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

/**
 * Service to handle encryption and decryption of string data.
 * It uses a simple RC4-like stream cipher for demonstration purposes.
 */
class CryptoService {
    /**
     * Encrypts the [data] string using the given [password].
     *
     * @param data The plaintext data to encrypt.
     * @param password The password to use as a key.
     * @return The base64-encoded encrypted string.
     */
    fun encrypt(
        data: String,
        password: String,
    ): String {
        val dataBytes = data.encodeToByteArray()
        val encrypted = rc4(password, dataBytes)
        return encrypted.toByteString().base64()
    }

    /**
     * Decrypts the [base64Data] string using the given [password].
     *
     * @param base64Data The base64-encoded encrypted data.
     * @param password The password to use as a key.
     * @return The decrypted plaintext string, or an empty string if decryption fails.
     */
    fun decrypt(
        base64Data: String,
        password: String,
    ): String {
        val dataBytes = base64Data.decodeBase64()?.toByteArray() ?: return ""
        val decrypted = rc4(password, dataBytes)
        return decrypted.decodeToString()
    }

    /**
     * A simple stream cipher algorithm (RC4-like).
     *
     * @param key The string key used for the cipher.
     * @param data The data to encrypt or decrypt.
     * @return The transformed bytes.
     */
    private fun rc4(
        key: String,
        data: ByteArray,
    ): ByteArray {
        if (key.isEmpty()) return data
        val keyBytes = key.encodeToByteArray()
        val sArray = IntArray(256) { it }
        var j = 0
        for (i in 0 until 256) {
            val unsignedKeyByte = keyBytes[i % keyBytes.size].toInt() and 0xFF
            j = (j + sArray[i] + unsignedKeyByte) % 256
            val temp = sArray[i]
            sArray[i] = sArray[j]
            sArray[j] = temp
        }

        val result = ByteArray(data.size)
        var i = 0
        j = 0
        for (k in data.indices) {
            i = (i + 1) % 256
            j = (j + sArray[i]) % 256
            val temp = sArray[i]
            sArray[i] = sArray[j]
            sArray[j] = temp
            val kVal = sArray[(sArray[i] + sArray[j]) % 256]
            result[k] = (data[k].toInt() xor kVal).toByte()
        }
        return result
    }
}
