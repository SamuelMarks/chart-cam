/**
 * @file BrowserHistorySetupAndroidTest.kt
 * Contains declarations for BrowserHistorySetupAndroidTest.kt.
 */
package io.healthplatform.chartcam.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Android host tests for BrowserHistorySetup.
 */
class BrowserHistorySetupAndroidTest {
    /**
     * Verifies Android route definitions.
     */
    @Test
    fun testBrowserHistorySetupAndroid() {
        assertEquals("/auth/login", Routes.LOGIN)
    }
}
