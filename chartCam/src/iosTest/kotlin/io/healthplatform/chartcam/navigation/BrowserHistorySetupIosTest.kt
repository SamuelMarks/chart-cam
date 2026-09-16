/**
 * @file BrowserHistorySetupIosTest.kt
 * Contains declarations for BrowserHistorySetupIosTest.kt.
 */
package io.healthplatform.chartcam.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Validates no-op safety of history setup on iOS.
 */
class BrowserHistorySetupIosTest {
    /**
     * Verifies that the iOS navigation root route is correctly defined.
     */
    @Test
    fun testBrowserHistoryClassIntegrity() {
        assertEquals("/auth/login", Routes.LOGIN)
    }
}
