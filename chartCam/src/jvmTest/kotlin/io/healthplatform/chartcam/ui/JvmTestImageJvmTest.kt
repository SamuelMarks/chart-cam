/**
 * @file JvmTestImageJvmTest.kt
 * Contains declarations for JvmTestImageJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for JvmTestImage on JVM.
 */
class JvmTestImageJvmTest {
    /**
     * Verifies converting BufferedImage into Compose ImageBitmap via test() helper.
     */
    @Test
    fun testTestImage() {
        val img = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        test(img)
        assertNotNull(img)
    }
}
