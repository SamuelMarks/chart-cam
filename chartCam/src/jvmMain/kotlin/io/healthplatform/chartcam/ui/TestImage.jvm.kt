package io.healthplatform.chartcam.ui

import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage

fun test(img: BufferedImage) {
    img.toComposeImageBitmap()
}
