/**
 * @file SetupPlatformPrivacyWasmJsTest.kt
 * Contains declarations for SetupPlatformPrivacyWasmJsTest.kt.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * WasmJs unit tests for SetupPlatformPrivacy.
 */
class SetupPlatformPrivacyWasmJsTest {
    /**
     * Verifies SetupPlatformPrivacy references currentAppPrivacyManager on WasmJs.
     */
    @Test
    fun testSetupPlatformPrivacyWasmJs() {
        assertNotNull(currentAppPrivacyManager)
    }
}
