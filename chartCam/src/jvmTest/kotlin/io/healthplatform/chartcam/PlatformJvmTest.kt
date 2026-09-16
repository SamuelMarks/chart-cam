/**
 * @file PlatformJvmTest.kt
 * Contains declarations for PlatformJvmTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test class for Platform on JVM.
 */
class PlatformJvmTest {
    /**
     * Test platform properties on JVM.
     */
    @Test
    fun testPlatformJvm() {
        val platform = getPlatform()
        assertTrue(platform.name.isNotBlank())
    }
}
