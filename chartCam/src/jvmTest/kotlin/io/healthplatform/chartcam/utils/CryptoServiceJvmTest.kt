/**
 * @file CryptoServiceJvmTest.kt
 * Contains declarations for CryptoServiceJvmTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test class for CryptoService on JVM.
 */
class CryptoServiceJvmTest {
    /**
     * Test crypto service encryption and decryption round trip on JVM.
     */
    @Test
    fun testCryptoServiceJvm() =
        runTest {
            val crypto = CryptoService()
            val plainText = "Patient sensitive medical history"
            val password = "StrongClinicalPassword123!"

            val encrypted = crypto.encrypt(plainText, password)
            assertTrue(encrypted.isNotBlank())

            val decrypted = crypto.decrypt(encrypted, password)
            assertEquals(plainText, decrypted)
        }
}
