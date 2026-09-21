/**
 * @file SilhouetteOverlay.kt
 * Contains declarations for SilhouetteOverlay.kt.
 *
 * Provides real-time on-screen vector guidance overlays for patient alignment during clinical photography.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_silhouette_overlay
import chartcam.chartcam.generated.resources.level_status_level
import chartcam.chartcam.generated.resources.level_status_tilted
import chartcam.chartcam.generated.resources.silhouette_align_cornea_nose
import chartcam.chartcam.generated.resources.silhouette_align_front
import io.healthplatform.chartcam.camera.SilhouetteType
import io.healthplatform.chartcam.sensors.SensorManager
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

private const val LEVEL_TOLERANCE_DEGREES = 3.0f
private const val GHOST_IMAGE_ALPHA = 0.22f

/**
 * Renders an on-screen anatomical silhouette overlay above the camera preview.
 *
 * **State & Side Effects:**
 * Observes sensor orientation to adjust stroke coloring and provide haptic feedback when level.
 *
 * @param silhouetteType The type of silhouette to draw.
 * @param sensorManager Optional sensor manager providing device pitch and roll.
 * @param ghostImageBytes Optional previous image bytes to draw as an onion-skin ghost overlay.
 * @param isVisible Whether the silhouette should be rendered.
 * @param modifier The modifier to be applied to the overlay layout.
 */
@Composable
fun SilhouetteOverlay(
    silhouetteType: SilhouetteType,
    sensorManager: SensorManager? = null,
    ghostImageBytes: ByteArray? = null,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (!isVisible || silhouetteType == SilhouetteType.NONE) return

    val orientation by (
        sensorManager?.orientation ?: kotlinx.coroutines.flow.flowOf(
            io.healthplatform.chartcam.sensors
                .OrientationData(0.0, 0.0),
        )
    ).collectAsState(
        initial =
            io.healthplatform.chartcam.sensors
                .OrientationData(0.0, 0.0),
    )

    val pitch = orientation.pitch.toFloat()
    val roll = orientation.roll.toFloat()
    val isLevel = abs(pitch) < LEVEL_TOLERANCE_DEGREES && abs(roll) < LEVEL_TOLERANCE_DEGREES

    SilhouetteOverlayContent(
        silhouetteType = silhouetteType,
        isLevel = isLevel,
        ghostImageBytes = ghostImageBytes,
        modifier = modifier,
    )
}

/**
 * Stateless content renderer for [SilhouetteOverlay].
 *
 * **State & Side Effects:**
 * Draws vector paths and handles haptic trigger on leveling.
 *
 * @param silhouetteType The type of silhouette to draw.
 * @param isLevel True if device orientation is within acceptable level tolerance.
 * @param ghostImageBytes Optional previous image bytes for ghost overlay.
 * @param modifier The modifier to be applied to the canvas box.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun SilhouetteOverlayContent(
    silhouetteType: SilhouetteType,
    isLevel: Boolean,
    ghostImageBytes: ByteArray? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var lastLevelAnnounced by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(isLevel) {
        if (isLevel && lastLevelAnnounced != true) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastLevelAnnounced = isLevel
    }

    val ghostBitmap =
        remember(ghostImageBytes) {
            ghostImageBytes?.let {
                runCatching { it.decodeToImageBitmap() }.getOrNull()
            }
        }

    val guideText =
        when (silhouetteType) {
            SilhouetteType.PROFILE_CORNEA_NOSE_LEFT,
            SilhouetteType.PROFILE_CORNEA_NOSE_RIGHT,
            -> stringResource(Res.string.silhouette_align_cornea_nose)
            SilhouetteType.FRONTAL_FACE -> stringResource(Res.string.silhouette_align_front)
            SilhouetteType.NONE -> ""
        }

    val statusText =
        if (isLevel) {
            stringResource(Res.string.level_status_level)
        } else {
            stringResource(Res.string.level_status_tilted)
        }
    val fullCd = stringResource(Res.string.cd_silhouette_overlay) + ": $guideText. $statusText"

    val activeColor =
        if (isLevel) {
            MaterialTheme.colorScheme.primary
        } else {
            Color(0xFF00E5FF)
        }
    val outlineColor = Color(0x99000000)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .semantics(mergeDescendants = true) {
                    contentDescription = fullCd
                    liveRegion = LiveRegionMode.Polite
                },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Render ghost image if available (mirrored for contralateral comparison)
            if (ghostBitmap != null) {
                renderGhostImage(ghostBitmap, w, h)
            }

            when (silhouetteType) {
                SilhouetteType.PROFILE_CORNEA_NOSE_LEFT -> {
                    drawProfile(w, h, isMirrored = false, activeColor, outlineColor)
                }
                SilhouetteType.PROFILE_CORNEA_NOSE_RIGHT -> {
                    drawProfile(w, h, isMirrored = true, activeColor, outlineColor)
                }
                SilhouetteType.FRONTAL_FACE -> {
                    drawFrontal(w, h, activeColor, outlineColor)
                }
                SilhouetteType.NONE -> Unit
            }
        }
    }
}

/**
 * Draws the ghost image with transparency and horizontal mirror transformation.
 *
 * @param bitmap The decoded image bitmap.
 * @param width Viewport width.
 * @param height Viewport height.
 */
