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
import platform.posix.size_t
import platform.posix.size_tVar
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Service providing cryptographic operations on the iOS platform using CommonCrypto and argon2 C-interop.
 */
@OptIn(ExperimentalForeignApi::class)
actual class CryptoService actual constructor() {
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
            val key = ByteArray(32)

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
                throw IllegalStateException("Argon2 key derivation failed with error code: $result")
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
            val iv = ByteArray(12)
            iv.usePinned { ivPinned ->
                SecRandomCopyBytes(kSecRandomDefault, 12u, ivPinned.addressOf(0))
            }

            val ciphertext = ByteArray(plaintext.size)
            val tag = ByteArray(16)

            memScoped {
                val cryptor = alloc<CCCryptorRefVar>()

                var status =
                    key.usePinned { keyPinned ->
                        CCCryptorCreateWithMode(
                            kCCEncrypt,
                            kCCModeGCM.convert(),
                            kCCAlgorithmAES,
                            ccNoPadding,
                            null, // IV is added later
                            keyPinned.addressOf(0),
                            key.size.convert(),
                            null,
                            0u,
                            0,
                            0u,
                            cryptor.ptr,
                        )
                    }

                if (status != kCCSuccess) throw IllegalStateException("CCCryptorCreateWithMode failed: $status")

                val cryptorRef = cryptor.value!!

                try {
                    status =
                        iv.usePinned { ivPinned ->
                            CCCryptorGCMAddIV(cryptorRef, ivPinned.addressOf(0), 12u)
                        }
                    if (status != kCCSuccess) throw IllegalStateException("CCCryptorGCMAddIV failed: $status")

                    var moved: size_t = 0u
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
                                        alloc<platform.posix.size_tVar>().ptr, // Discard moved for now, since ccNoPadding
                                    )
                                }
                            }
                        if (status != kCCSuccess) throw IllegalStateException("CCCryptorUpdate failed: $status")
                    }

                    status =
                        tag.usePinned { tagPinned ->
                            val tagLenVar = alloc<platform.posix.size_tVar>()
                            tagLenVar.value = 16u
                            CCCryptorGCMFinal(cryptorRef, tagPinned.addressOf(0), tagLenVar.ptr)
                        }
                    if (status != kCCSuccess) throw IllegalStateException("CCCryptorGCMFinal failed: $status")
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
            if (ciphertext.size < 12 + 16) throw IllegalArgumentException("Ciphertext too short")

            val iv = ciphertext.copyOfRange(0, 12)
            val actualCiphertext = ciphertext.copyOfRange(12, ciphertext.size - 16)
            val expectedTag = ciphertext.copyOfRange(ciphertext.size - 16, ciphertext.size)

            val plaintext = ByteArray(actualCiphertext.size)

            memScoped {
                val cryptor = alloc<CCCryptorRefVar>()

                var status =
                    key.usePinned { keyPinned ->
                        CCCryptorCreateWithMode(
                            kCCDecrypt,
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

                if (status != kCCSuccess) throw IllegalStateException("CCCryptorCreateWithMode failed: $status")

                val cryptorRef = cryptor.value!!

                try {
                    status =
                        iv.usePinned { ivPinned ->
                            CCCryptorGCMAddIV(cryptorRef, ivPinned.addressOf(0), 12u)
                        }
                    if (status != kCCSuccess) throw IllegalStateException("CCCryptorGCMAddIV failed: $status")

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
                        if (status != kCCSuccess) throw IllegalStateException("CCCryptorUpdate failed: $status")
                    }

                    val tagOut = ByteArray(16)
                    status =
                        tagOut.usePinned { tagOutPinned ->
                            val tagLenVar = alloc<platform.posix.size_tVar>()
                            tagLenVar.value = 16u
                            CCCryptorGCMFinal(cryptorRef, tagOutPinned.addressOf(0), tagLenVar.ptr)
                        }
                    if (status != kCCSuccess) throw IllegalStateException("CCCryptorGCMFinal failed: $status")

                    // Compare tags
                    var tagMatches = true
                    for (i in 0 until 16) {
                        if (tagOut[i] != expectedTag[i]) tagMatches = false
                    }
                    if (!tagMatches) {
                        throw IllegalStateException("Authentication failed (tag mismatch)")
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
            val salt = ByteArray(16)
            salt.usePinned { saltPinned ->
                SecRandomCopyBytes(kSecRandomDefault, 16u, saltPinned.addressOf(0))
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
                if (payload.size < 16 + 12 + 16) return@withContext ""

                val salt = payload.copyOfRange(0, 16)
                val ivAndCiphertextAndTag = payload.copyOfRange(16, payload.size)

                val key = deriveKeyArgon2(password, salt)
                val plaintext = decryptAesGcm(ivAndCiphertextAndTag, key)

                plaintext.decodeToString()
            } catch (e: Exception) {
                ""
            }
        }
}
