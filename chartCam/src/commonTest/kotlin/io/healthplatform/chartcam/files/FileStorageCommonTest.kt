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

        /**
         * Delete an image from the map.
         * @param path The path.
         * @return A Result indicating success or failure.
         */
        override fun deleteImage(path: String): Result<Unit> =
            if (storage.remove(path) != null) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("File not found: $path"))
            }

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

    /**
     * Tests deleteImage in FileStorage.
     */
    @Test
    fun testFileStorageDeleteImage() {
        val fs = MockFileStorage()
        val path = fs.saveImage("test.jpg", byteArrayOf(1, 2, 3))
        kotlin.test.assertTrue(fs.deleteImage(path).isSuccess)
        kotlin.test.assertTrue(fs.deleteImage(path).isFailure)
    }

    /**
     * Tests saveImageCatching and readImageCatching extension functions.
     */
    @Test
    fun testCatchingExtensions() {
        val fs = MockFileStorage()
        val payload = byteArrayOf(10, 20, 30)

        val saveResult = fs.saveImageCatching("catching.jpg", payload)
        kotlin.test.assertTrue(saveResult.isSuccess)
        assertEquals("catching.jpg", saveResult.getOrNull())

        val readResult = fs.readImageCatching("catching.jpg")
        kotlin.test.assertTrue(readResult.isSuccess)
        assertContentEquals(payload, readResult.getOrNull())
    }
}
