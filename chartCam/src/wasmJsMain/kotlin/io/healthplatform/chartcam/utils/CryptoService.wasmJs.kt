/**
 * @file CryptoService.wasmJs.kt
 * @file CryptoService.wasmJs.kt
 * Contains declarations for CryptoService.wasmJs.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

private fun getWebCrypto(): JsAny? =
    js(
        "typeof window !== 'undefined' && window.crypto ? window.crypto : (typeof global !== 'undefined' && global.crypto ? global.crypto : require('crypto').webcrypto)",
    )

private fun deriveKeyArgon2Js(
    password: String,
    salt: Int8Array,
): Promise<JsAny> =
    js(
        """
    (async () => {
        const hashWasm = require('hash-wasm');
        const hash = await hashWasm.argon2id({
            password: password,
            salt: new Uint8Array(salt.buffer, salt.byteOffset, salt.length),
            parallelism: 4,
            iterations: 3,
            memorySize: 65536,
            hashLength: 32,
            outputType: 'binary'
        });
        return new Int8Array(hash);
    })()
""",
    )

private fun encryptAesGcmJs(
    plaintext: Int8Array,
    key: Int8Array,
    crypto: JsAny,
): Promise<JsAny> =
    js(
        """
    (async () => {
        const iv = new Uint8Array(12);
        crypto.getRandomValues(iv);
        
        const cryptoKey = await crypto.subtle.importKey(
            "raw",
            new Uint8Array(key.buffer, key.byteOffset, key.length),
            { name: 'AES-GCM' },
            false,
            ["encrypt"]
        );
        
        const ciphertextBuffer = await crypto.subtle.encrypt(
            { name: 'AES-GCM', iv: iv },
            cryptoKey,
            new Uint8Array(plaintext.buffer, plaintext.byteOffset, plaintext.length)
        );
        
        const ciphertext = new Uint8Array(ciphertextBuffer);
        const result = new Int8Array(12 + ciphertext.length);
        result.set(new Int8Array(iv.buffer), 0);
        result.set(new Int8Array(ciphertext.buffer), 12);
        return result;
    })()
""",
    )

private fun decryptAesGcmJs(
    ciphertext: Int8Array,
    key: Int8Array,
    crypto: JsAny,
): Promise<JsAny> =
    js(
        """
    (async () => {
        const ct = new Uint8Array(ciphertext.buffer, ciphertext.byteOffset, ciphertext.length);
        if (ct.length < 12) throw new Error("Ciphertext too short");
        
        const iv = ct.slice(0, 12);
        const actualCiphertext = ct.slice(12);
        
        const cryptoKey = await crypto.subtle.importKey(
            "raw",
            new Uint8Array(key.buffer, key.byteOffset, key.length),
            { name: 'AES-GCM' },
            false,
            ["decrypt"]
        );
        
        const plaintextBuffer = await crypto.subtle.decrypt(
            { name: 'AES-GCM', iv: iv },
            cryptoKey,
            actualCiphertext
        );
        
        return new Int8Array(plaintextBuffer);
    })()
""",
    )

private fun getRandomValuesJs(
    array: Int8Array,
    crypto: JsAny,
): Unit =
    js(
        """
    crypto.getRandomValues(new Uint8Array(array.buffer, array.byteOffset, array.length))
""",
    )

/**
 * Service providing cryptographic operations on the WasmJS platform by delegating to JavaScript WebCrypto and hash-wasm.
 */
