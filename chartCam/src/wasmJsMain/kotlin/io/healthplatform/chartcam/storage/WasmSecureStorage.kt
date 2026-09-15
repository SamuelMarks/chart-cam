/**
 * @file WasmSecureStorage.kt
 *
 * Provides the WebAssembly (WasmJs) implementation of [SecureStorage].
 * Uses browser `localStorage` in combination with AES encryption from the `crypto-js` library.
 */
package io.healthplatform.chartcam.storage

import kotlinx.browser.localStorage

/**
 * External declaration for the `crypto-js` module in JavaScript.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsModule("crypto-js")
external object CryptoJS : JsAny {
    /**
     * Provides access to AES encryption and decryption functions.
     */
    val AES: AESObj

    /**
     * Provides access to encoding options, such as UTF-8.
     */
    val enc: EncObj
}

/**
 * External interface representing the AES object from `crypto-js`.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
external interface AESObj : JsAny {
    /**
     * Encrypts a message using a secret key.
     *
     * @param message The plain text message to encrypt.
     * @param key The secret key used for encryption.
     * @return An object containing cipher parameters, representing the encrypted output.
     */
    fun encrypt(
        message: String,
        key: String,
    ): CipherParams

    /**
     * Decrypts a ciphertext string using a secret key.
     *
     * @param ciphertext The encrypted string to decrypt.
     * @param key The secret key used for decryption.
     * @return A word array representing the decrypted bytes.
     */
    fun decrypt(
        ciphertext: String,
        key: String,
    ): WordArray
}

/**
 * External interface representing the encoding object from `crypto-js`.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
external interface EncObj : JsAny {
    /**
     * Represents the UTF-8 encoding type.
     */
    @JsName("Utf8")
    val utf8: JsAny
}

/**
 * External interface representing cipher parameters returned by `crypto-js` encryption.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
external interface CipherParams : JsAny

/**
 * Converts a [CipherParams] object to its string representation (ciphertext) via a JS function.
 *
 * @param cp The cipher parameters to convert.
 * @return The resulting ciphertext as a string.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(cp) => cp.toString()")
external fun cpToString(cp: CipherParams): String

/**
 * External interface representing a word array returned by `crypto-js` decryption.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
external interface WordArray : JsAny

/**
 * Safely decrypts a ciphertext string using a secret key, catching JavaScript exceptions.
 *
 * @param aes The AES object.
 * @param ciphertext The encrypted string to decrypt.
 * @param key The secret key used for decryption.
 * @return The resulting word array, or null if decryption fails.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(aes, ciphertext, key) => { try { return aes.decrypt(ciphertext, key); } catch (e) { return null; } }")
external fun safeDecrypt(
    aes: AESObj,
    ciphertext: String,
    key: String,
): WordArray?

/**
 * Converts a [WordArray] to a string using a specific encoding (e.g., UTF-8) via a JS function.
 *
 * @param wa The word array to convert.
 * @param enc The encoding object to use (e.g., [EncObj.Utf8]).
 * @return The decoded plaintext string.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(wa, enc) => { try { return wa ? wa.toString(enc) : ''; } catch (e) { return ''; } }")
external fun waToString(
    wa: WordArray?,
    enc: JsAny,
): String

private const val SEED_LENGTH = 32
private const val STORAGE_KEY_SEED = "chartcam_sec_seed"

/**
 * Wasm-specific implementation of [SecureStorage].
 * Since browsers do not provide a synchronous encrypted local storage API by default,
 * this implementation uses [localStorage] combined with the `crypto-js` NPM library
 * to apply AES encryption.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
class WasmSecureStorage : SecureStorage {
    private var inMemorySessionKey: String? = null

    /**
     * Resolves or initializes a randomized master cryptographic seed for the origin.
     *
     * @return The local master key string.
     */
    private fun getMasterKey(): String {
        val existing = inMemorySessionKey ?: localStorage.getItem(STORAGE_KEY_SEED)
        if (!existing.isNullOrBlank()) {
            inMemorySessionKey = existing
            return existing
        }
        val charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val seed = (1..SEED_LENGTH).map { charset.random() }.joinToString("")
        localStorage.setItem(STORAGE_KEY_SEED, seed)
        inMemorySessionKey = seed
        return seed
    }

    /**
     * Encrypts the given value and saves it to [localStorage] under the specified key.
     *
     * @param key The unique key used to identify the stored value.
     * @param value The plaintext string to encrypt and store.
     */
    override fun save(
        key: String,
        value: String,
    ) {
        val cp = CryptoJS.AES.encrypt(value, getMasterKey())
        val encrypted = cpToString(cp)
        localStorage.setItem(key, encrypted)
    }

    /**
     * Retrieves an encrypted string from [localStorage] and decrypts it.
     * If decryption fails or data is corrupted, null is returned.
     *
     * @param key The unique key identifying the stored value.
     * @return The decrypted string, or null if the key does not exist or decryption fails.
     */
    override fun getString(key: String): String? {
        val stored = localStorage.getItem(key) ?: return null
        val wa = safeDecrypt(CryptoJS.AES, stored, getMasterKey())
        val result = if (wa != null) waToString(wa, CryptoJS.enc.utf8) else ""
        return if (result.isEmpty()) null else result
    }

    /**
     * Deletes a stored value from [localStorage] associated with the specified key.
     *
     * @param key The unique key identifying the value to delete.
     */
    override fun delete(key: String) {
        localStorage.removeItem(key)
    }
}

/**
 * Factory function to create a [WasmSecureStorage] instance for the Wasm target.
 *
 * @return A new instance of [WasmSecureStorage] implementing [SecureStorage].
 */
actual fun createSecureStorage(): SecureStorage = WasmSecureStorage()
