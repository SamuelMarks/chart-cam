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
}
