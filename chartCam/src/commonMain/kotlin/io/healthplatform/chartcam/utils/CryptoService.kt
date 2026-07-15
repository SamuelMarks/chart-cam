/**
 * @file CryptoService.kt
 * Contains declarations for CryptoService.kt.
 */
package io.healthplatform.chartcam.utils

/**
 * Service to handle encryption and decryption of data.
 *
 * This implementation utilizes platform-native AES-GCM (Advanced Encryption Standard with Galois/Counter Mode)
 * via expect/actual bindings, ensuring that key derivation uses Argon2.
 */
expect class CryptoService() {
    /**
     * Derives a cryptographic key using Argon2.
     * @param password The password to use.
     * @param salt The salt (must be generated securely, e.g., via SecureRandom/WebCrypto).
     * @return The derived key.
     */
    suspend fun deriveKeyArgon2(
        password: String,
        salt: ByteArray,
    ): ByteArray

    /**
     * Encrypts plaintext using AES-GCM.
     * @param plaintext The data to encrypt.
     * @param key The AES key (derived via Argon2).
     * @return The resulting ciphertext, prepended with the IV.
     */
    suspend fun encryptAesGcm(
        plaintext: ByteArray,
        key: ByteArray,
    ): ByteArray

    /**
     * Decrypts ciphertext using AES-GCM.
     * @param ciphertext The ciphertext prepended with the IV.
     * @param key The AES key (derived via Argon2).
     * @return The decrypted plaintext.
     */
    suspend fun decryptAesGcm(
        ciphertext: ByteArray,
        key: ByteArray,
    ): ByteArray

    /**
     * Encrypts the [data] string using the given [password].
     * Generates a random salt, derives a key using Argon2, encrypts with AES-GCM,
     * and returns a base64-encoded string combining salt + iv + ciphertext.
     *
     * @param data The plaintext data to encrypt.
     * @param password The password to use as a key.
     * @return The base64-encoded encrypted string.
     */
    suspend fun encrypt(
        data: String,
        password: String,
    ): String

    /**
     * Decrypts the [base64Data] string using the given [password].
     * Expects the data to contain salt + iv + ciphertext.
     *
     * @param base64Data The base64-encoded encrypted data.
     * @param password The password to use as a key.
     * @return The decrypted plaintext string, or an empty string if decryption fails.
     */
    suspend fun decrypt(
        base64Data: String,
        password: String,
    ): String
}
