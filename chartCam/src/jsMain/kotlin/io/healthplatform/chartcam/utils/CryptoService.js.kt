/**
 * @file CryptoService.js.kt
 * Contains declarations for CryptoService.js.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

private const val ARGON_PARALLELISM = 4
private const val ARGON_ITERATIONS = 3
private const val ARGON_MEMORY = 65536
private const val ARGON_HASH_LENGTH = 32
private const val IV_LENGTH = 12
private const val SALT_LENGTH = 16

/**
 * External JS module for hash-wasm providing argon2id implementation.
 */
@JsModule("hash-wasm")
@JsNonModule
external object HashWasm {
    /**
     * Dummy.
     */
    val dummy: dynamic
}

private val webCrypto: dynamic =
    js(
        """
        typeof window !== 'undefined' && window.crypto ? window.crypto : 
        (typeof global !== 'undefined' && global.crypto ? global.crypto : require('crypto').webcrypto)
        """,
    )

/**
 * Service providing cryptographic operations on the JS platform using WebCrypto and hash-wasm.
 */
actual class CryptoService actual constructor() {
    /**
     * Derives a cryptographic key using the Argon2 hashing algorithm via hash-wasm.
     *
     * @param password The user-provided password string.
     * @param salt The salt byte array.
     * @return The derived cryptographic key.
     */
    actual suspend fun deriveKeyArgon2(
        password: String,
        salt: ByteArray,
    ): ByteArray {
        val options = js("{}")
        options.password = password
        options.salt = salt.toUint8Array()
        options.parallelism = ARGON_PARALLELISM
        options.iterations = ARGON_ITERATIONS
        options.memorySize = ARGON_MEMORY
        options.hashLength = ARGON_HASH_LENGTH
        options.outputType = "binary"

        val uint8Array =
            HashWasm
                .asDynamic()
                .argon2id(options)
                .unsafeCast<Promise<Uint8Array>>()
                .await()
        return uint8Array.toByteArray()
    }

    /**
     * Encrypts plaintext data using AES-GCM via WebCrypto.
     *
     * @param plaintext The data to encrypt.
     * @param key The symmetric key used for encryption.
     * @return A byte array containing the Initialization Vector (IV) followed by the ciphertext.
     */
    actual suspend fun encryptAesGcm(
        plaintext: ByteArray,
        key: ByteArray,
    ): ByteArray {
        val iv = Uint8Array(IV_LENGTH)
        webCrypto.getRandomValues(iv)

        val cryptoKey =
            webCrypto.subtle
                .importKey(
                    "raw",
                    key.toUint8Array(),
                    js("{ name: 'AES-GCM' }"),
                    false,
                    arrayOf("encrypt"),
                ).unsafeCast<Promise<dynamic>>()
                .await()

        val algorithm = js("{}")
        algorithm.name = "AES-GCM"
        algorithm.iv = iv

        val ciphertextBuffer =
            webCrypto.subtle
                .encrypt(
                    algorithm,
                    cryptoKey,
                    plaintext.toUint8Array(),
                ).unsafeCast<Promise<dynamic>>()
                .await()

        val ciphertext = Uint8Array(ciphertextBuffer.unsafeCast<org.khronos.webgl.ArrayBuffer>())

        val result = ByteArray(IV_LENGTH + ciphertext.length)
        val ivBytes = iv.toByteArray()
        val ctBytes = ciphertext.toByteArray()
        for (i in 0 until IV_LENGTH) result[i] = ivBytes[i]
        for (i in 0 until ctBytes.size) result[IV_LENGTH + i] = ctBytes[i]
        return result
    }

    /**
     * Decrypts ciphertext data using AES-GCM via WebCrypto.
     *
     * @param ciphertext The data to decrypt, starting with a 12-byte IV.
     * @param key The symmetric key used for decryption.
     * @return The decrypted plaintext byte array.
     */
    actual suspend fun decryptAesGcm(
        ciphertext: ByteArray,
        key: ByteArray,
    ): ByteArray {
        require(ciphertext.size >= IV_LENGTH) { "Ciphertext too short" }
        val iv = ciphertext.copyOfRange(0, IV_LENGTH).toUint8Array()
        val actualCiphertext = ciphertext.copyOfRange(IV_LENGTH, ciphertext.size).toUint8Array()

        val cryptoKey =
            webCrypto.subtle
                .importKey(
                    "raw",
                    key.toUint8Array(),
                    js("{ name: 'AES-GCM' }"),
                    false,
                    arrayOf("decrypt"),
                ).unsafeCast<Promise<dynamic>>()
                .await()

        val algorithm = js("{}")
        algorithm.name = "AES-GCM"
        algorithm.iv = iv

        val plaintextBuffer =
            webCrypto.subtle
                .decrypt(
                    algorithm,
                    cryptoKey,
                    actualCiphertext,
                ).unsafeCast<Promise<dynamic>>()
                .await()

        return Uint8Array(plaintextBuffer.unsafeCast<org.khronos.webgl.ArrayBuffer>()).toByteArray()
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
    ): String {
        val salt = Uint8Array(SALT_LENGTH)
        webCrypto.getRandomValues(salt)

        val key = deriveKeyArgon2(password, salt.toByteArray())
        val ivAndCiphertext = encryptAesGcm(data.encodeToByteArray(), key)

        val payload = salt.toByteArray() + ivAndCiphertext
        return Base64.encode(payload)
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
        try {
            val payload = Base64.decode(base64Data)
            require(payload.size >= SALT_LENGTH + IV_LENGTH) { "Payload too short" }

            val salt = payload.copyOfRange(0, SALT_LENGTH)
            val ivAndCiphertext = payload.copyOfRange(SALT_LENGTH, payload.size)

            val key = deriveKeyArgon2(password, salt)
            val plaintext = decryptAesGcm(ivAndCiphertext, key)

            plaintext.decodeToString()
        } catch (e: IllegalStateException) {
            println(e.message)
            ""
        }

    /**
     * Converts a Kotlin [ByteArray] to a JS [Uint8Array].
     *
     * @return The converted [Uint8Array].
     */
    private fun ByteArray.toUint8Array(): Uint8Array =
        Uint8Array(
            this.unsafeCast<org.khronos.webgl.Int8Array>().buffer,
            this.unsafeCast<org.khronos.webgl.Int8Array>().byteOffset,
            this.size,
        )

    /**
     * Converts a JS [Uint8Array] to a Kotlin [ByteArray].
     *
     * @return The converted [ByteArray].
     */
    private fun Uint8Array.toByteArray(): ByteArray =
        org.khronos.webgl
            .Int8Array(this.buffer, this.byteOffset, this.length)
            .unsafeCast<ByteArray>()
}
