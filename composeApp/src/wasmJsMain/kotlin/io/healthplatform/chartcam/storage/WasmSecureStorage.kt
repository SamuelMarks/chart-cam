package io.healthplatform.chartcam.storage

import kotlinx.browser.localStorage

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsModule("crypto-js")
external object CryptoJS : JsAny {
    val AES: AESObj
    val enc: EncObj
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
external interface AESObj : JsAny {
    fun encrypt(message: String, key: String): CipherParams
    fun decrypt(ciphertext: String, key: String): WordArray
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
external interface EncObj : JsAny {
    val Utf8: JsAny
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
external interface CipherParams : JsAny

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(cp) => cp.toString()")
external fun cpToString(cp: CipherParams): String

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
external interface WordArray : JsAny

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(wa, enc) => wa.toString(enc)")
external fun waToString(wa: WordArray, enc: JsAny): String

/**
 * Wasm-specific implementation of [SecureStorage].
 * Since browsers do not provide a synchronous encrypted local storage API by default,
 * this implementation uses [localStorage] combined with the `crypto-js` NPM library
 * to apply AES encryption.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
class WasmSecureStorage : SecureStorage {
    private val secretKey = "ChartCamWebXorKey123" // Encryption key

    /**
     * Saves an encrypted string to [localStorage].
     */
    override fun save(key: String, value: String) {
        val cp = CryptoJS.AES.encrypt(value, secretKey)
        val encrypted = cpToString(cp)
        localStorage.setItem(key, encrypted)
    }

    /**
     * Retrieves and decrypts a string from [localStorage].
     */
    override fun getString(key: String): String? {
        val stored = localStorage.getItem(key) ?: return null
        return try {
            val wa = CryptoJS.AES.decrypt(stored, secretKey)
            val result = waToString(wa, CryptoJS.enc.Utf8)
            if (result.isEmpty()) stored else result // fallback if decryption yields empty
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
 * Factory function to create a [WasmSecureStorage] instance for Wasm.
 *
 * @return A new instance of [WasmSecureStorage].
 */
actual fun createSecureStorage(): SecureStorage = WasmSecureStorage()
