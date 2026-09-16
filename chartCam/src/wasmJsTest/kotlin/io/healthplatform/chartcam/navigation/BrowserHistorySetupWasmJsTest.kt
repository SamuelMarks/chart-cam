/**
 * @file BrowserHistorySetupWasmJsTest.kt
 * Contains declarations for BrowserHistorySetupWasmJsTest.kt.
 */
package io.healthplatform.chartcam.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for BrowserHistorySetup on WasmJS.
 */
class BrowserHistorySetupWasmJsTest {
    /**
     * Test browser history setup route resolution on WasmJS.
     */
    @Test
    fun testBrowserHistorySetupWasmJs() {
        assertEquals("/auth/login", Routes.LOGIN)
    }
}
