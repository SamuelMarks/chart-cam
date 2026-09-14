/**
 * @file VisualPainScaleControl.kt
 * Contains declarations for VisualPainScaleControl.kt.
 *
 * Interactive Wong-Baker FACES and Visual Analogue Scale (VAS) pain assessment control.
 */
package io.healthplatform.chartcam.ui.sdc.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_pain_slider
import chartcam.chartcam.generated.resources.pain_level_0
import chartcam.chartcam.generated.resources.pain_level_10
import chartcam.chartcam.generated.resources.pain_level_2
import chartcam.chartcam.generated.resources.pain_level_4
import chartcam.chartcam.generated.resources.pain_level_6
import chartcam.chartcam.generated.resources.pain_level_8
import chartcam.chartcam.generated.resources.pain_scale_state_desc_format
import chartcam.chartcam.generated.resources.pain_score_format
import chartcam.chartcam.generated.resources.state_selected
import chartcam.chartcam.generated.resources.state_unselected
import io.healthplatform.chartcam.models.PainScaleDefaults
import io.healthplatform.chartcam.models.PainScaleLevel
import io.healthplatform.chartcam.ui.components.FormLabel
import io.healthplatform.chartcam.ui.theme.AppSpacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val MIN_PAIN_SCORE = 0f
private const val MAX_PAIN_SCORE = 10f
private const val PAIN_STEPS = 4

/**
 * Returns the localized [StringResource] for a given pain score.
 *
 * @param score The pain score from 0 to 10.
 * @return The corresponding string resource.
 */
fun getPainScoreResource(score: Int): StringResource =
    when {
        score <= 1 -> Res.string.pain_level_0
        score <= 3 -> Res.string.pain_level_2
        score <= 5 -> Res.string.pain_level_4
        score <= 7 -> Res.string.pain_level_6
        score <= 9 -> Res.string.pain_level_8
        else -> Res.string.pain_level_10
    }

/**
 * Returns the theme-aware accessible color associated with a given pain score.
 *
 * @param score The pain score from 0 to 10.
 * @param isDarkTheme Whether dark theme palette is active.
 * @return The Compose [Color] representing the severity with WCAG-compliant contrast.
 */
fun getPainScoreColor(
    score: Int,
    isDarkTheme: Boolean = false,
): Color =
    io.healthplatform.chartcam.ui.theme
        .resolvePainScoreColor(score, isDarkTheme)

/**
 * Overload for backward compatibility defaulting to light theme.
 *
 * @param score The pain score from 0 to 10.
 * @return The Compose [Color] representing the severity.
 */
fun getPainScoreColor(score: Int): Color = getPainScoreColor(score, isDarkTheme = false)

/**
 * Interactive Wong-Baker FACES and Visual Analogue Scale (VAS) pain assessment control.
 *
 * @param value The currently selected integer pain rating (0 to 10), or null.
 * @param onValueChange Callback invoked when the user selects or adjusts the pain score.
 * @param label The localized title/label for the questionnaire item.
 * @param isRequired Whether an answer is required.
 * @param isError Whether this control is in an error state.
 * @param errorMessage Localized error message to announce when [isError] is true.
 * @param readOnly Whether the control is rendered in read-only / review mode.
 * @param modifier The modifier to apply to the root layout.
 */
