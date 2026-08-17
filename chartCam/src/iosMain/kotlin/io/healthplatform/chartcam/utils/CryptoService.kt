@file:Suppress(
    "ktlint:standard:no-wildcard-imports",
    "WildcardImport",
    "UNCHECKED_CAST",
    "CAST_NEVER_SUCCEEDS",
    "USELESS_CAST",
)
/**
 * @file CryptoService.ios.kt
 * Contains declarations for CryptoService.ios.kt.
 */

package io.healthplatform.chartcam.utils

import argon2.*
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreCrypto.*
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.posix.size_tVar
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Service providing cryptographic operations on the iOS platform using CommonCrypto and argon2 C-interop.
 */
@OptIn(ExperimentalForeignApi::class)
private const val GCM_IV_LENGTH = 12
private const val GCM_IV_LENGTH_U = 12UL
private const val GCM_TAG_LENGTH = 16
private const val GCM_TAG_LENGTH_U = 16UL
private const val ARGON2_SALT_LEN = 16
private const val ARGON2_HASH_LEN = 32

/**
 * CryptoService implementation for iOS.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual class CryptoService actual constructor() {
    /**
     * Inits cryptor.
     * @param op The op.
     * @param key The key.
     * @return The cryptor reference.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun MemScope.initCryptor(
        op: platform.CoreCrypto.CCOperation,
        key: ByteArray,
    ): CCCryptorRefVar {
        val cryptor = alloc<CCCryptorRefVar>()
        val status =
            key.usePinned { keyPinned ->
                CCCryptorCreateWithMode(
                    op,
                    kCCModeGCM.convert(),
                    kCCAlgorithmAES,
                    ccNoPadding,
                    null,
                    keyPinned.addressOf(0),
                    key.size.convert(),
                    null,
                    0u,
                    0,
                    0u,
                    cryptor.ptr,
                )
            }
        if (status != kCCSuccess) error("CCCryptorCreateWithMode failed: $status")
        return cryptor
    }

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
            val pwdBytes = password.encodeToByteArray()
            val key = ByteArray(ARGON2_HASH_LEN)

            val result =
                pwdBytes.usePinned { pwdPinned ->
                    salt.usePinned { saltPinned ->
                        key.usePinned { keyPinned ->
                            argon2_hash(
                                3u, // t_cost (iterations)
                                65536u, // m_cost (memory in KB)
                                4u, // parallelism
                                if (pwdBytes.isNotEmpty()) pwdPinned.addressOf(0) else null,
                                pwdBytes.size.convert(),
                                saltPinned.addressOf(0),
                                salt.size.convert(),
                                keyPinned.addressOf(0),
                                key.size.convert(),
                                null,
                                0u,
                                Argon2_id,
                                ARGON2_VERSION_13.convert(),
                            )
                        }
                    }
                }
            if (result != argon2.ARGON2_OK) {
                error("Argon2 key derivation failed with error code: $result")
            }
            key
        }

    /**
     * Encrypts plaintext data using AES-GCM.
     *
     * @param plaintext The data to encrypt.
     * @param key The symmetric key used for encryption.
     * @return A byte array containing the Initialization Vector (IV), the ciphertext, and the authentication tag.
     */
    actual suspend fun encryptAesGcm(
        plaintext: ByteArray,
        key: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.Default) {
            val iv = ByteArray(GCM_IV_LENGTH)
            iv.usePinned { ivPinned ->
                SecRandomCopyBytes(kSecRandomDefault, GCM_IV_LENGTH_U, ivPinned.addressOf(0))
            }

            val ciphertext = ByteArray(plaintext.size)
            val tag = ByteArray(GCM_TAG_LENGTH)

            memScoped {
                val cryptor = initCryptor(kCCEncrypt, key)
                var status = kCCSuccess

                val cryptorRef = cryptor.value!!

                try {
                    status =
                        iv.usePinned { ivPinned ->
                            my_CCCryptorGCMAddIV(cryptorRef, ivPinned.addressOf(0), GCM_IV_LENGTH_U)
                        }
                    if (status != kCCSuccess) error("my_CCCryptorGCMAddIV failed: $status")

                    if (plaintext.isNotEmpty()) {
                        status =
                            plaintext.usePinned { ptPinned ->
                                ciphertext.usePinned { ctPinned ->
                                    CCCryptorUpdate(
                                        cryptorRef,
                                        ptPinned.addressOf(0),
                                        plaintext.size.convert(),
                                        ctPinned.addressOf(0),
                                        ciphertext.size.convert(),
                                        alloc<platform.posix.size_tVar>().ptr,
                                    )
                                }
                            }
                        if (status != kCCSuccess) error("CCCryptorUpdate failed: $status")
                    }

                    status =
                        tag.usePinned { tagPinned ->
                            val tagLenVar = alloc<platform.posix.size_tVar>()
                            tagLenVar.value = GCM_TAG_LENGTH_U
                            my_CCCryptorGCMFinal(cryptorRef, tagPinned.addressOf(0), tagLenVar.ptr)
                        }
                    if (status != kCCSuccess) error("my_CCCryptorGCMFinal failed: $status")
                } finally {
                    CCCryptorRelease(cryptorRef)
                }
            }

            iv + ciphertext + tag
        }

    /**
     * Decrypts ciphertext data using AES-GCM.
     *
     * @param ciphertext The data to decrypt, starting with a 12-byte IV and ending with a 16-byte tag.
     * @param key The symmetric key used for decryption.
     * @return The decrypted plaintext byte array.
     */
    actual suspend fun decryptAesGcm(
        ciphertext: ByteArray,
        key: ByteArray,
    ): ByteArray =
        withContext(Dispatchers.Default) {
            if (ciphertext.size < GCM_IV_LENGTH + GCM_TAG_LENGTH) require(false) { "Ciphertext too short" }

            val iv = ciphertext.copyOfRange(0, GCM_IV_LENGTH)
            val actualCiphertext = ciphertext.copyOfRange(GCM_IV_LENGTH, ciphertext.size - GCM_TAG_LENGTH)
            val expectedTag = ciphertext.copyOfRange(ciphertext.size - GCM_TAG_LENGTH, ciphertext.size)

            val plaintext = ByteArray(actualCiphertext.size)

            memScoped {
                val cryptor = initCryptor(kCCDecrypt, key)
                var status = kCCSuccess

                val cryptorRef = cryptor.value!!

                try {
                    status =
                        iv.usePinned { ivPinned ->
                            my_CCCryptorGCMAddIV(cryptorRef, ivPinned.addressOf(0), GCM_IV_LENGTH_U)
                        }
                    if (status != kCCSuccess) error("my_CCCryptorGCMAddIV failed: $status")

                    if (actualCiphertext.isNotEmpty()) {
                        status =
                            actualCiphertext.usePinned { ctPinned ->
                                plaintext.usePinned { ptPinned ->
                                    CCCryptorUpdate(
                                        cryptorRef,
                                        ctPinned.addressOf(0),
                                        actualCiphertext.size.convert(),
                                        ptPinned.addressOf(0),
                                        plaintext.size.convert(),
                                        alloc<platform.posix.size_tVar>().ptr,
                                    )
                                }
                            }
                        if (status != kCCSuccess) error("CCCryptorUpdate failed: $status")
                    }

                    val tagOut = ByteArray(GCM_TAG_LENGTH)
                    status =
                        tagOut.usePinned { tagOutPinned ->
                            val tagLenVar = alloc<platform.posix.size_tVar>()
                            tagLenVar.value = GCM_TAG_LENGTH_U
                            my_CCCryptorGCMFinal(cryptorRef, tagOutPinned.addressOf(0), tagLenVar.ptr)
                        }
                    if (status != kCCSuccess) error("my_CCCryptorGCMFinal failed: $status")

                    // Compare tags
                    var tagMatches = true
                    for (i in 0 until GCM_TAG_LENGTH) {
                        if (tagOut[i] != expectedTag[i]) tagMatches = false
                    }
                    if (!tagMatches) {
                        error("Authentication failed (tag mismatch)")
                    }
                } finally {
                    CCCryptorRelease(cryptorRef)
                }
            }

            plaintext
        }

    /**
     * Encrypts a string into a Base64 encoded format using Argon2 key derivation and AES-GCM.
     *
     * @param data The plaintext string to encrypt.
     * @param password The user-provided password used for key derivation.
     * @return The Base64 encoded payload containing salt, IV, ciphertext, and tag.
     */
    @OptIn(ExperimentalEncodingApi::class)
    actual suspend fun encrypt(
        data: String,
        password: String,
    ): String =
        withContext(Dispatchers.Default) {
            val salt = ByteArray(GCM_TAG_LENGTH)
            salt.usePinned { saltPinned ->
                SecRandomCopyBytes(kSecRandomDefault, GCM_TAG_LENGTH_U, saltPinned.addressOf(0))
            }

            val key = deriveKeyArgon2(password, salt)
            val ivAndCiphertextAndTag = encryptAesGcm(data.encodeToByteArray(), key)

            val payload = salt + ivAndCiphertextAndTag
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
                if (payload.size < ARGON2_SALT_LEN + GCM_IV_LENGTH + GCM_TAG_LENGTH) return@withContext ""

                val salt = payload.copyOfRange(0, GCM_TAG_LENGTH)
                val ivAndCiphertextAndTag = payload.copyOfRange(ARGON2_SALT_LEN, payload.size)

                val key = deriveKeyArgon2(password, salt)
                val plaintext = decryptAesGcm(ivAndCiphertextAndTag, key)

                plaintext.decodeToString()
            } catch (ignored: Exception) {
                println(ignored.message)
                ""
            }
        }
}
