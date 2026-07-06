package io.healthplatform.chartcam.files

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileStorageJvmTest {
    private lateinit var fileStorage: FileStorage
    private val tempDir = System.getProperty("java.io.tmpdir")

    @Before
    fun setup() {
        fileStorage = JvmFileStorage()
    }

    @After
    fun tearDown() {
        // Clean up test files
        File(tempDir, "test_jvm_image.jpg").delete()
    }

    @Test
    fun testSaveAndReadImage() {
        val fileName = "test_jvm_image.jpg"
        val testBytes = byteArrayOf(10, 20, 30, 40)

        // Save
        val path = fileStorage.saveImage(fileName, testBytes)
        assertTrue(path.endsWith(fileName))

        // Read
        val readBytes = fileStorage.readImage(path)
        assertTrue(testBytes.contentEquals(readBytes))
    }

    @Test
    fun testReadNonExistentImage() {
        val path = File(tempDir, "non_existent_jvm.jpg").absolutePath
        val bytes = fileStorage.readImage(path)
        assertEquals(0, bytes.size)
    }

    @Test
    fun testClearCache() {
        // Clear cache is a no-op on JVM. We just test it doesn't crash.
        fileStorage.clearCache()
    }

    @Test
    fun testCreateFileStorageFactory() {
        val storage = createFileStorage()
        assertTrue(storage is JvmFileStorage)
    }
}
