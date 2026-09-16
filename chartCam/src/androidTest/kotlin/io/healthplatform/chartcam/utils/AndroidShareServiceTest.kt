/**
 * @file AndroidShareServiceTest.kt
 * Contains declarations for AndroidShareServiceTest.kt.
 */
package io.healthplatform.chartcam.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.share_password
import io.healthplatform.chartcam.AndroidAppInit
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Tests for the Android [ShareService].
 */
@RunWith(AndroidJUnit4::class)
class AndroidShareServiceTest {
    /**
     * Setup test environment.
     */
    @Before
    fun setup() {
        AndroidAppInit.init(ApplicationProvider.getApplicationContext())
    }

    /**
     * Tests creating a share service.
     */
    @Test
    fun testCreateShareService() {
        val service = createShareService()
        assertTrue(service is AndroidShareService)
    }

    /**
     * Tests sharing text logic.
     */
    @Test
    fun testShareText() =
        runBlocking {
            val service = createShareService()
            val text = getString(Res.string.share_password)
            val result = service.shareText(text)
            assertNotNull(result)
        }

    /**
     * Tests sharing a file.
     */
    @Test
    fun testShareFile() {
        val service = createShareService()
        val file = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "test.txt")
        file.writeText("test")
        val result = service.shareFile(file.absolutePath)
        assertNotNull(result)
        file.delete()
    }
}
