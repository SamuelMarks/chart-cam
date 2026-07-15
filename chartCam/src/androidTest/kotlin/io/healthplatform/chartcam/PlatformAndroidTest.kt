package io.healthplatform.chartcam

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [Platform] on Android.
 */
@RunWith(AndroidJUnit4::class)
class PlatformAndroidTest {
    /**
     * Test platform creation and name.
     */
    @Test
    fun testGetPlatform() {
        val platform = getPlatform()
        assertTrue(platform is AndroidPlatform)
        assertTrue(platform.name.startsWith("Android "))
    }
}