private fun DrawScope.renderGhostImage(
    bitmap: ImageBitmap,
    width: Float,
    height: Float,
) {
    scale(scaleX = -1f, scaleY = 1f, pivot = Offset(width / 2f, height / 2f)) {
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize(width.toInt(), height.toInt()),
            alpha = GHOST_IMAGE_ALPHA,
        )
    }
}

/**
 * Draws the profile outline, cornea bracket, nose reticle, and Frankfurt line with high-contrast dual strokes.
 *
 * @param w Width.
 * @param h Height.
 * @param isMirrored True if contralateral profile.
 * @param activeColor Color for inner stroke.
 * @param outlineColor Color for outer stroke shadow.
 */
private fun DrawScope.drawProfile(
    w: Float,
    h: Float,
    isMirrored: Boolean,
    activeColor: Color,
    outlineColor: Color,
) {
    val facePath = buildProfileFacePath(w, h, isMirrored)
    val corneaPath = buildCorneaBracketPath(w, h, isMirrored)
    val nosePath = buildNoseBoxPath(w, h, isMirrored)
    val frankfortPath = buildFrankfortLinePath(w, h, isMirrored)

    val outerStroke = Stroke(width = 4.dp.toPx())
    val innerStroke = Stroke(width = 1.8.dp.toPx())

    // Draw outer dark shadow strokes
    drawPath(facePath, outlineColor, style = outerStroke)
    drawPath(corneaPath, outlineColor, style = outerStroke)
    drawPath(nosePath, outlineColor, style = outerStroke)
    drawPath(frankfortPath, outlineColor, style = outerStroke)

    // Draw inner high-contrast strokes
    drawPath(facePath, activeColor, style = innerStroke)
    drawPath(corneaPath, activeColor, style = innerStroke)
    drawPath(nosePath, activeColor, style = innerStroke)
    drawPath(frankfortPath, activeColor, style = innerStroke)
}

/**
 * Draws the frontal oval, bipupillary line, midline, and mouth guides with high-contrast dual strokes.
 *
 * @param w Width.
 * @param h Height.
 * @param activeColor Color for inner stroke.
 * @param outlineColor Color for outer stroke shadow.
 */
private fun DrawScope.drawFrontal(
    w: Float,
    h: Float,
    activeColor: Color,
    outlineColor: Color,
) {
    val ovalPath = buildFrontalFaceOvalPath(w, h)
    val eyesPath = buildBipupillaryLinePath(w, h)
    val midlinePath = buildFacialMidlinePath(w, h)
    val guidesPath = buildNoseAndMouthGuidesPath(w, h)

    val outerStroke = Stroke(width = 4.dp.toPx())
    val innerStroke = Stroke(width = 1.8.dp.toPx())

    // Draw outer dark shadow strokes
    drawPath(ovalPath, outlineColor, style = outerStroke)
    drawPath(eyesPath, outlineColor, style = outerStroke)
    drawPath(midlinePath, outlineColor, style = outerStroke)
    drawPath(guidesPath, outlineColor, style = outerStroke)

    // Draw inner high-contrast strokes
    drawPath(ovalPath, activeColor, style = innerStroke)
    drawPath(eyesPath, activeColor, style = innerStroke)
    drawPath(midlinePath, activeColor, style = innerStroke)
    drawPath(guidesPath, activeColor, style = innerStroke)
}
