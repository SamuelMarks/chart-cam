/**
 * @file SecureStorageIosTest.kt
 * Contains declarations for SecureStorageIosTest.kt.
 */
package io.healthplatform.chartcam.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Validates baseline iOS secure storage operations with the iOS Keychain.
 */
class SecureStorageIosTest {
    /**
     * Verifies saving, retrieving, and deleting a secure value via IosSecureStorage.
     */
    @Test
    fun testSecureStorageLifecycle() {
        val storage = createSecureStorage()
        val key = "test_clinical_key"
        val value = "secret_session_token_123"

        storage.save(key, value)
        val retrieved = storage.getString(key)
        assertEquals(value, retrieved)

        storage.delete(key)
        val afterDelete = storage.getString(key)
        assertNull(afterDelete)
    }
}
