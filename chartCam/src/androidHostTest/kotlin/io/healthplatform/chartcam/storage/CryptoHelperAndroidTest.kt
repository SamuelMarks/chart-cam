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

        val encryptedCatching = CryptoHelper.encryptCatching(original).getOrThrow()
        val decryptedCatching = CryptoHelper.decryptCatching(encryptedCatching).getOrThrow()
        assertContentEquals(original, decryptedCatching)
    }

    /**
     * Verifies decryption failures on empty and truncated payloads.
     */
    @Test
    fun testDecryptInvalidPayloads() {
        val emptyResult = CryptoHelper.decryptCatching(byteArrayOf())
        kotlin.test.assertTrue(emptyResult.isFailure)

        val truncatedResult = CryptoHelper.decryptCatching(byteArrayOf(12, 1, 2, 3))
        kotlin.test.assertTrue(truncatedResult.isFailure)

        val invalidDataResult =
            CryptoHelper.decryptCatching(
                byteArrayOf(4, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21),
            )
        kotlin.test.assertTrue(invalidDataResult.isFailure)
    }
}
