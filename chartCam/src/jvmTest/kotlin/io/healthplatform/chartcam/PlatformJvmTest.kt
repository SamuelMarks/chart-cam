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

    /**
     * Test platform default implementation bridge if present on JVM.
     */
    @Test
    fun testPlatformDefaultImpls() {
        val customMobile =
            object : Platform {
                override val name: String = "Android 34"
            }
        runCatching {
            val defaultImpls = Class.forName("io.healthplatform.chartcam.Platform\$DefaultImpls")
            val method = defaultImpls.getMethod("isMobile", Platform::class.java)
            val result = method.invoke(null, customMobile) as Boolean
            assertTrue(result)
        }
    }
}
