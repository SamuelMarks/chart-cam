/**
 * @file AndroidCryptoServiceTest.kt
 * Contains declarations for AndroidCryptoServiceTest.kt.
 */
package io.healthplatform.chartcam.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidCryptoServiceTest {
    @Test
    fun testCryptoService() =
        runBlocking {
            val service = CryptoService()
            val original = "Test string"
            val password = "secret_password"

            val encrypted = service.encrypt(original, password)
            assertNotEquals(original, encrypted)

            val decrypted = service.decrypt(encrypted, password)
            assertEquals(original, decrypted)

            val failed = service.decrypt(encrypted, "wrong")
            assertEquals("", failed)
        }
}
