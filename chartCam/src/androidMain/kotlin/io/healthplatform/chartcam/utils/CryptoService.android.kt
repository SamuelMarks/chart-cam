/**
 * @file CryptoService.android.kt
 * Contains declarations for CryptoService.android.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Service providing cryptographic operations on the Android platform using BouncyCastle and javax.crypto.
 */
actual class CryptoService actual constructor() {
    /**
     * Derives a cryptographic key using the Argon2 hashing algorithm.
     *
     * @param password The user-provided password string.
     * @param salt The salt byte array.
     * @return The derived cryptographic key.
     */
    actual suspend fun deriveKeyArgon2(
        password: String,
        salt: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.Default) {
            val parameters =
                Argon2Parameters
                    .Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                    .withIterations(3)
                    .withMemoryAsKB(65536)
                    .withParallelism(4)
                    .withSalt(salt)
                    .build()

            val generator = Argon2BytesGenerator()
            generator.init(parameters)

            val key = ByteArray(32) // 256-bit key
            generator.generateBytes(password.encodeToByteArray(), key, 0, key.size)
            key
        }

    /**
     * Encrypts plaintext data using AES-GCM.
     *
     * @param plaintext The data to encrypt.
     * @param key The symmetric key used for encryption.
     * @return A byte array containing the Initialization Vector (IV) followed by the ciphertext.
     */
    actual suspend fun encryptAesGcm(
        plaintext: ByteArray,
        key: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.Default) {
            val secureRandom = SecureRandom()
            val iv = ByteArray(12)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
            val ciphertext = cipher.doFinal(plaintext)

            iv + ciphertext
        }

    /**
     * Decrypts ciphertext data using AES-GCM.
     *
     * @param ciphertext The data to decrypt, starting with a 12-byte IV.
     * @param key The symmetric key used for decryption.
     * @return The decrypted plaintext byte array.
     */
    actual suspend fun decryptAesGcm(
        ciphertext: ByteArray,
        key: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.Default) {
            if (ciphertext.size < 12) throw IllegalArgumentException("Ciphertext too short")
            val iv = ciphertext.copyOfRange(0, 12)
            val actualCiphertext = ciphertext.copyOfRange(12, ciphertext.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            cipher.doFinal(actualCiphertext)
        }

    /**
     * Encrypts a string into a Base64 encoded format using Argon2 key derivation and AES-GCM.
     *
     * @param data The plaintext string to encrypt.
     * @param password The user-provided password used for key derivation.
     * @return The Base64 encoded payload containing salt, IV, and ciphertext.
     */
    @OptIn(ExperimentalEncodingApi::class)
    actual suspend fun encrypt(
        data: String,
        password: String,
    ): String =
        withContext(Dispatchers.Default) {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)

            val key = deriveKeyArgon2(password, salt)
            val ivAndCiphertext = encryptAesGcm(data.encodeToByteArray(), key)

            val payload = salt + ivAndCiphertext
            Base64.encode(payload)
        }

    /**
     * Decrypts a Base64 encoded payload back into a string using Argon2 key derivation and AES-GCM.
     *
     * @param base64Data The encrypted Base64 string payload.
     * @param password The user-provided password used for key derivation.
     * @return The decrypted plaintext string, or an empty string if decryption fails.
     */
    @OptIn(ExperimentalEncodingApi::class)
    actual suspend fun decrypt(
        base64Data: String,
        password: String,
    ): String =
        withContext(Dispatchers.Default) {
            try {
                val payload = Base64.decode(base64Data)
                if (payload.size < 16 + 12) return@withContext ""

                val salt = payload.copyOfRange(0, 16)
                val ivAndCiphertext = payload.copyOfRange(16, payload.size)

                val key = deriveKeyArgon2(password, salt)
                val plaintext = decryptAesGcm(ivAndCiphertext, key)

                plaintext.decodeToString()
            } catch (e: Exception) {
                ""
            }
        }
}
