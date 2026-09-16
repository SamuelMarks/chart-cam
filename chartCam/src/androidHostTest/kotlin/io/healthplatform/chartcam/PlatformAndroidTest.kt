/**
 * @file PlatformAndroidTest.kt
 * Contains declarations for PlatformAndroidTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for Android Platform features.
 */
class PlatformAndroidTest {
    /**
     * Verifies platform naming on Android.
     */
    @Test
    fun testPlatformAndroid() {
        val platform = getPlatform()
        assertTrue(platform.name.contains("Android"))
    }
}
