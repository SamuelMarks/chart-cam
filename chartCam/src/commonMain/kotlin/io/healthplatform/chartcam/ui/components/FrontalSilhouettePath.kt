/**
 * @file FrontalSilhouettePath.kt
 * Contains declarations for FrontalSilhouettePath.kt.
 *
 * Provides vector path calculation utilities for frontal (anterior) facial alignment silhouettes.
 */
@file:Suppress("MagicNumber")

package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path

/**
 * Builds the anthropometric facial oval [Path] for frontal patient alignment.
 *
 * @param width Viewport width in pixels.
 * @param height Viewport height in pixels.
 * @return A Compose [Path] representing the frontal facial oval.
 */
fun buildFrontalFaceOvalPath(
    width: Float,
    height: Float,
): Path {
    val path = Path()
    val ovalWidth = width * 0.58f
    val ovalHeight = height * 0.65f
    val left = (width - ovalWidth) / 2f
    val top = height * 0.16f

    path.addOval(Rect(left, top, left + ovalWidth, top + ovalHeight))
    return path
}

/**
 * Builds the horizontal bipupillary alignment line and dual eye boundary boxes [Path].
 *
 * @param width Viewport width in pixels.
 * @param height Viewport height in pixels.
 * @return A Compose [Path] for the bipupillary alignment guide.
 */
fun buildBipupillaryLinePath(
    width: Float,
    height: Float,
): Path {
    val path = Path()
    val eyeY = height * 0.40f

    // Horizontal bipupillary baseline
    path.moveTo(width * 0.26f, eyeY)
    path.lineTo(width * 0.74f, eyeY)

    // Left eye box (viewer's left = patient's right)
    val eyeBoxWidth = width * 0.12f
    val eyeBoxHeight = height * 0.06f
    val leftEyeCenterX = width * 0.38f
    path.addRoundRect(
        RoundRect(
            rect =
                Rect(
                    leftEyeCenterX - (eyeBoxWidth / 2f),
                    eyeY - (eyeBoxHeight / 2f),
                    leftEyeCenterX + (eyeBoxWidth / 2f),
                    eyeY + (eyeBoxHeight / 2f),
                ),
            cornerRadius = CornerRadius(eyeBoxWidth * 0.2f, eyeBoxWidth * 0.2f),
        ),
    )

    // Right eye box (viewer's right = patient's left)
    val rightEyeCenterX = width * 0.62f
    path.addRoundRect(
        RoundRect(
            rect =
                Rect(
                    rightEyeCenterX - (eyeBoxWidth / 2f),
                    eyeY - (eyeBoxHeight / 2f),
                    rightEyeCenterX + (eyeBoxWidth / 2f),
                    eyeY + (eyeBoxHeight / 2f),
                ),
            cornerRadius = CornerRadius(eyeBoxWidth * 0.2f, eyeBoxWidth * 0.2f),
        ),
    )

    return path
}

/**
 * Builds the vertical facial midline [Path] connecting glabella, philtrum, and mentum.
 *
 * @param width Viewport width in pixels.
 * @param height Viewport height in pixels.
 * @return A Compose [Path] for the facial midline.
 */
fun buildFacialMidlinePath(
    width: Float,
    height: Float,
): Path {
    val path = Path()
    val centerX = width * 0.50f
    val startY = height * 0.16f
    val endY = height * 0.81f

    val step = (endY - startY) / 14f
    for (i in 0 until 14 step 2) {
        path.moveTo(centerX, startY + (i * step))
        path.lineTo(centerX, startY + ((i + 1) * step))
    }
    return path
}

/**
 * Builds the horizontal nasal base and oral commissure guide lines [Path].
 *
 * @param width Viewport width in pixels.
 * @param height Viewport height in pixels.
 * @return A Compose [Path] for the nose and mouth baseline guides.
 */
fun buildNoseAndMouthGuidesPath(
    width: Float,
    height: Float,
): Path {
    val path = Path()
    val centerX = width * 0.50f

    // Nose base width guide
    val noseY = height * 0.54f
    path.moveTo(centerX - (width * 0.08f), noseY)
    path.lineTo(centerX + (width * 0.08f), noseY)

    // Mouth / lip width guide
    val mouthY = height * 0.66f
    path.moveTo(centerX - (width * 0.11f), mouthY)
    path.lineTo(centerX + (width * 0.11f), mouthY)

    return path
}
