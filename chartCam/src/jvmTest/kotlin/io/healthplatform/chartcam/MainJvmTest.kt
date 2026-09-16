/**
 * @file MainJvmTest.kt
 * Contains declarations for MainJvmTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test class for Main on JVM.
 */
class MainJvmTest {
    /**
     * Verifies JVM platform naming.
     */
    @Test
    fun testMainJvm() {
        val platform = getPlatform()
        assertTrue(platform.name.contains("Java") || platform.name.contains("JVM") || platform.name.contains("Desktop"))
    }
}
