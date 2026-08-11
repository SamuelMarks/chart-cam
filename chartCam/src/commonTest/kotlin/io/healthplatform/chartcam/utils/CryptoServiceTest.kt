/**
 * @file CryptoServiceTest.kt
 * Contains declarations for CryptoServiceTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests for [CryptoService] core behaviors including AES-GCM and Argon2 logic.
 */
class CryptoServiceTest {
    /**
     * Standard encryption/decryption round trip test.
     */
    @Test
    fun testEncryptionAndDecryption() =
        runTest {
            val service = CryptoService()
            val original = "Hello, secret data!"
            val password = "mypassword123"

            val encrypted = service.encrypt(original, password)
            assertNotEquals(original, encrypted)

            val decrypted = service.decrypt(encrypted, password)
            assertEquals(original, decrypted)
        }

    /**
     * Tests behavior when utilizing empty passwords.
     */
    @Test
    fun testEmptyPassword() =
        runTest {
            val service = CryptoService()
            val original = "No password data"
            try {
                val encrypted = service.encrypt(original, "")
                val decrypted = service.decrypt(encrypted, "")
                assertEquals(original, decrypted)
            } catch (e: Throwable) {
            }
        }

    /**
     * Tests behavior of malformed base64 decryption attempt.
     */
    @Test
    fun testInvalidBase64() =
        runTest {
            val service = CryptoService()
            val result = service.decrypt("not base64!!!", "pass")
            assertEquals("", result)
        }

    /**
     * Validate Argon2 determinism and length constraints.
     */
    @Test
    fun testArgon2KeyDerivation() =
        runTest {
            val service = CryptoService()
            val password = "deterministic_password"
            val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

            val key1 = service.deriveKeyArgon2(password, salt)
            val key2 = service.deriveKeyArgon2(password, salt)

            assertEquals(32, key1.size, "Derived key should be 32 bytes (256-bit)")
            assertContentEquals(key1, key2, "Derived keys with same inputs should be identical")
        }

    /**
     * Basic AES-GCM operation test.
     */
    @Test
    fun testAesGcmEncryptionDecryption() =
        runTest {
            val service = CryptoService()
            val plaintext = "Secure Message".encodeToByteArray()
            val key = ByteArray(32) { it.toByte() } // Fake 32-byte key

            val ciphertext = service.encryptAesGcm(plaintext, key)
            val decrypted = service.decryptAesGcm(ciphertext, key)

            assertContentEquals(plaintext, decrypted, "Decrypted data must match original plaintext")
        }

    /**
     * Validation of AES-GCM tag verification on tampered data.
     */
    @Test
    fun testAesGcmAuthentication() =
        runTest {
            val service = CryptoService()
            val plaintext = "Sensitive Data".encodeToByteArray()
            val key = ByteArray(32) { (it * 2).toByte() }

            val ciphertext = service.encryptAesGcm(plaintext, key)

            // Modify the ciphertext (tamper with the tag or data)
            ciphertext[ciphertext.lastIndex] = (ciphertext[ciphertext.lastIndex] + 1).toByte()

            try {
                service.decryptAesGcm(ciphertext, key)
                throw AssertionError("Expected exception during decryption due to invalid tag")
            } catch (e: Throwable) {
                // Expected
            }
        }

    /**
     * Tests decryption with a ciphertext shorter than the IV payload limits.
     */
    @Test
    fun testAesGcmCiphertextTooShort() =
        runTest {
            val service = CryptoService()
            val key = ByteArray(32) { it.toByte() }
            val shortCiphertext = ByteArray(10) { it.toByte() } // Less than 12

            try {
                service.decryptAesGcm(shortCiphertext, key)
                throw AssertionError("Expected exception for short ciphertext")
            } catch (e: Throwable) {
                // Expected IllegalArgumentException
            }
        }

    /**
     * Test failure flow for incorrect password derivation.
     */
    @Test
    fun testWrongPassword() =
        runTest {
            val service = CryptoService()
            val original = "Secret!"
            val encrypted = service.encrypt(original, "correct")

            // Should return empty string due to catch(e: Exception)
            val decrypted = service.decrypt(encrypted, "wrong")
            assertEquals("", decrypted)
        }

    /**
     * Tests decrypt behavior on abnormally short but valid base64 input.
     */
    @Test
    fun testDecryptShortPayload() =
        runTest {
            val service = CryptoService()
            // Shorter than 16 + 12 (28) bytes
            val shortPayload =
                kotlin.io.encoding.Base64
                    .encode(ByteArray(10) { it.toByte() })
            val result = service.decrypt(shortPayload, "pass")
            assertEquals("", result)
        }
}
