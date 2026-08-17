/**
 * @file FileStorageCommonTest.kt
 * Contains declarations for FileStorageCommonTest.kt.
 */
package io.healthplatform.chartcam.files

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Common tests for the FileStorage interface.
 */
class FileStorageCommonTest {
    /**
     * Mock implementation of FileStorage.
     */
    class MockFileStorage : FileStorage {
        /** Internal storage map. */
        val storage = mutableMapOf<String, ByteArray>()

        /**
         * Save an image to the map.
         * @param fileName The name.
         * @param bytes The bytes.
         * @return The path.
         */
        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            storage[fileName] = bytes
            return fileName
        }

        /**
         * Read an image from the map.
         * @param path The path.
         * @return The bytes.
         */
        override fun readImage(path: String): ByteArray = storage[path] ?: ByteArray(0)

        /** Clear cache. */
        override fun clearCache() {
            storage.clear()
        }
    }

    /**
     * Test basic file storage operations using the mock.
     */
    @Test
    fun testFileStorageOperations() {
        val fs = MockFileStorage()
        val data = byteArrayOf(1, 2, 3)

        val path = fs.saveImage("test.jpg", data)
        assertEquals("test.jpg", path)

        val readData = fs.readImage(path)
        assertContentEquals(data, readData)

        fs.clearCache()
        val emptyData = fs.readImage(path)
        assertEquals(0, emptyData.size)
    }
}
