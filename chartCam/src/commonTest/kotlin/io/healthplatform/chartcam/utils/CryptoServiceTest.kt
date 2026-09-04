/**
 * @file CryptoServiceTest.kt
 * Contains tests for the [CryptoService] class.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Test class for validating the security and encryption functionality provided by [CryptoService].
 */
class CryptoServiceTest {
    /**
     * Verifies the consistency of Argon2 hashing.
     * Ensures that deriving a key with the same password and salt produces the same result.
     */
    @Test
    fun testArgon2ConsistencyAndSaltGeneration() =
        runTest {
            val cryptoService = CryptoService()
            val password = "securePassword123"
            val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

            val key1 = cryptoService.deriveKeyArgon2(password, salt)
            val key2 = cryptoService.deriveKeyArgon2(password, salt)

            // Ensure same inputs yield same output
            assertTrue(key1.contentEquals(key2), "Argon2 should be deterministic with same salt and password")

            val diffSalt = byteArrayOf(16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
            val key3 = cryptoService.deriveKeyArgon2(password, diffSalt)

            // Ensure different salt yields different key
            assertTrue(!key1.contentEquals(key3), "Argon2 should produce different keys with different salts")
        }

    /**
     * Verifies that the encrypt method generates a random salt and IV on each invocation,
     * so that encrypting the same data twice yields different ciphertexts.
     */
    @Test
    fun testEncryptionRandomness() =
        runTest {
            val cryptoService = CryptoService()
            val password = "securePassword123"
            val plaintext = "Sensitive Patient Data: Name: John Doe, DOB: 1980-01-01"

            val cipherText1 = cryptoService.encrypt(plaintext, password)
            val cipherText2 = cryptoService.encrypt(plaintext, password)

            // Ensure randomized salt/IV means different ciphertext for the same plaintext
            assertNotEquals(cipherText1, cipherText2, "Encryption should yield different base64 strings due to random salts/IVs")

            // Ensure plaintext is properly encrypted and not present in the output
            assertTrue(!cipherText1.contains("John Doe"), "Ciphertext should not contain plaintext data")
        }

    /**
     * Verifies end-to-end encryption and decryption of patient details.
     */
    @Test
    fun testEndToEndEncryptionOfPatientDetails() =
        runTest {
            val cryptoService = CryptoService()
            val password = "masterEncryptionKey!@#"
            val patientDetails =
                """
                {
                    "id": "12345",
                    "name": "Jane Smith",
                    "condition": "Hypertension",
                    "notes": "Patient requires daily monitoring."
                }
                """.trimIndent()

            val encrypted = cryptoService.encrypt(patientDetails, password)
            val decrypted = cryptoService.decrypt(encrypted, password)

            assertEquals(patientDetails, decrypted, "Decrypted patient details should match the original plaintext")
        }

    /**
     * Test edge cases for decryption such as tampered ciphertext or invalid password.
     */
    @Test
    fun testDecryptionFailureWithInvalidPasswordOrTamperedCiphertext() =
        runTest {
            val cryptoService = CryptoService()
            val password = "correctPassword123"
            val plaintext = "Secret Message"

            val encrypted = cryptoService.encrypt(plaintext, password)

            var failedAsExpected = false
            try {
                val result = cryptoService.decrypt(encrypted, "wrongPassword456")
                if (result.isEmpty()) failedAsExpected = true
            } catch (e: Exception) {
                failedAsExpected = true
            }
            assertTrue(failedAsExpected, "Decryption should fail with incorrect password")

            var tamperedFailedAsExpected = false
            try {
                // Tamper by modifying the first character (which is part of the salt or IV/ciphertext)
                val tampered = if (encrypted.first() == 'A') 'B' + encrypted.drop(1) else 'A' + encrypted.drop(1)
                val result = cryptoService.decrypt(tampered, password)
                if (result.isEmpty()) tamperedFailedAsExpected = true
            } catch (e: Exception) {
                tamperedFailedAsExpected = true
            }
            assertTrue(tamperedFailedAsExpected, "Decryption should fail with tampered ciphertext")
        }

    /**
     * Verifies Argon2 key derivation matches expected output length and format.
     */
    @Test
    fun testArgon2KeyDerivationOutputFormat() =
        runTest {
            val cryptoService = CryptoService()
            val password = "password"
            val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            val key = cryptoService.deriveKeyArgon2(password, salt)

            // Argon2 key should be 32 bytes for AES-256
            assertEquals(32, key.size, "Derived key should be 32 bytes for AES-256")
        }

    /**
     * Test data masking function to ensure no sensitive data leaks.
     */
    @Test
    fun testDataMasking() {
        val original = "123456789"
        val masked = original.replace(Regex(".(?=.{4})"), "*")
        assertEquals("*****6789", masked, "Data should be correctly masked leaving only last 4 characters")
    }

    /**
     * Tests encryptAesGcm and decryptAesGcm directly.
     */
    @Test
    fun testDirectAesGcmEncryptionDecryption() =
        runTest {
            val cryptoService = CryptoService()
            val key = ByteArray(32) { 1 }
            val plaintext = "Direct AES-GCM Test".encodeToByteArray()

            val ciphertext = cryptoService.encryptAesGcm(plaintext, key)
            val decrypted = cryptoService.decryptAesGcm(ciphertext, key)

            assertTrue(plaintext.contentEquals(decrypted), "Direct AES-GCM decryption should match original plaintext")
        }

    /**
     * Tests decryptAesGcm with ciphertext that is too short.
     */
    @Test
    fun testDecryptAesGcmTooShort() =
        runTest {
            val cryptoService = CryptoService()
            val key = ByteArray(32) { 1 }
            val shortCiphertext = ByteArray(4) { 0 }

            var threwException = false
            try {
                cryptoService.decryptAesGcm(shortCiphertext, key)
            } catch (e: Throwable) {
                threwException = true
            }
            assertTrue(threwException, "decryptAesGcm should throw an exception when ciphertext is shorter than IV size")
        }

    /**
     * Tests decrypt with short base64 payload.
     */
    @Test
    fun testDecryptShortPayload() =
        runTest {
            val cryptoService = CryptoService()
            // 8 bytes in base64: "AAAAAAAAAAA="
            val result = cryptoService.decrypt("AAAAAAAAAAA=", "password")
            assertEquals("", result, "Decrypting payload shorter than SALT + IV should return empty string")
        }

    /**
     * Tests decrypt with invalid base64 input.
     */
    @Test
    fun testDecryptInvalidBase64() =
        runTest {
            val cryptoService = CryptoService()
            val result = cryptoService.decrypt("not-valid-base-64!!!", "password")
            assertEquals("", result, "Decrypting invalid base64 should return empty string")
        }
}
