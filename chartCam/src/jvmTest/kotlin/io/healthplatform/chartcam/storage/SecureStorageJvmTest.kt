/**
 * @file SecureStorageJvmTest.kt
 * Contains declarations for SecureStorageJvmTest.kt.
 */
package io.healthplatform.chartcam.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test class for SecureStorage and KeystoreHardwareProvider on JVM.
 */
class SecureStorageJvmTest {
    /**
     * Verifies SecureStorage persistence, reading, and deletion on JVM.
     */
    @Test
    fun testSecureStorageJvm() {
        val storage = createSecureStorage()
        val testKey = "test_clinical_key"
        val testValue = "encrypted_secret_data"

        storage.save(testKey, testValue)
        assertEquals(testValue, storage.getString(testKey))

        storage.delete(testKey)
        assertNull(storage.getString(testKey))
    }

    /**
     * Verifies JvmKeystoreHardwareProvider correctly reports lack of mobile hardware keystore.
     */
    @Test
    fun testJvmKeystoreHardwareProvider() {
        val provider = createKeystoreHardwareProvider()
        assertEquals(BiometricHardwareStatus.NO_HARDWARE, provider.getHardwareStatus())

        val hardwareCheck = provider.checkHardwareBacked()
        assertTrue(hardwareCheck.isFailure)
    }
}
