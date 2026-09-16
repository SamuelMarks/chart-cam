/**
 * @file IosSecureStorageTest.kt
 * Contains declarations for IosSecureStorageTest.kt.
 */
package io.healthplatform.chartcam.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Validates iOS secure storage component key update and overwrite behaviors.
 */
class IosSecureStorageTest {
    /**
     * Verifies updating existing keys and querying missing keys.
     */
    @Test
    fun testSecureStorageOverwrite() {
        val storage = IosSecureStorage()
        val key = "overwrite_key"

        // Non-existent key should return null
        assertNull(storage.getString("non_existent_key_999"))

        // Save initial
        storage.save(key, "v1")
        assertEquals("v1", storage.getString(key))

        // Overwrite
        storage.save(key, "v2")
        assertEquals("v2", storage.getString(key))

        // Cleanup
        storage.delete(key)
        assertNull(storage.getString(key))
    }
}
