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
external val CryptoJS: dynamic

private const val SEED_LENGTH = 32
private const val STORAGE_KEY_SEED = "chartcam_sec_seed"

/**
 * JS-specific implementation of [SecureStorage].
 * Since browsers do not provide a synchronous encrypted local storage API by default,
 * this implementation uses [localStorage] combined with the `crypto-js` NPM library
 * to apply AES encryption.
 */
class JsSecureStorage : SecureStorage {
    private var inMemorySessionKey: String? = null

    /**
     * Resolves or initializes a randomized master cryptographic seed for the origin.
     * Uses in-memory session key backed by sessionStorage to prevent plaintext key exposure in localStorage.
     *
     * @return The local master key string.
     */
    private fun getMasterKey(): String {
        val existing = inMemorySessionKey ?: kotlinx.browser.sessionStorage.getItem(STORAGE_KEY_SEED)
        if (!existing.isNullOrEmpty()) {
            inMemorySessionKey = existing
            return existing
        }
        val charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val seed = (1..SEED_LENGTH).map { charset.random() }.joinToString("")
        runCatching { kotlinx.browser.sessionStorage.setItem(STORAGE_KEY_SEED, seed) }
        inMemorySessionKey = seed
        return seed
    }

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
        val encrypted = CryptoJS.AES.encrypt(value, getMasterKey()).toString()
        localStorage.setItem(key, encrypted)
    }

    /**
     * Retrieves and decrypts a string from [localStorage].
     *
     * @param key The key of the item to retrieve.
     * @return The decrypted string value, or null if the key doesn't exist or decryption fails.
     */
    override fun getString(key: String): String? {
        val stored = localStorage.getItem(key) ?: return null
        return runCatching {
            val decryptedWords = CryptoJS.AES.decrypt(stored, getMasterKey())
            val result = decryptedWords.toString(CryptoJS.enc.Utf8) as String
            if (result.isEmpty()) null else result
        }.getOrNull()
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
