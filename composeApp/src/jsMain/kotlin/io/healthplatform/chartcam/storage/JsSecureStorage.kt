package io.healthplatform.chartcam.storage

import kotlinx.browser.localStorage

@JsModule("crypto-js")
@JsNonModule
external object CryptoJS {
    object AES {
        fun encrypt(message: String, key: String): dynamic
        fun decrypt(ciphertext: String, key: String): dynamic
    }
    object enc {
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
    private val secretKey = "ChartCamWebXorKey123" // Encryption key

    /**
     * Saves an encrypted string to [localStorage].
     */
    override fun save(key: String, value: String) {
        val encrypted = CryptoJS.AES.encrypt(value, secretKey).toString()
        localStorage.setItem(key, encrypted)
    }

    /**
     * Retrieves and decrypts a string from [localStorage].
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
