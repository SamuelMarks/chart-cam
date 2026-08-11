/**
 * @file FileStorageJvmCoverageTest.kt
 * Contains declarations for FileStorageJvmCoverageTest.kt.
 */
package io.healthplatform.chartcam.files

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Coverage test class for FileStorage on JVM.
 */
class FileStorageJvmCoverageTest {
    /**
     * Tests saving and reading an image.
     */
    @Test
    fun testSaveAndReadImage() {
        val storage = createFileStorage()
        val path = storage.saveImage("test.jpg", byteArrayOf(1, 2, 3))
        assertTrue(path.endsWith("test.jpg"))

        val bytes = storage.readImage(path)
        assertEquals(3, bytes.size)
    }

    /**
     * Tests reading a non-existent image.
     */
    @Test
    fun testReadNonExistent() {
        val storage = createFileStorage()
        val bytes = storage.readImage("does-not-exist.jpg")
        assertEquals(0, bytes.size)
        storage.clearCache() // No op
    }
}
