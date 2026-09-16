/**
 * @file SetupPlatformPrivacyAndroidTest.kt
 * Contains declarations for SetupPlatformPrivacyAndroidTest.kt.
 */
package io.healthplatform.chartcam.ui

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

/**
 * Android unit tests for SetupPlatformPrivacy and AppPrivacyManager.
 */
@RunWith(RobolectricTestRunner::class)
class SetupPlatformPrivacyAndroidTest {
    /**
     * Tests that currentAppPrivacyManager lifecycle state transitions execute cleanly on Android.
     */
    @Test
    fun testSetupPlatformPrivacyRuns() {
        currentAppPrivacyManager.onAppMovedToBackground()
        assertNotNull(currentAppPrivacyManager)
        currentAppPrivacyManager.onAppMovedToForeground()
        assertNotNull(currentAppPrivacyManager)
    }
}