@Composable
fun VisualPainScaleControl(
    value: Int?,
    onValueChange: (Int) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val currentScore = (value ?: 0).coerceIn(0, 10)
    val isDark = isSystemInDarkTheme()
    val activeColor = getPainScoreColor(currentScore, isDark)
    val localizedSeverity = stringResource(getPainScoreResource(currentScore))
    val scoreSummary = stringResource(Res.string.pain_score_format, currentScore)
    val sliderDesc = stringResource(Res.string.cd_pain_slider)
    val stateDesc = stringResource(Res.string.pain_scale_state_desc_format, scoreSummary, localizedSeverity)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.sm)
                .semantics {
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                        liveRegion = LiveRegionMode.Polite
                    }
                },
    ) {
        FormLabel(
            text = label,
            isRequired = isRequired,
            modifier = Modifier.padding(bottom = AppSpacing.compact),
        )

        if (readOnly) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().testTag("VisualPainScaleReadOnly"),
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.moderate),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    PainFaceIcon(
                        score = currentScore,
                        isSelected = true,
                        activeColor = activeColor,
                        modifier = Modifier.size(AppSpacing.minTouchTarget),
                    )
                    Column {
                        Text(
                            text = scoreSummary,
                            style = MaterialTheme.typography.titleMedium,
                            color = activeColor,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = localizedSeverity,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            val selectedStateDesc = stringResource(Res.string.state_selected)
            val unselectedStateDesc = stringResource(Res.string.state_unselected)
            // Interactive Face Selector Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PainScaleDefaults.LEVELS.forEach { level: PainScaleLevel ->
                    val isSelected = currentScore == level.score
                    val faceLabel = stringResource(getPainScoreResource(level.score))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("PainFaceButton_${level.score}")
                                .clickable(role = Role.RadioButton) { onValueChange(level.score) }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = faceLabel
                                    stateDescription =
                                        if (isSelected) {
                                            selectedStateDesc
                                        } else {
                                            unselectedStateDesc
                                        }
                                },
                    ) {
                        PainFaceIcon(
                            score = level.score,
                            isSelected = isSelected,
                            activeColor = getPainScoreColor(level.score, isDark),
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            // Interactive Continuous/Stepped Slider
            Slider(
                value = currentScore.toFloat(),
                onValueChange = { onValueChange(it.toInt().coerceIn(0, 10)) },
                valueRange = MIN_PAIN_SCORE..MAX_PAIN_SCORE,
                steps = PAIN_STEPS,
                colors =
                    SliderDefaults.colors(
                        thumbColor = activeColor,
                        activeTrackColor = activeColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("PainScaleSlider")
                        .semantics {
                            contentDescription = sliderDesc
                            stateDescription = stateDesc
                        },
            )

            // Current Score Display Badge
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = AppSpacing.xs)
                        .background(activeColor.copy(alpha = 0.12f), MaterialTheme.shapes.small)
                        .border(1.dp, activeColor.copy(alpha = 0.4f), MaterialTheme.shapes.small)
                        .padding(horizontal = AppSpacing.moderate, vertical = AppSpacing.compact),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = scoreSummary,
                    style = MaterialTheme.typography.titleSmall,
                    color = activeColor,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = localizedSeverity,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = AppSpacing.xs),
            )
        }
    }
}

/**
 * Renders an affective vector face representing pain severity on a [Canvas].
 *
 * @param score The pain score level.
 * @param isSelected Whether this face is currently selected.
 * @param activeColor The color tone representing the face's severity.
 * @param modifier The modifier for sizing the canvas.
 */
@Composable
fun PainFaceIcon(
    score: Int,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    val faceLabel = stringResource(getPainScoreResource(score))
    val defaultBorderColor = MaterialTheme.colorScheme.outlineVariant
    val defaultFeatureColor = MaterialTheme.colorScheme.outline
    Box(
        modifier =
            modifier
                .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
                .border(if (isSelected) 2.dp else 1.dp, if (isSelected) activeColor else defaultBorderColor, CircleShape)
                .semantics {
                    contentDescription = faceLabel
                },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val strokeWidth = 2.dp.toPx()
            val eyeRadius = 1.8.dp.toPx()
            val eyeColor = if (isSelected) activeColor else defaultFeatureColor

            // Left Eye
            drawCircle(color = eyeColor, radius = eyeRadius, center = Offset(size.width * 0.32f, size.height * 0.38f))
            // Right Eye
            drawCircle(color = eyeColor, radius = eyeRadius, center = Offset(size.width * 0.68f, size.height * 0.38f))

            // Mouth Shape based on score
            when {
                score <= 2 -> {
                    // Smile Arc
                    drawArc(
                        color = eyeColor,
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.28f, size.height * 0.40f),
                        size = Size(size.width * 0.44f, size.height * 0.35f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                score <= 4 -> {
                    // Neutral straight line
                    drawLine(
                        color = eyeColor,
                        start = Offset(size.width * 0.32f, size.height * 0.68f),
                        end = Offset(size.width * 0.68f, size.height * 0.68f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
                score <= 6 -> {
                    // Slight frown
                    drawArc(
                        color = eyeColor,
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.28f, size.height * 0.60f),
                        size = Size(size.width * 0.44f, size.height * 0.30f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                else -> {
                    // Pronounced frown / distressed
                    drawArc(
                        color = eyeColor,
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.24f, size.height * 0.55f),
                        size = Size(size.width * 0.52f, size.height * 0.40f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}
