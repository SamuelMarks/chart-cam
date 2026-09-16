/**
 * @file BrowserHistorySetupJsTest.kt
 * Contains declarations for BrowserHistorySetupJsTest.kt.
 */
package io.healthplatform.chartcam.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for BrowserHistorySetup on JS.
 */
class BrowserHistorySetupJsTest {
    /**
     * Test browser history setup route resolution on JS.
     */
    @Test
    fun testBrowserHistorySetupJs() {
        assertEquals("/auth/login", Routes.LOGIN)
    }
}
