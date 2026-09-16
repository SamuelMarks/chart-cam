/**
 * @file CryptoHelperAndroidTest.kt
 * Contains declarations for CryptoHelperAndroidTest.kt.
 */
package io.healthplatform.chartcam.storage

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertContentEquals

/**
 * Android host tests for CryptoHelper.
 */
@Config(manifest = Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class CryptoHelperAndroidTest {
    /**
     * Verifies symmetric encryption and decryption on Android.
     */
    @Test
    fun testEncryptDecrypt() {
        val original = "Clinical Android KeyStore payload".encodeToByteArray()
        val encrypted = CryptoHelper.encrypt(original)
        val decrypted = CryptoHelper.decrypt(encrypted)
        assertContentEquals(original, decrypted)
    }
}
