package io.healthplatform.chartcam.ui

import org.junit.Test
import java.awt.image.BufferedImage

class TestImageJvmTest {
    @Test
    fun testImageConversion() {
        // Create a simple 1x1 BufferedImage
        val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)

        // This will call the 'test' function and ensure it does not throw
        test(img)
    }
}
