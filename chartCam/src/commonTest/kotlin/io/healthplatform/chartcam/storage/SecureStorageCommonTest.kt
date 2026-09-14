/**
 * @file SecureStorageCommonTest.kt
 * Contains declarations for SecureStorageCommonTest.kt.
 */
package io.healthplatform.chartcam.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Common test assertions for the [SecureStorage] interface behavior.
 */
class SecureStorageCommonTest {
    /**
     * A mock storage for testing abstract logic dependent on [SecureStorage].
     */
    class MockSecureStorage : SecureStorage {
        /** In memory storage map. */
        val map = mutableMapOf<String, String>()

        /**
         * Save a value to map.
         * @param key The key.
         * @param value The value.
         */
        override fun save(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        /**
         * Read a value from map.
         * @param key The key.
         * @return The value or null.
         */
        override fun getString(key: String): String? = map[key]

        /**
         * Delete a value from map.
         * @param key The key.
         */
        override fun delete(key: String) {
            map.remove(key)
        }
    }

    /**
     * Test basic write, read, and delete operations of [SecureStorage].
     */
    @Test
    fun testSecureStorageInterface() {
        val storage = MockSecureStorage()

        assertNull(storage.getString("missing"))

        storage.save("key", "value")
        assertEquals("value", storage.getString("key"))

        storage.delete("key")
        assertNull(storage.getString("key"))
    }

    /**
     * Tests safe Result-returning extensions for SecureStorage.
     */
    @Test
    fun testSecureStorageCatching() {
        val storage = MockSecureStorage()

        val saveResult = storage.saveCatching("auth_token", "secret_123")
        kotlin.test.assertTrue(saveResult.isSuccess)

        val readResult = storage.getStringCatching("auth_token")
        kotlin.test.assertTrue(readResult.isSuccess)
        assertEquals("secret_123", readResult.getOrNull())

        val deleteResult = storage.deleteCatching("auth_token")
        kotlin.test.assertTrue(deleteResult.isSuccess)
        assertNull(storage.getString("auth_token"))
    }
}
