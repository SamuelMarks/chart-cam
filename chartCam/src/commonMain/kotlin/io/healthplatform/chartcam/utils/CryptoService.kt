package io.healthplatform.chartcam.utils

import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString

/**
 * Service to handle encryption and decryption.
 */
class CryptoService {

    /**
     * Encrypts the [data] string using the given [password].
     * @param data The plaintext data to encrypt.
     * @param password The password to use as a key.
     * @return The base64-encoded encrypted string.
     */
    fun encrypt(data: String, password: String): String {
        val dataBytes = data.encodeToByteArray()
        val encrypted = rc4(password, dataBytes)
        return encrypted.toByteString().base64()
    }

    /**
     * Decrypts the [base64Data] string using the given [password].
     * @param base64Data The base64-encoded encrypted data.
     * @param password The password to use as a key.
     * @return The decrypted plaintext string.
     */
    fun decrypt(base64Data: String, password: String): String {
        val dataBytes = base64Data.decodeBase64()?.toByteArray() ?: return ""
        val decrypted = rc4(password, dataBytes)
        return decrypted.decodeToString()
    }

    /**
     * A simple stream cipher algorithm (RC4-like).
     * @param key The string key.
     * @param data The data to encrypt/decrypt.
     * @return The transformed bytes.
     */
    private fun rc4(key: String, data: ByteArray): ByteArray {
        if (key.isEmpty()) return data
        val keyBytes = key.encodeToByteArray()
        val S = IntArray(256) { it }
        var j = 0
        for (i in 0 until 256) {
            val unsignedKeyByte = keyBytes[i % keyBytes.size].toInt() and 0xFF
            j = (j + S[i] + unsignedKeyByte) % 256
            val temp = S[i]
            S[i] = S[j]
            S[j] = temp
        }

        val result = ByteArray(data.size)
        var i = 0
        j = 0
        for (k in data.indices) {
            i = (i + 1) % 256
            j = (j + S[i]) % 256
            val temp = S[i]
            S[i] = S[j]
            S[j] = temp
            val K = S[(S[i] + S[j]) % 256]
            result[k] = (data[k].toInt() xor K).toByte()
        }
        return result
    }
}
