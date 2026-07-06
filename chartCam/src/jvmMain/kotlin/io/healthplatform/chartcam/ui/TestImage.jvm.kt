/**
 * Contains test image utilities tailored specifically for the JVM platform.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage

/**
 * A test utility function demonstrating how to convert a standard Java [BufferedImage]
 * into a Jetpack Compose [androidx.compose.ui.graphics.ImageBitmap].
 *
 * @param img The source [BufferedImage] that needs to be converted.
 */
fun test(img: BufferedImage) {
    img.toComposeImageBitmap()
}
