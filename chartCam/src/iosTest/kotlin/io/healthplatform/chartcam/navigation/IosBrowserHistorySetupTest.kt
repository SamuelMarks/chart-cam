/**
 * @file IosBrowserHistorySetupTest.kt
 * Contains declarations for IosBrowserHistorySetupTest.kt.
 */
package io.healthplatform.chartcam.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Supplementary test wrapper for browser history module on iOS.
 */
class IosBrowserHistorySetupTest {
    /**
     * Verifies that the iOS navigation capture route is accessible.
     */
    @Test
    fun testNavigationModuleIntegrity() {
        assertEquals("/capture", Routes.CAPTURE)
    }
}
