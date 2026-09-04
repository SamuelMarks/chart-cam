/**
 * @file AndroidAppInitTest.kt
 * Contains declarations for AndroidAppInitTest.kt.
 */
package io.healthplatform.chartcam

import android.content.Context
import org.junit.Test
import org.mockito.Mockito
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for [AndroidAppInit].
 */
class AndroidAppInitTest {
    /**
     * Test successful context retrieval after init.
     */
    @Test
    fun testInitAndGetContext() {
        val mockContext = Mockito.mock(Context::class.java)
        val mockAppContext = Mockito.mock(Context::class.java)

        val mockCacheDir = java.io.File(System.getProperty("java.io.tmpdir"), "mockCacheDir")
        mockCacheDir.mkdirs()
        // Add a mock file to cover migratePhotosFromCache loop
        java.io.File(mockCacheDir, "mockPhoto.png").createNewFile()

        val mockFilesDir = java.io.File(System.getProperty("java.io.tmpdir"), "mockFilesDir")
        mockFilesDir.mkdirs()

        Mockito.`when`(mockContext.applicationContext).thenReturn(mockAppContext)
        Mockito.`when`(mockAppContext.cacheDir).thenReturn(mockCacheDir)
        Mockito.`when`(mockAppContext.filesDir).thenReturn(mockFilesDir)
        Mockito.`when`(mockAppContext.applicationContext).thenReturn(mockAppContext) // for the nested call in migratePhotosFromCache if any

        AndroidAppInit.init(mockContext)

        val retrievedContext = AndroidAppInit.getContext()
        assertEquals(mockAppContext, retrievedContext)

        // Clean up
        mockCacheDir.deleteRecursively()
        mockFilesDir.deleteRecursively()
    }

    /**
     * Test getting context before init throws exception.
     */
    @Test
    fun testGetContextBeforeInitThrowsException() {
        // We need to reset the context to null using reflection because it's a private var
        val field = AndroidAppInit::class.java.getDeclaredField("context")
        field.isAccessible = true
        field.set(AndroidAppInit, null)

        val exception =
            assertFailsWith<IllegalStateException> {
                AndroidAppInit.getContext()
            }
        assertEquals("AndroidAppInit.init(context) must be called before using platform features.", exception.message)
    }

    /**
     * Test migration when destination file already exists.
     */
    @Test
    fun testInitWhenDestFileAlreadyExists() {
        val mockContext = Mockito.mock(Context::class.java)
        val mockAppContext = Mockito.mock(Context::class.java)

        val mockCacheDir = java.io.File(System.getProperty("java.io.tmpdir"), "mockCacheDirExist")
        mockCacheDir.mkdirs()
        val cacheFile = java.io.File(mockCacheDir, "alreadyThere.png")
        cacheFile.createNewFile()

        val mockFilesDir = java.io.File(System.getProperty("java.io.tmpdir"), "mockFilesDirExist")
        mockFilesDir.mkdirs()
        val destFile = java.io.File(mockFilesDir, "alreadyThere.png")
        destFile.createNewFile()

        Mockito.`when`(mockContext.applicationContext).thenReturn(mockAppContext)
        Mockito.`when`(mockAppContext.cacheDir).thenReturn(mockCacheDir)
        Mockito.`when`(mockAppContext.filesDir).thenReturn(mockFilesDir)

        AndroidAppInit.init(mockContext)

        val retrievedContext = AndroidAppInit.getContext()
        assertEquals(mockAppContext, retrievedContext)

        mockCacheDir.deleteRecursively()
        mockFilesDir.deleteRecursively()
    }

    /**
     * Test migration when cacheDir listFiles returns null.
     */
    @Test
    fun testInitWhenCacheDirListFilesIsNull() {
        val mockContext = Mockito.mock(Context::class.java)
        val mockAppContext = Mockito.mock(Context::class.java)

        val mockCacheFileAsDir = java.io.File(System.getProperty("java.io.tmpdir"), "cacheFileAsDir")
        mockCacheFileAsDir.createNewFile() // A regular file has listFiles() == null

        val mockFilesDir = java.io.File(System.getProperty("java.io.tmpdir"), "mockFilesDirNull")
        mockFilesDir.mkdirs()

        Mockito.`when`(mockContext.applicationContext).thenReturn(mockAppContext)
        Mockito.`when`(mockAppContext.cacheDir).thenReturn(mockCacheFileAsDir)
        Mockito.`when`(mockAppContext.filesDir).thenReturn(mockFilesDir)

        AndroidAppInit.init(mockContext)

        val retrievedContext = AndroidAppInit.getContext()
        assertEquals(mockAppContext, retrievedContext)

        mockCacheFileAsDir.delete()
        mockFilesDir.deleteRecursively()
    }

    /**
     * Test migration when cacheDir contains subdirectories.
     */
    @Test
    fun testInitWhenCacheDirContainsSubdirectories() {
        val mockContext = Mockito.mock(Context::class.java)
        val mockAppContext = Mockito.mock(Context::class.java)

        val mockCacheDir = java.io.File(System.getProperty("java.io.tmpdir"), "mockCacheDirSub")
        mockCacheDir.mkdirs()
        val subDir = java.io.File(mockCacheDir, "code_cache")
        subDir.mkdirs()
        val mockFile = java.io.File(mockCacheDir, "photoToMove.jpg")
        mockFile.createNewFile()

        val mockFilesDir = java.io.File(System.getProperty("java.io.tmpdir"), "mockFilesDirSub")
        mockFilesDir.mkdirs()

        Mockito.`when`(mockContext.applicationContext).thenReturn(mockAppContext)
        Mockito.`when`(mockAppContext.cacheDir).thenReturn(mockCacheDir)
        Mockito.`when`(mockAppContext.filesDir).thenReturn(mockFilesDir)

        AndroidAppInit.init(mockContext)

        // Photo should be moved to filesDir
        val destPhoto = java.io.File(mockFilesDir, "photoToMove.jpg")
        kotlin.test.assertTrue(destPhoto.exists())

        mockCacheDir.deleteRecursively()
        mockFilesDir.deleteRecursively()
    }
}
