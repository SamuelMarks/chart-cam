/**
 * @file JsSecureStorage.kt
 * JS-specific secure storage implementation.
 */
package io.healthplatform.chartcam.storage

import kotlinx.browser.localStorage

/**
 * External declaration for the crypto-js library to handle AES encryption and decryption.
 */
@JsModule("crypto-js")
@JsNonModule
external object CryptoJS {
    /**
     * Advanced Encryption Standard module.
     */
    object AES {
        /**
         * Encrypts a message using a secret key.
         *
         * @param message The plaintext message to encrypt.
         * @param key The secret key used for encryption.
         * @return A dynamic object representing the encrypted ciphertext.
         */
        fun encrypt(
            message: String,
            key: String,
        ): dynamic

        /**
         * Decrypts ciphertext back to the original message.
         *
         * @param ciphertext The encrypted string.
         * @param key The secret key used for decryption.
         * @return A dynamic object representing the decrypted plaintext words.
         */
        fun decrypt(
            ciphertext: String,
            key: String,
        ): dynamic
    }

    /**
     * Encoding module.
     */
    @Suppress("ktlint:standard:class-naming")
    object enc {
        /**
         * Utf8 encoding format.
         */
        val Utf8: dynamic
    }
}

/**
 * JS-specific implementation of [SecureStorage].
 * Since browsers do not provide a synchronous encrypted local storage API by default,
 * this implementation uses [localStorage] combined with the `crypto-js` NPM library
 * to apply AES encryption.
 */
class JsSecureStorage : SecureStorage {
    /**
     * The fixed secret key used for symmetric encryption.
     */
    private val secretKey = "ChartCamWebXorKey123" // Encryption key

    /**
     * Saves an encrypted string to [localStorage].
     *
     * @param key The string key to store the value under.
     * @param value The plaintext value to be encrypted and stored.
     */
    override fun save(
        key: String,
        value: String,
    ) {
        val encrypted = CryptoJS.AES.encrypt(value, secretKey).toString()
        localStorage.setItem(key, encrypted)
    }

    /**
     * Retrieves and decrypts a string from [localStorage].
     *
     * @param key The key of the item to retrieve.
     * @return The decrypted string value, or null if the key doesn't exist. Falls back to raw string on error.
     */
    override fun getString(key: String): String? {
        val stored = localStorage.getItem(key) ?: return null
        return try {
            val decryptedWords = CryptoJS.AES.decrypt(stored, secretKey)
            val result = decryptedWords.toString(CryptoJS.enc.Utf8) as String
            if (result.isEmpty()) stored else result // fallback if decryption yields empty due to bad key/data
        } catch (_: Throwable) {
            stored
        }
    }

    /**
     * Deletes a stored value from [localStorage].
     *
     * @param key The key of the item to delete.
     */
    override fun delete(key: String) {
        localStorage.removeItem(key)
    }
}

/**
 * Factory function to create a [JsSecureStorage] instance for JS.
 *
 * @return A new instance of [JsSecureStorage].
 */
actual fun createSecureStorage(): SecureStorage = JsSecureStorage()
