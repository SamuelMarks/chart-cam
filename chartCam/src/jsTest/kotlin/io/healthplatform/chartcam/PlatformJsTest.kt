/**
 * @file PlatformJsTest.kt
 * Contains declarations for PlatformJsTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test class for Platform on JS.
 */
class PlatformJsTest {
    /**
     * Test platform on JS.
     */
    @Test
    fun testPlatformJs() {
        val platform = getPlatform()
        assertTrue(platform.name.isNotBlank())
    }
}
