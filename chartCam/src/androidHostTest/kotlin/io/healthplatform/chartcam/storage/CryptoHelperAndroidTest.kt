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

    /**
     * Verifies KeyStore key creation and retrieval branches in non-robolectric environment.
     */
    @Test
    fun testKeyStoreCreationAndRetrievalBranches() {
        val keyGen = javax.crypto.KeyGenerator.getInstance("AES")
        keyGen.init(128)
        val generatedSecretKey = keyGen.generateKey()

        val mockKeyStore = org.mockito.Mockito.mock(java.security.KeyStore::class.java)
        val mockKeyGen = org.mockito.Mockito.mock(javax.crypto.KeyGenerator::class.java)
        org.mockito.Mockito
            .`when`(mockKeyGen.generateKey())
            .thenReturn(generatedSecretKey)

        // First call: keyStore.getKey returns null, triggers keyGenerator.generateKey()
        org.mockito.Mockito
            .`when`(mockKeyStore.getKey("ChartCamKeyAlias", null))
            .thenReturn(null)

        val oldFp = CryptoHelper.buildFingerprintProvider
        val oldKs = CryptoHelper.keyStoreProvider
        val oldKg = CryptoHelper.keyGeneratorProvider

        // allow-exception
        try {
            CryptoHelper.buildFingerprintProvider = { "pixel_device" }
            CryptoHelper.keyStoreProvider = { mockKeyStore }
            CryptoHelper.keyGeneratorProvider = { _, _ -> mockKeyGen }

            val original = "KeyStore Test Data".encodeToByteArray()
            val encrypted = CryptoHelper.encrypt(original)
            kotlin.test.assertNotNull(encrypted)

            // Second call: keyStore.getKey returns existing SecretKey
            org.mockito.Mockito
                .`when`(mockKeyStore.getKey("ChartCamKeyAlias", null))
                .thenReturn(generatedSecretKey)
            val decrypted = CryptoHelper.decrypt(encrypted)
            assertContentEquals(original, decrypted)

            // Third call: keyStore.getKey returns a non-SecretKey (e.g. PrivateKey), exercising the (as? SecretKey) null branch
            val mockNonSecretKey = org.mockito.Mockito.mock(java.security.PrivateKey::class.java)
            org.mockito.Mockito
                .`when`(mockKeyStore.getKey("ChartCamKeyAlias", null))
                .thenReturn(mockNonSecretKey)
            val encryptedWithNonSecretKeyReturn = CryptoHelper.encrypt(original)
            kotlin.test.assertNotNull(encryptedWithNonSecretKeyReturn)
        } finally {
            CryptoHelper.buildFingerprintProvider = oldFp
            CryptoHelper.keyStoreProvider = oldKs
            CryptoHelper.keyGeneratorProvider = oldKg
        }

        // Exercise default keyGeneratorProvider lambda
        runCatching {
            CryptoHelper.keyGeneratorProvider.invoke("AES", "BC")
        }
    }

    /**
     * Verifies cached robolectric key branch.
     */
    @Test
    fun testCachedRobolectricKey() {
        val original = "Cached key test".encodeToByteArray()
        val enc1 = CryptoHelper.encrypt(original)
        val enc2 = CryptoHelper.encrypt(original)
        kotlin.test.assertNotNull(enc1)
        kotlin.test.assertNotNull(enc2)
    }
}