actual class CryptoService actual constructor() {
    /**
     * Derives a cryptographic key using the Argon2 hashing algorithm via JS interop.
     *
     * @param password The user-provided password string.
     * @param salt The salt byte array.
     * @return The derived cryptographic key.
     */
    actual suspend fun deriveKeyArgon2(
        password: String,
        salt: ByteArray,
    ): ByteArray {
        val resultJs = deriveKeyArgon2Js(password, salt.toInt8Array()).await()
        return resultJs.unsafeCast<Int8Array>().toByteArray()
    }

    /**
     * Encrypts plaintext data using AES-GCM via JS interop.
     *
     * @param plaintext The data to encrypt.
     * @param key The symmetric key used for encryption.
     * @return A byte array containing the Initialization Vector (IV) followed by the ciphertext.
     */
    actual suspend fun encryptAesGcm(
        plaintext: ByteArray,
        key: ByteArray,
    ): ByteArray {
        val crypto = getWebCrypto() ?: throw IllegalStateException("WebCrypto not found")
        val resultJs = encryptAesGcmJs(plaintext.toInt8Array(), key.toInt8Array(), crypto).await()
        return resultJs.unsafeCast<Int8Array>().toByteArray()
    }

    /**
     * Decrypts ciphertext data using AES-GCM via JS interop.
     *
     * @param ciphertext The data to decrypt, starting with a 12-byte IV.
     * @param key The symmetric key used for decryption.
     * @return The decrypted plaintext byte array.
     */
    actual suspend fun decryptAesGcm(
        ciphertext: ByteArray,
        key: ByteArray,
    ): ByteArray {
        val crypto = getWebCrypto() ?: throw IllegalStateException("WebCrypto not found")
        val resultJs = decryptAesGcmJs(ciphertext.toInt8Array(), key.toInt8Array(), crypto).await()
        return resultJs.unsafeCast<Int8Array>().toByteArray()
    }

    /**
     * Encrypts a string into a Base64 encoded format using Argon2 key derivation and AES-GCM via JS interop.
     *
     * @param data The plaintext string to encrypt.
     * @param password The user-provided password used for key derivation.
     * @return The Base64 encoded payload containing salt, IV, and ciphertext.
     */
    @OptIn(ExperimentalEncodingApi::class)
    actual suspend fun encrypt(
        data: String,
        password: String,
    ): String {
        val saltBytes = ByteArray(16)
        val saltInt8 = saltBytes.toInt8Array()
        val crypto = getWebCrypto() ?: throw IllegalStateException("WebCrypto not found")
        getRandomValuesJs(saltInt8, crypto)
        val salt = saltInt8.toByteArray()

        val key = deriveKeyArgon2(password, salt)
        val ivAndCiphertext = encryptAesGcm(data.encodeToByteArray(), key)

        val payload = salt + ivAndCiphertext
        return Base64.encode(payload)
    }

    /**
     * Decrypts a Base64 encoded payload back into a string using Argon2 key derivation and AES-GCM via JS interop.
     *
     * @param base64Data The encrypted Base64 string payload.
     * @param password The user-provided password used for key derivation.
     * @return The decrypted plaintext string, or an empty string if decryption fails.
     */
    @OptIn(ExperimentalEncodingApi::class)
    actual suspend fun decrypt(
        base64Data: String,
        password: String,
    ): String {
        try {
            val payload = Base64.decode(base64Data)
            if (payload.size < 16 + 12) return ""

            val salt = payload.copyOfRange(0, 16)
            val ivAndCiphertext = payload.copyOfRange(16, payload.size)

            val key = deriveKeyArgon2(password, salt)
            val plaintext = decryptAesGcm(ivAndCiphertext, key)

            return plaintext.decodeToString()
        } catch (e: Exception) {
            return ""
        }
    }

    private fun ByteArray.toInt8Array(): Int8Array {
        val array = Int8Array(this.size)
        for (i in this.indices) {
            setInt8Js(array, i, this[i])
        }
        return array
    }

    private fun Int8Array.toByteArray(): ByteArray = ByteArray(this.length) { getInt8Js(this, it) }
}

private fun getInt8Js(
    array: Int8Array,
    index: Int,
): Byte = js("array[index]")

private fun setInt8Js(
    array: Int8Array,
    index: Int,
    value: Byte,
): Unit = js("array[index] = value")
