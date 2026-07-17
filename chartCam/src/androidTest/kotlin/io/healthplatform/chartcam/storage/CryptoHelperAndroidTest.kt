/**
 * @file CryptoHelperAndroidTest.kt
 * Contains declarations for CryptoHelperAndroidTest.kt.
 */
package io.healthplatform.chartcam.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContentEquals

/**
 * Tests for [CryptoHelper].
 */
@RunWith(AndroidJUnit4::class)
class CryptoHelperAndroidTest {
    /**
     * Test encrypt and decrypt.
     */
    @Test
    fun testEncryptDecrypt() {
        val original = "Sensitive Data".toByteArray()
        val encrypted = CryptoHelper.encrypt(original)
        val decrypted = CryptoHelper.decrypt(encrypted)

        assertContentEquals(original, decrypted)
    }
}
