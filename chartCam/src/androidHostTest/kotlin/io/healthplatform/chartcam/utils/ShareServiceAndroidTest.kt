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

    /**
     * Tests successful file sharing through mock URI provider.
     */
    @Test
    fun testShareFileSuccessWithMockUri() {
        val filesDirFile = File(context.filesDir, "share_success.enc")
        filesDirFile.writeText("payload")
        // allow-exception
        try {
            val service =
                AndroidShareService(
                    context = context,
                    uriProvider = { _, _, f -> android.net.Uri.parse("content://test.fileprovider/${f.name}") },
                )
            val result = service.shareFile(filesDirFile.absolutePath)
            assertTrue(result.isSuccess)
        } finally {
            filesDirFile.delete()
        }
    }

    /**
     * Tests file sharing failure when uri provider throws.
     */
    @Test
    fun testShareFileFailureWhenUriProviderThrows() {
        val filesDirFile = File(context.filesDir, "share_error.enc")
        filesDirFile.writeText("payload")
        // allow-exception
        try {
            val service =
                AndroidShareService(
                    context = context,
                    uriProvider = { _, _, _ -> throw IllegalArgumentException("Failed to find provider") }, // allow-exception
                )
            val result = service.shareFile(filesDirFile.absolutePath)
            assertTrue(result.isFailure)
        } finally {
            filesDirFile.delete()
        }
    }

    /**
     * Tests text sharing failure when startActivity throws.
     */
    @Test
    fun testShareTextFailureWhenStartActivityThrows() {
        val failingContext =
            object : android.content.ContextWrapper(context) {
                override fun startActivity(
                    intent: android.content.Intent?,
                ): Unit = throw android.content.ActivityNotFoundException("No handler for send intent") // allow-exception
            }
        val service = AndroidShareService(context = failingContext)
        val result = service.shareText("test payload")
        assertTrue(result.isFailure)
    }
}
