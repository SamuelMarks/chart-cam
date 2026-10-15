/**
 * @file JvmTestImageJvmTest.kt
 * Contains JVM unit tests verifying [BufferedImage] to Compose [androidx.compose.ui.graphics.ImageBitmap] conversion.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for [BufferedImage] to Compose [androidx.compose.ui.graphics.ImageBitmap] conversion on JVM.
 */
class JvmTestImageJvmTest {
    /**
     * Verifies converting a [BufferedImage] into a Compose [androidx.compose.ui.graphics.ImageBitmap] preserves dimensions.
     */
    @Test
    fun testTestImage() {
        val img = BufferedImage(10, 12, BufferedImage.TYPE_INT_ARGB)
        val bitmap = img.toComposeImageBitmap()
        assertEquals(10, bitmap.width)
        assertEquals(12, bitmap.height)
    }
}
