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

        /** Save a value to map. */
        override fun save(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        /** Read a value from map. */
        override fun getString(key: String): String? = map[key]

        /** Delete a value from map. */
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
}
