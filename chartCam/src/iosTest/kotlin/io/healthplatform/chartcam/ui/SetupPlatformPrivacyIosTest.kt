/**
 * @file SetupPlatformPrivacyIosTest.kt
 * Contains declarations for SetupPlatformPrivacyIosTest.kt.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * iOS unit tests for SetupPlatformPrivacy.
 */
class SetupPlatformPrivacyIosTest {
    /**
     * Verifies SetupPlatformPrivacy references currentAppPrivacyManager.
     */
    @Test
    fun testSetupPlatformPrivacyIos() {
        assertNotNull(currentAppPrivacyManager)
    }
}
