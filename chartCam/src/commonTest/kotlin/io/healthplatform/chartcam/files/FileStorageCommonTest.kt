/**
 * @file FileStorageCommonTest.kt
 * Contains declarations for FileStorageCommonTest.kt.
 */
package io.healthplatform.chartcam.files

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FileStorageCommonTest {
    class MockFileStorage : FileStorage {
        val storage = mutableMapOf<String, ByteArray>()

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            storage[fileName] = bytes
            return fileName
        }

        override fun readImage(path: String): ByteArray = storage[path] ?: ByteArray(0)

        override fun clearCache() {
            storage.clear()
        }
    }

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
