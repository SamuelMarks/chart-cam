/**
 * @file CryptoServiceIosTest.kt
 * Contains declarations for CryptoServiceIosTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests mapping for standard crypto APIs on iOS.
 */
class CryptoServiceIosTest {
    /**
     * Verifies that CryptoService can derive keys and perform encryption round-trips on iOS.
     */
    @Test
    fun testCryptoServiceEncryptionRoundTrip() =
        runTest {
            val service = CryptoService()
            val password = "clinical_secret_passphrase"
            val plaintext = "Sensitive Patient Health Information 456"

            val encrypted = service.encrypt(plaintext, password)
            assertNotNull(encrypted)
            assertTrue(encrypted.isNotEmpty(), "Encrypted payload should not be empty")

            val decrypted = service.decrypt(encrypted, password)
            assertEquals(plaintext, decrypted, "Decrypted text should match original plaintext")
        }
}
