/**
 * @file SetupPlatformPrivacyJsTest.kt
 * Contains declarations for SetupPlatformPrivacyJsTest.kt.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * JS unit tests for SetupPlatformPrivacy.
 */
class SetupPlatformPrivacyJsTest {
    /**
     * Verifies SetupPlatformPrivacy references currentAppPrivacyManager on JS.
     */
    @Test
    fun testSetupPlatformPrivacyJs() {
        assertNotNull(currentAppPrivacyManager)
    }
}
