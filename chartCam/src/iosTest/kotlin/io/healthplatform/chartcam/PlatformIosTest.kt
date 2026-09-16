/**
 * @file PlatformIosTest.kt
 * Contains declarations for PlatformIosTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for iOS-specific platform functionality.
 */
class PlatformIosTest {
    /**
     * Verifies that getPlatform returns the iOS platform descriptor.
     */
    @Test
    fun testPlatformIos() {
        val platform = getPlatform()
        assertTrue(platform.name.contains("iOS"))
    }
}
