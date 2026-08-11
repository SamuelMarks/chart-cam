/**
 * @file ScreenshotGeneratorTest.kt
 * Contains declarations for ScreenshotGeneratorTest.kt.
 */
package io.healthplatform.chartcam

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

/**
 * Test class for generating screenshots.
 */
class ScreenshotGeneratorTest {
    /**
     * Generates a screenshot of the app on JVM.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun generateScreenshots() {
        runDesktopComposeUiTest(width = 1284, height = 2778) {
            setContent {
                androidx.compose.material3.Text("iPhone Size")
            }
            val img = onRoot().captureToImage().toAwtImage()
            ImageIO.write(img, "png", File("iphone.png"))
        }
    }
}
