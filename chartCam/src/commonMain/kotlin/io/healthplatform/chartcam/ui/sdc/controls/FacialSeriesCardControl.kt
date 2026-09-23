/**
 * @file FacialSeriesCardControl.kt
 * Contains declarations for FacialSeriesCardControl.kt.
 *
 * Dedicated SDC Form control card for 3-angle facial and cornea/nose profile photography series.
 */
package io.healthplatform.chartcam.ui.sdc.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.start_facial_series
import chartcam.chartcam.generated.resources.step_front_view
import chartcam.chartcam.generated.resources.step_left_profile_cornea
import chartcam.chartcam.generated.resources.step_right_profile_cornea
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.ui.theme.AppSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * Interactive card rendering the 3-angle facial and cornea profile capture series.
 *
 * **State & Side Effects:**
 * Dispatches capture requests via [onCaptureSeries] or [onCaptureSingle].
 *
 * @param title The group heading title.
 * @param items The list of 3 child questionnaire items in the group.
 * @param answers The map of current form answers.
 * @param existingAttachments List of attachment items from existing questionnaire responses.
 * @param readOnly True if the form is in read-only mode.
 * @param onCaptureSeries Callback invoked to initiate the full 3-step capture sequence.
 * @param onCaptureSingle Callback invoked to capture or retake a specific single view angle.
 * @param modifier The modifier to be applied to the root card.
 */
@Composable
fun FacialSeriesCardControl(
    title: String,
    items: List<Questionnaire.Item>,
    answers: Map<String, Any?>,
    existingAttachments: List<DocumentReference>,
    readOnly: Boolean,
    onCaptureSeries: () -> Unit,
    onCaptureSingle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultTitles =
        listOf(
            stringResource(Res.string.step_left_profile_cornea),
            stringResource(Res.string.step_front_view),
            stringResource(Res.string.step_right_profile_cornea),
        )

    val slots =
        items.mapIndexed { index, item ->
            val linkId = item.linkId.value ?: ""
            val defaultTitle = if (index < defaultTitles.size) defaultTitles[index] else "View $index"
            val rawText = item.text?.value
            val itemText = if (rawText != null && rawText.isNotBlank()) rawText else defaultTitle
            val hasAnswer = answers[linkId] != null
            val hasExisting =
                existingAttachments.any { doc ->
                    val ctx = doc.context
                    val rel = if (ctx != null) ctx.related else null
                    rel != null && rel.any { ref -> ref.reference?.value == linkId }
                }
            FacialSlotInfo(
                linkId = linkId,
                title = itemText,
                isCaptured = hasAnswer || hasExisting,
            )
        }

    val completedCount = slots.count { it.isCaptured }
    val totalCount = slots.size.coerceAtLeast(3)

    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.sm)
                .semantics {
                    contentDescription = "$title: $completedCount of $totalCount completed"
                    heading()
                },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Surface(
                    color =
                        if (completedCount == totalCount) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "$completedCount/$totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (completedCount == totalCount) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                slots.forEach { slot ->
                    FacialSlotTile(
                        slot = slot,
                        readOnly = readOnly,
                        onCapture = { onCaptureSingle(slot.linkId) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (!readOnly && completedCount < totalCount) {
                Spacer(modifier = Modifier.height(AppSpacing.md))
                Button(
                    onClick = onCaptureSeries,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("StartFacialSeriesButton"),
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.padding(end = AppSpacing.sm),
                    )
                    Text(stringResource(Res.string.start_facial_series))
                }
            }
        }
    }
}

/**
 * Individual angle tile inside [FacialSeriesCardControl].
 *
 * @param slot Information about the view slot.
 * @param readOnly Whether the form is read-only.
 * @param onCapture Callback to capture or retake this specific view.
 * @param modifier The modifier to be applied to this slot tile.
 */
@Composable
fun FacialSlotTile(
    slot: FacialSlotInfo,
    readOnly: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusDesc = if (slot.isCaptured) "Captured" else "Pending"
    val slotDesc = "${slot.title}: $statusDesc"

    OutlinedCard(
        modifier =
            modifier
                .testTag("FacialSlotTile_${slot.linkId}")
                .semantics {
                    contentDescription = slotDesc
                },
        border =
            BorderStroke(
                1.dp,
                if (slot.isCaptured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        colors =
            CardDefaults.outlinedCardColors(
                containerColor =
                    if (slot.isCaptured) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.sm).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(
                            color =
                                if (slot.isCaptured) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else {
                                    Color.Transparent
                                },
                            shape = MaterialTheme.shapes.small,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (slot.isCaptured) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Text(
                text = slot.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )

            if (!readOnly) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                OutlinedButton(
                    onClick = onCapture,
                    modifier =
                        Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("CaptureSlotButton_${slot.linkId}"),
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 4.dp,
                            vertical = 2.dp,
                        ),
                ) {
                    Text(
                        text = if (slot.isCaptured) "Retake" else "Capture",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
