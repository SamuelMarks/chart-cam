package io.healthplatform.chartcam.files

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileStorageJvmCoverageTest {
    @Test
    fun testSaveAndReadImage() {
        val storage = createFileStorage()
        val path = storage.saveImage("test.jpg", byteArrayOf(1, 2, 3))
        assertTrue(path.endsWith("test.jpg"))

        val bytes = storage.readImage(path)
        assertEquals(3, bytes.size)
    }

    @Test
    fun testReadNonExistent() {
        val storage = createFileStorage()
        val bytes = storage.readImage("does-not-exist.jpg")
        assertEquals(0, bytes.size)
        storage.clearCache() // No op
    }
}
