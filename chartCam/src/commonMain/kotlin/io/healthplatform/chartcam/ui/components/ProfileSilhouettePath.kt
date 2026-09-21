/**
 * @file ProfileSilhouettePath.kt
 * Contains declarations for ProfileSilhouettePath.kt.
 *
 * Provides vector path calculation utilities for lateral craniofacial and cornea/nose profile silhouettes.
 */
@file:Suppress("MagicNumber", "LongMethod")

package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path

/**
 * Maps a normalized horizontal coordinate [0.0..1.0] to pixel canvas width, accounting for mirroring.
 *
 * @param normX Normalized horizontal coordinate.
 * @param width Viewport width in pixels.
 * @param isMirrored True if mirrored horizontally for contralateral view.
 * @return Pixel X coordinate.
 */
private fun mapNormX(normX: Float, width: Float, isMirrored: Boolean): Float =
    if (isMirrored) width * (1f - normX) else width * normX

/**
 * Maps a normalized vertical coordinate [0.0..1.0] to pixel canvas height.
 *
 * @param normY Normalized vertical coordinate.
 * @param height Viewport height in pixels.
 * @return Pixel Y coordinate.
 */
private fun mapNormY(normY: Float, height: Float): Float = height * normY

/**
 * Builds the facial profile outline [Path] matching anatomical landmarks (forehead, nasion, nose, lips, chin, jaw).
 *
 * @param width Viewport width in pixels.
 * @param height Viewport height in pixels.
 * @param isMirrored True if facing right (contralateral profile), false if facing left.
 * @return A Compose [Path] representing the profile outline.
 */
fun buildProfileFacePath(
    width: Float,
    height: Float,
    isMirrored: Boolean = false,
): Path {
    val path = Path()

    path.moveTo(mapNormX(0.50f, width, isMirrored), mapNormY(0.12f, height))

    // Forehead slope to Glabella
    path.cubicTo(
        mapNormX(0.42f, width, isMirrored),
        mapNormY(0.16f, height),
        mapNormX(0.37f, width, isMirrored),
        mapNormY(0.24f, height),
        mapNormX(0.36f, width, isMirrored),
        mapNormY(0.30f, height),
    )

    // Glabella to Nasion (nasal bridge depression)
    path.quadraticTo(
        mapNormX(0.35f, width, isMirrored),
        mapNormY(0.33f, height),
        mapNormX(0.34f, width, isMirrored),
        mapNormY(0.36f, height),
    )

    // Nasion down along nasal dorsum to Pronasion (nasal tip)
    path.cubicTo(
        mapNormX(0.32f, width, isMirrored),
        mapNormY(0.40f, height),
        mapNormX(0.25f, width, isMirrored),
        mapNormY(0.45f, height),
        mapNormX(0.22f, width, isMirrored),
        mapNormY(0.48f, height),
    )

    // Pronasion curving under to Columella & Subnasale
    path.cubicTo(
        mapNormX(0.22f, width, isMirrored),
        mapNormY(0.50f, height),
        mapNormX(0.28f, width, isMirrored),
        mapNormY(0.52f, height),
        mapNormX(0.31f, width, isMirrored),
        mapNormY(0.53f, height),
    )

    // Subnasale to Labrale superius (upper lip)
    path.quadraticTo(
        mapNormX(0.30f, width, isMirrored),
        mapNormY(0.55f, height),
        mapNormX(0.28f, width, isMirrored),
        mapNormY(0.57f, height),
    )

    // Upper lip to Stomion (oral commissure)
    path.quadraticTo(
        mapNormX(0.27f, width, isMirrored),
        mapNormY(0.59f, height),
        mapNormX(0.31f, width, isMirrored),
        mapNormY(0.60f, height),
    )

    // Stomion to Labrale inferius (lower lip)
    path.quadraticTo(
        mapNormX(0.27f, width, isMirrored),
        mapNormY(0.62f, height),
        mapNormX(0.28f, width, isMirrored),
        mapNormY(0.64f, height),
    )

    // Lower lip to Mentolabial sulcus to Pogonion (chin apex)
    path.cubicTo(
        mapNormX(0.31f, width, isMirrored),
        mapNormY(0.66f, height),
        mapNormX(0.26f, width, isMirrored),
        mapNormY(0.70f, height),
        mapNormX(0.27f, width, isMirrored),
        mapNormY(0.74f, height),
    )

    // Pogonion to Gnathion and along submental/jawline
    path.cubicTo(
        mapNormX(0.28f, width, isMirrored),
        mapNormY(0.78f, height),
        mapNormX(0.35f, width, isMirrored),
        mapNormY(0.82f, height),
        mapNormX(0.46f, width, isMirrored),
        mapNormY(0.83f, height),
    )

    return path
}

