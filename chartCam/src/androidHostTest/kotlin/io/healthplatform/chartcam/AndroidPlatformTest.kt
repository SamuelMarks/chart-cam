/**
 * @file AndroidPlatformTest.kt
 * Contains declarations for AndroidPlatformTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

class AndroidPlatformTest {
    @Test
    fun testPlatformName() {
        val platform = getPlatform()
        assertTrue(platform is AndroidPlatform)
        // Robolectric or local test might have SDK_INT = 0 or some other number
        assertTrue(platform.name.startsWith("Android"))
    }
}
