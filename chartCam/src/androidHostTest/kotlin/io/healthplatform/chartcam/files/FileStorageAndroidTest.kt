package io.healthplatform.chartcam.files

import io.healthplatform.chartcam.AndroidAppInit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class FileStorageAndroidTest {
    private lateinit var fileStorage: FileStorage

    @Before
    fun setup() {
        AndroidAppInit.init(RuntimeEnvironment.getApplication())
        fileStorage = AndroidFileStorage()
    }

    @After
    fun tearDown() {
        fileStorage.clearCache()
    }

    @Test
    fun testSaveAndReadImage() {
        val fileName = "test_image.jpg"
        val testBytes = byteArrayOf(1, 2, 3, 4, 5)

        // Save
        val path = fileStorage.saveImage(fileName, testBytes)
        assertTrue(path.endsWith(fileName))

        // Read
        val readBytes = fileStorage.readImage(path)
        assertTrue(testBytes.contentEquals(readBytes))
    }

    @Test
    fun testReadNonExistentImage() {
        val path = RuntimeEnvironment.getApplication().cacheDir.absolutePath + "/non_existent.jpg"
        val bytes = fileStorage.readImage(path)
        assertEquals(0, bytes.size)
    }

    @Test
    fun testClearCache() {
        val fileName = "temp_image.jpg"
        val path = fileStorage.saveImage(fileName, byteArrayOf(1, 2, 3))

        fileStorage.clearCache()

        val bytes = fileStorage.readImage(path)
        assertEquals(0, bytes.size)
    }

    @Test
    fun testCreateFileStorageFactory() {
        val storage = createFileStorage()
        assertTrue(storage is AndroidFileStorage)
    }
}
