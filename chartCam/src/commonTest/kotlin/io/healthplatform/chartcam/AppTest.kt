/**
 * @file AppTest.kt
 * Contains declarations for AppTest.kt.
 */
package io.healthplatform.chartcam

import io.healthplatform.chartcam.navigation.Routes
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Common tests for the main application entry point.
 */
class AppTest {
    /**
     * Test the basic app initialization and routing root.
     */
    @Test
    fun testApp() {
        val rootRoute = Routes.LOGIN
        assertEquals("/auth/login", rootRoute)
    }
}
