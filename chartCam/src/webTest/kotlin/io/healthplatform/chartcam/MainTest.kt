/**
 * @file MainTest.kt
 * Contains declarations for MainTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for Main on web.
 */
class MainTest {
    /**
     * Test web route resolution.
     */
    @Test
    fun testMain() {
        assertEquals("/auth/login", io.healthplatform.chartcam.navigation.Routes.LOGIN)
    }
}
