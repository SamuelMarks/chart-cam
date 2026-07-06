/**
 * Contains unit tests validating the [CryptoService] functionality.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Validates the core logic of [CryptoService], including text encryption,
 * decryption, handling of empty passwords, and invalid Base64 decoding.
 */
class CryptoServiceTest {
    /**
     * Tests that data can be correctly encrypted and then decrypted back to its original value.
     */
    @Test
    fun testEncryptionAndDecryption() {
        val service = CryptoService()
        val original = "Hello, secret data!"
        val password = "mypassword123"

        val encrypted = service.encrypt(original, password)
        assertNotEquals(original, encrypted)

        val decrypted = service.decrypt(encrypted, password)
        assertEquals(original, decrypted)
    }

    /**
     * Tests that encryption and decryption handle empty passwords correctly.
     */
    @Test
    fun testEmptyPassword() {
        val service = CryptoService()
        val original = "No password data"
        val encrypted = service.encrypt(original, "")
        val decrypted = service.decrypt(encrypted, "")
        assertEquals(original, decrypted)
    }

    /**
     * Tests that decryption of invalid Base64 data safely returns an empty string without crashing.
     */
    @Test
    fun testInvalidBase64() {
        val service = CryptoService()
        val result = service.decrypt("not base64!!!", "pass")
        assertEquals("", result)
    }
}
