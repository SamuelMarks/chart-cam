/**
 * @file PlatformWasmJsTest.kt
 * Contains declarations for PlatformWasmJsTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test class for Platform on WasmJS.
 */
class PlatformWasmJsTest {
    /**
     * Test platform on WasmJS.
     */
    @Test
    fun testPlatformWasmJs() {
        val platform = getPlatform()
        assertTrue(platform.name.isNotBlank())
    }
}
