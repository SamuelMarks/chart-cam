/**
 * @file CryptoService.wasmJs.kt
 * Contains declarations for CryptoService.wasmJs.kt.
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.healthplatform.chartcam.utils

import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

private const val IV_SIZE = 12
private const val SALT_SIZE = 16

private const val GET_WEB_CRYPTO_JS =
    "() => typeof window !== 'undefined' && window.crypto ? window.crypto : " +
        "(typeof global !== 'undefined' && global.crypto ? global.crypto : undefined)"

private const val CREATE_ARGON2_OPTIONS_JS =
    "(password, salt) => ({ password: password, " +
        "salt: new Uint8Array(salt.buffer, salt.byteOffset, salt.length), " +
        "parallelism: 4, iterations: 3, memorySize: 65536, " +
        "hashLength: 32, outputType: 'binary' })"

private const val CONVERT_UINT8_ARRAY_JS =
    "(uint8Array) => new Int8Array(uint8Array.buffer, uint8Array.byteOffset, uint8Array.length)"

private const val ENCRYPT_AES_GCM_JS =
    "(plaintext, key, crypto) => (async () => { " +
        "const iv = new Uint8Array(12); crypto.getRandomValues(iv); " +
        "const cryptoKey = await crypto.subtle.importKey('raw', " +
        "new Uint8Array(key.buffer, key.byteOffset, key.length), " +
        "{ name: 'AES-GCM' }, false, ['encrypt']); " +
        "const ciphertextBuffer = await crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, " +
        "cryptoKey, " +
        "new Uint8Array(plaintext.buffer, plaintext.byteOffset, plaintext.length)); " +
        "const ciphertext = new Uint8Array(ciphertextBuffer); " +
        "const result = new Int8Array(12 + ciphertext.length); " +
        "result.set(new Int8Array(iv.buffer), 0); " +
        "result.set(new Int8Array(ciphertext.buffer), 12); return result; })()"

private const val DECRYPT_AES_GCM_JS =
    "(ciphertext, key, crypto) => (async () => { " +
        "const ct = new Uint8Array(ciphertext.buffer, ciphertext.byteOffset, ciphertext.length); " +
        "if (ct.length < 12) throw new Error('Ciphertext too short'); " +
        "const iv = ct.slice(0, 12); const actualCiphertext = ct.slice(12); " +
        "const cryptoKey = await crypto.subtle.importKey('raw', " +
        "new Uint8Array(key.buffer, key.byteOffset, key.length), { name: 'AES-GCM' }, false, ['decrypt']); " +
        "const plaintextBuffer = await crypto.subtle.decrypt(" +
        "{ name: 'AES-GCM', iv: iv }, cryptoKey, actualCiphertext); " +
        "return new Int8Array(plaintextBuffer); })()"

private const val GET_RANDOM_VALUES_JS =
    "(array, crypto) => crypto.getRandomValues(new Uint8Array(array.buffer, array.byteOffset, array.length))"

/**
 * External WASM module wrapper for hash-wasm providing argon2id implementation.
 */
@JsModule("hash-wasm")
external object HashWasm {
    /**
     * Argon2id hashing wrapper.
     *
     * @return the promise of the derived hash.
     * @param ignored The ignored.
     */
    fun argon2id(ignored: JsAny): Promise<JsAny>
}

/**
 * Retrieves the Web Crypto API object depending on the environment (browser or Node.js).
 *
 * @return The crypto object as [JsAny].
 */
@JsFun(GET_WEB_CRYPTO_JS)
private external fun getWebCrypto(): JsAny?

/**
 * Derives a cryptographic key using the Argon2 hashing algorithm in JavaScript.
 *
 * @param password The plaintext password.
 * @param salt The cryptographic salt.
 * @return A promise containing the derived key as [JsAny].
 */
@JsFun(CREATE_ARGON2_OPTIONS_JS)
private external fun createArgon2Options(
    password: String,
    salt: Int8Array,
): JsAny

/**
 * Converts a Javascript Uint8Array into a Javascript Int8Array.
 *
 * @param uint8Array The input Uint8Array as a [JsAny].
 * @return The corresponding [Int8Array].
 */
