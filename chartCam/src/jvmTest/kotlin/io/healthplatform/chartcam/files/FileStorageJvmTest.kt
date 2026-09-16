/**
 * @file FileStorageJvmTest.kt
 * Unit tests verifying JVM file storage persistence, deletion, and cache clearing.
 */
package io.healthplatform.chartcam.files

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test suite for [JvmFileStorage].
 */
class FileStorageJvmTest {
    /**
     * Tests saving, reading, resolving, and deleting files with JvmFileStorage.
     */
    @Test
    fun testSaveReadDeleteFile() {
        val tempTestDir = (System.getProperty("java.io.tmpdir") + "/chartcam_test_" + System.currentTimeMillis()).toPath()
        val storage = JvmFileStorage(tempTestDir)

        val payload = "Clinical photo payload".toByteArray(Charsets.UTF_8)
        val savedPath = storage.saveImage("patient_lesion.jpg", payload)

        assertTrue(savedPath.isNotEmpty())
        val readBack = storage.readImage(savedPath)
        assertEquals("Clinical photo payload", String(readBack, Charsets.UTF_8))

        // Read by relative filename
        val readRelative = storage.readImage("patient_lesion.jpg")
        assertEquals("Clinical photo payload", String(readRelative, Charsets.UTF_8))

        // Test non-existent file read
        val nonExistent = storage.readImage("ghost_file.jpg")
        assertEquals(0, nonExistent.size)

        // Test delete
        val deleteRes = storage.deleteImage("patient_lesion.jpg")
        assertTrue(deleteRes.isSuccess)

        // Delete non-existent file
        val deleteGhost = storage.deleteImage("ghost_file.jpg")
        assertTrue(deleteGhost.isFailure)

        // Nested directory file saving
        val nestedSaved = storage.saveImage("nested/subfolder/file.jpg", payload)
        assertTrue(nestedSaved.contains("nested"))
        assertEquals("Clinical photo payload", String(storage.readImage(nestedSaved), Charsets.UTF_8))

        // Saving to absolute path
        val absPath = (tempTestDir / "direct_abs.jpg").toString()
        val directSaved = storage.saveImage(absPath, payload)
        assertEquals(absPath, directSaved)
        assertEquals("Clinical photo payload", String(storage.readImage(directSaved), Charsets.UTF_8))

        // Path starting with backslash (UNC/root)
        val winSaved = storage.saveImage("win_test\\photo.jpg", payload)
        assertTrue(winSaved.isNotEmpty())

        val uncPath = "\\\\server\\share\\photo.jpg"
        storage.readImage(uncPath)

        // Drive letter path format
        val drivePath = "C:\\test\\drive.jpg"
        storage.readImage(drivePath)

        // Short relative filename
        val shortRelative = "a"
        storage.readImage(shortRelative)

        // Path with null parent
        runCatching { storage.saveImage("/", ByteArray(0)) }

        // Clean up test dir
        FileSystem.SYSTEM.deleteRecursively(tempTestDir)
    }

    /**
     * Tests cache clearing removes temporary preview files.
     */
    @Test
    fun testClearCache() {
        val tempTestDir = (System.getProperty("java.io.tmpdir") + "/chartcam_cache_test_" + System.currentTimeMillis()).toPath()
        val storage = JvmFileStorage(tempTestDir)

        storage.saveImage("preview_1.tmp", "temporary 1".toByteArray(Charsets.UTF_8))
        storage.saveImage("temp_thumb.jpg", "temporary 2".toByteArray(Charsets.UTF_8))
        storage.saveImage("permanent.jpg", "keep this".toByteArray(Charsets.UTF_8))

        storage.clearCache()

        assertEquals(0, storage.readImage("preview_1.tmp").size)
        assertEquals(0, storage.readImage("temp_thumb.jpg").size)
        assertTrue(storage.readImage("permanent.jpg").isNotEmpty())

        // Clear cache on non-existent base directory
        FileSystem.SYSTEM.deleteRecursively(tempTestDir)
        storage.clearCache()
    }
}
