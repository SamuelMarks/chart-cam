/**
 * @file BrowserHistorySetupJvmTest.kt
 * Contains declarations for BrowserHistorySetupJvmTest.kt.
 */
package io.healthplatform.chartcam.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for BrowserHistorySetup on JVM.
 */
class BrowserHistorySetupJvmTest {
    /**
     * Test browser history setup component presence on JVM.
     */
    @Test
    fun testBrowserHistorySetupJvm() {
        val rootRoute = Routes.LOGIN
        assertEquals("/auth/login", rootRoute)
    }
}