@JsFun(CONVERT_UINT8_ARRAY_JS)
private external fun convertUint8ArrayToInt8Array(uint8Array: JsAny): Int8Array

/**
 * Encrypts data using AES-GCM via the Web Crypto API.
 *
 * @param plaintext The plaintext data to encrypt.
 * @param key The encryption key.
 * @param crypto The Web Crypto API object.
 * @return A promise containing the encrypted data as [JsAny].
 */
@JsFun(ENCRYPT_AES_GCM_JS)
private external fun encryptAesGcmJs(
    plaintext: Int8Array,
    key: Int8Array,
    crypto: JsAny,
): Promise<JsAny>

/**
 * Decrypts data using AES-GCM via the Web Crypto API.
 *
 * @param ciphertext The encrypted data.
 * @param key The encryption key.
 * @param crypto The Web Crypto API object.
 * @return A promise containing the decrypted data as [JsAny].
 */
@JsFun(DECRYPT_AES_GCM_JS)
private external fun decryptAesGcmJs(
    ciphertext: Int8Array,
    key: Int8Array,
    crypto: JsAny,
): Promise<JsAny>

/**
 * Generates cryptographically secure random values.
 *
 * @param array The array to fill with random values.
 * @param crypto The Web Crypto API object.
 */
@JsFun(GET_RANDOM_VALUES_JS)
private external fun getRandomValuesJs(
    array: Int8Array,
    crypto: JsAny,
): Unit

/**
 * * Service providing cryptographic operations on the WasmJS platform
 * by delegating to JavaScript WebCrypto and hash-wasm.
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
        val options = createArgon2Options(password, salt.toInt8Array())
        val hashJs = HashWasm.argon2id(options).await()
        return convertUint8ArrayToInt8Array(hashJs).toByteArray()
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
        val crypto = getWebCrypto() ?: error("WebCrypto not found")
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
        val crypto = getWebCrypto() ?: error("WebCrypto not found")
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
        val saltBytes = ByteArray(SALT_SIZE)
        val saltInt8 = saltBytes.toInt8Array()
        val crypto = getWebCrypto() ?: error("WebCrypto not found")
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
    ): String =
        try {
            val payload = Base64.decode(base64Data)
            require(payload.size >= SALT_SIZE + IV_SIZE) { "Payload too short" }

            val salt = payload.copyOfRange(0, SALT_SIZE)
            val ivAndCiphertext = payload.copyOfRange(SALT_SIZE, payload.size)

            val key = deriveKeyArgon2(password, salt)
            val plaintext = decryptAesGcm(ivAndCiphertext, key)

            plaintext.decodeToString()
        } catch (ignored: IllegalArgumentException) {
            println(ignored.message)
            ""
        } catch (ignored: Exception) {
            ""
        } catch (ignored: Exception) {
            ; ""
        } catch (ignored: Exception) {
            ""
        }

    /**
     * Converts a Kotlin [ByteArray] to a JS [Int8Array].
     *
     * @return The converted [Int8Array].
     */
    private fun ByteArray.toInt8Array(): Int8Array {
        val array = Int8Array(this.size)
        for (i in this.indices) {
            setInt8Js(array, i, this[i])
        }
        return array
    }

    /**
     * Converts a JS [Int8Array] to a Kotlin [ByteArray].
     *
     * @return The converted [ByteArray].
     */
    private fun Int8Array.toByteArray(): ByteArray = ByteArray(this.length) { getInt8Js(this, it) }
}

/**
 * Gets a byte at the specified index from an [Int8Array].
 *
 * @param array The JS [Int8Array].
 * @param index The index to read from.
 * @return The byte at the specified index.
 */
@JsFun("(array, index) => array[index]")
private external fun getInt8Js(
    array: Int8Array,
    index: Int,
): Byte

/**
 * Sets a byte at the specified index in an [Int8Array].
 *
 * @param array The JS [Int8Array].
 * @param index The index to write to.
 * @param value The byte value to write.
 */
@JsFun("(array, index, value) => { array[index] = value; }")
private external fun setInt8Js(
    array: Int8Array,
    index: Int,
    value: Byte,
): Unit
