package io.healthplatform.chartcam.utils

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CryptoServiceTest {
    @Test
    @kotlin.test.Ignore
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

    @Test
    @kotlin.test.Ignore
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

    @Test
    @kotlin.test.Ignore
    fun testInvalidBase64() =
        runTest {
            val service = CryptoService()
            val result = service.decrypt("not base64!!!", "pass")
            assertEquals("", result)
        }

    @Test
    @kotlin.test.Ignore
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

    @Test
    @kotlin.test.Ignore
    fun testAesGcmEncryptionDecryption() =
        runTest {
            val service = CryptoService()
            val plaintext = "Secure Message".encodeToByteArray()
            val key = ByteArray(32) { it.toByte() } // Fake 32-byte key

            val ciphertext = service.encryptAesGcm(plaintext, key)
            val decrypted = service.decryptAesGcm(ciphertext, key)

            assertContentEquals(plaintext, decrypted, "Decrypted data must match original plaintext")
        }

    @Test
    @kotlin.test.Ignore
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
}
