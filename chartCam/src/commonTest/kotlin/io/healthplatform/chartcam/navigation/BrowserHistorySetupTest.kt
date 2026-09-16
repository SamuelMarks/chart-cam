/**
 * @file BrowserHistorySetupTest.kt
 * Contains declarations for BrowserHistorySetupTest.kt.
 */
package io.healthplatform.chartcam.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for Browser History Setup routes.
 */
class BrowserHistorySetupTest {
    /**
     * Verifies root route definitions for browser navigation.
     */
    @Test
    fun testBrowserHistorySetup() {
        val rootRoute = Routes.LOGIN
        assertEquals("/auth/login", rootRoute)
    }
}
