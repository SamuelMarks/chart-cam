/**
 * @file StorageJvmTest.kt
 * Contains declarations for StorageJvmTest.kt.
 */
package io.healthplatform.chartcam.storage

import io.healthplatform.chartcam.files.createFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test class for Storage operations on JVM.
 */
class StorageJvmTest {
    /**
     * Tests SecureStorage logic.
     */
    @Test
    fun testSecureStorage() {
        val storage = createSecureStorage()
        storage.save("test_key", "test_value")
        assertEquals("test_value", storage.getString("test_key"))
        storage.delete("test_key")
        assertNull(storage.getString("test_key"))
    }

    /**
     * Tests FileStorage logic.
     */
    @Test
    fun testFileStorage() {
        val storage = createFileStorage()
        val data = "hello world".encodeToByteArray()
        val path = storage.saveImage("test.png", data)
        val read = storage.readImage(path)
        assertTrue(data.contentEquals(read))
        storage.clearCache()
        // verify cleared by attempting read
        // clearCache is no-op on JVM
    }
}
