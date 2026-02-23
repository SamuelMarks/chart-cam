package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CryptoServiceTest {

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

    @Test
    fun testEmptyPassword() {
        val service = CryptoService()
        val original = "No password data"
        val encrypted = service.encrypt(original, "")
        val decrypted = service.decrypt(encrypted, "")
        assertEquals(original, decrypted)
    }

    @Test
    fun testInvalidBase64() {
        val service = CryptoService()
        val result = service.decrypt("not base64!!!", "pass")
        assertEquals("", result)
    }
}
