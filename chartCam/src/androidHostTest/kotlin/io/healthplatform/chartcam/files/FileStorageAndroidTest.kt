/**
 * @file FileStorageAndroidTest.kt
 * Contains declarations for FileStorageAndroidTest.kt.
 */
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

/**
 * Android host tests for FileStorage.
 */
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class FileStorageAndroidTest {
    private lateinit var fileStorage: FileStorage

    /**
     * Setup for tests.
     */
    @Before
    fun setup() {
        AndroidAppInit.init(RuntimeEnvironment.getApplication())
        fileStorage = AndroidFileStorage()
    }

    /**
     * Teardown after tests.
     */
    @After
    fun tearDown() {
        fileStorage.clearCache()
    }

    /**
     * Test saving and reading an image.
     */
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

    /**
     * Test reading a non-existent image.
     */
    @Test
    fun testReadNonExistentImage() {
        val path = RuntimeEnvironment.getApplication().cacheDir.absolutePath + "/non_existent.jpg"
        val bytes = fileStorage.readImage(path)
        assertEquals(0, bytes.size)
    }

    /**
     * Test clearing the cache.
     */
    @Test
    fun testClearCache() {
        val fileName = "temp_image.jpg"
        val path = fileStorage.saveImage(fileName, byteArrayOf(1, 2, 3))

        fileStorage.clearCache()

        val bytes = fileStorage.readImage(path)
        assertEquals(0, bytes.size)
    }

    /**
     * Test factory creates AndroidFileStorage.
     */
    @Test
    fun testCreateFileStorageFactory() {
        val storage = createFileStorage()
        assertTrue(storage is AndroidFileStorage)
    }

    /**
     * Test saving an image overwrites an existing image with the same name.
     */
    @Test
    fun testSaveImageOverwritesExisting() {
        val fileName = "overwrite_image.jpg"
        val testBytes1 = byteArrayOf(1, 2, 3)
        val testBytes2 = byteArrayOf(4, 5, 6)

        fileStorage.saveImage(fileName, testBytes1)
        val path = fileStorage.saveImage(fileName, testBytes2)

        val readBytes = fileStorage.readImage(path)
        assertTrue(testBytes2.contentEquals(readBytes))
    }

    /**
     * Test clear cache deletes files.
     */
    @Test
    fun testClearCacheDeletesFiles() {
        val fileName1 = "temp_image1.jpg"
        val fileName2 = "temp_image2.jpg"
        fileStorage.saveImage(fileName1, byteArrayOf(1))
        fileStorage.saveImage(fileName2, byteArrayOf(2))

        fileStorage.clearCache()

        assertEquals(0, fileStorage.readImage(RuntimeEnvironment.getApplication().cacheDir.absolutePath + "/" + fileName1).size)
    }

    /**
     * Test reading invalid data handles errors.
     */
    @Test
    fun testReadInvalidData() {
        // If file exists but is corrupted, should return empty array because of catch
        val fileName = "corrupted.jpg"
        val path = RuntimeEnvironment.getApplication().cacheDir.absolutePath + "/" + fileName
        java.io.File(path).writeBytes(byteArrayOf(1, 2, 3)) // Not valid IV and ciphertext
        val result = fileStorage.readImage(path)
        assertEquals(0, result.size)
    }

    /**
     * Test readImage fallback when path points to an old cacheDir location,
     * verifying migration compatibility for records persisted in previous versions.
     */
    @Test
    fun testReadImageFallbackWhenPathPointsToOldCacheDir() {
        val fileName = "migrated_photo.jpg"
        val testBytes = byteArrayOf(10, 20, 30, 40, 50)

        // Save image into persistent filesDir
        fileStorage.saveImage(fileName, testBytes)

        // Simulate accessing it via an old v0.0.1 cacheDir path
        val oldCachePath = RuntimeEnvironment.getApplication().cacheDir.absolutePath + "/" + fileName
        val readBytes = fileStorage.readImage(oldCachePath)

        assertTrue(testBytes.contentEquals(readBytes))
    }

    /**
     * Test readImage fallback when file is physically present in cacheDir,
     * verifying automatic promotion to filesDir.
     */
    @Test
    fun testReadImageFallbackWhenFileInCacheDir() {
        val fileName = "legacy_cache_photo.jpg"
        val testBytes = byteArrayOf(99, 88, 77)

        val context = RuntimeEnvironment.getApplication()
        val cacheFile = java.io.File(context.cacheDir, fileName)
        val encryptedData =
            io.healthplatform.chartcam.storage.CryptoHelper
                .encrypt(testBytes)
        cacheFile.writeBytes(encryptedData)

        // Read using fileName or filesDir path
        val targetPath = java.io.File(context.filesDir, fileName).absolutePath
        val readBytes = fileStorage.readImage(targetPath)

        assertTrue(testBytes.contentEquals(readBytes))
        // Verify promoted to filesDir
        assertTrue(java.io.File(context.filesDir, fileName).exists())
    }
}