/**
 * Builds the cornea tangent bracket and proptosis clearance guide [Path].
 * Highlights the anterior corneal curve to ensure tangential visibility against background.
 *
 * @param width Viewport width in pixels.
 * @param height Viewport height in pixels.
 * @param isMirrored True if facing right, false if facing left.
 * @return A Compose [Path] for the corneal apex guide.
 */
fun buildCorneaBracketPath(
    width: Float,
    height: Float,
    isMirrored: Boolean = false,
): Path {
    val path = Path()

    // Anterior corneal arc
    path.moveTo(mapNormX(0.34f, width, isMirrored), mapNormY(0.34f, height))
    path.quadraticTo(
        mapNormX(0.29f, width, isMirrored),
        mapNormY(0.38f, height),
        mapNormX(0.34f, width, isMirrored),
        mapNormY(0.42f, height),
    )

    // Lateral orbital rim reference arc
    path.moveTo(mapNormX(0.37f, width, isMirrored), mapNormY(0.33f, height))
    path.quadraticTo(
        mapNormX(0.35f, width, isMirrored),
        mapNormY(0.38f, height),
        mapNormX(0.37f, width, isMirrored),
        mapNormY(0.43f, height),
    )

    // Cornea proptosis horizon tick
    val horizonDir = if (isMirrored) 1f else -1f
    val apexX = mapNormX(0.29f, width, isMirrored)
    val apexY = mapNormY(0.38f, height)
    path.moveTo(apexX, apexY)
    path.lineTo(apexX + (horizonDir * width * 0.04f), apexY)

    return path
}

/**
 * Builds the nasal tip focal reticle [Path].
 *
 * @param width Viewport width in pixels.
 * @param height Viewport height in pixels.
 * @param isMirrored True if facing right, false if facing left.
 * @return A Compose [Path] for the nose target reticle.
 */
fun buildNoseBoxPath(
    width: Float,
    height: Float,
    isMirrored: Boolean = false,
): Path {
    val path = Path()
    val boxWidth = width * 0.10f
    val boxHeight = height * 0.10f

    val centerNormX = if (isMirrored) 1f - 0.22f else 0.22f
    val left = (width * centerNormX) - (boxWidth / 2f)
    val top = (height * 0.48f) - (boxHeight / 2f)

    path.addRoundRect(
        RoundRect(
            rect = Rect(left, top, left + boxWidth, top + boxHeight),
            cornerRadius = CornerRadius(boxWidth * 0.2f, boxWidth * 0.2f),
        ),
    )
    return path
}

/**
 * Builds the dashed Frankfurt Horizontal baseline [Path].
 * Connects the tragus level to the infraorbital margin to guarantee zero head tilt.
 *
 * @param width Viewport width in pixels.
 * @param height Viewport height in pixels.
 * @param isMirrored True if facing right, false if facing left.
 * @return A Compose [Path] for the horizontal alignment guide.
 */
fun buildFrankfortLinePath(
    width: Float,
    height: Float,
    isMirrored: Boolean = false,
): Path {
    val path = Path()
    val y = height * 0.44f

    val startX = mapNormX(0.60f, width, isMirrored)
    val endX = mapNormX(0.20f, width, isMirrored)
    val step = (endX - startX) / 12f

    for (i in 0 until 12 step 2) {
        path.moveTo(startX + (i * step), y)
        path.lineTo(startX + ((i + 1) * step), y)
    }

    return path
}
