/**
 * @file PlatformTest.kt
 * Contains declarations for PlatformTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun testPlatformNameIsNotEmpty() {
        val platform = getPlatform()
        assertTrue(platform.name.isNotEmpty(), "Platform name should not be empty")
        assertTrue(
            platform.name.contains("iOS") ||
                platform.name.contains("Android") ||
                platform.name.contains("Java") ||
                platform.name.contains("Web") ||
                platform.name.contains("Wasm"),
            "Platform name should contain expected keyword",
        )
    }
}
