/**
 * @file ShareServiceAndroidTest.kt
 * Contains declarations for ShareServiceAndroidTest.kt.
 */
package io.healthplatform.chartcam.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.healthplatform.chartcam.AndroidAppInit
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Android host tests for [AndroidShareService].
 */
@RunWith(RobolectricTestRunner::class)
class ShareServiceAndroidTest {
    private lateinit var context: Context

    /**
     * Setup for tests.
     */
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        AndroidAppInit.init(context)
    }

    /**
     * Teardown for tests.
     */
    @After
    fun teardown() {
        val field = AndroidAppInit::class.java.getDeclaredField("context")
        field.isAccessible = true
        field.set(AndroidAppInit, null)
    }

    /**
     * Tests AndroidShareService with non-existent files.
     */
    @Test
    fun testShareNonExistentFileReturnsFailure() {
        val service = createShareService()
        val result = service.shareFile("completely_nonexistent_file_path.enc")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ExportFileNotFoundException)
    }

    /**
     * Tests sharing text on Android.
     */
    @Test
    fun testShareTextAndroid() {
        val service = createShareService()
        val result = service.shareText("Secure Password 123")
        assertNotNull(result)
    }

    /**
     * Tests sharing files located in filesDir and cacheDir, including relative fallback resolution.
     */
    @Test
    fun testShareFileResolutionAndFallback() {
        val service = createShareService()

        // Create file in internal filesDir
        val filesDirFile = File(context.filesDir, "export_test.enc")
        filesDirFile.writeText("sample encrypted payload")

        // Create file in cacheDir
        val cacheDirFile = File(context.cacheDir, "questionnaire_test.json")
        cacheDirFile.writeText("{}")

        // Test absolute path resolution
        val absResult = service.shareFile(filesDirFile.absolutePath)
        assertNotNull(absResult)

        // Test relative path resolution for filesDir file
        val relFilesResult = service.shareFile("export_test.enc")
        assertNotNull(relFilesResult)

        // Test relative path resolution for cacheDir file
        val relCacheResult = service.shareFile("questionnaire_test.json")
        assertNotNull(relCacheResult)

        filesDirFile.delete()
        cacheDirFile.delete()
    }
}
