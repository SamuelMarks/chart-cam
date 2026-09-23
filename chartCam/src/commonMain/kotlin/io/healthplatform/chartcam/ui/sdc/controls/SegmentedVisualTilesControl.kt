/**
 * @file SegmentedVisualTilesControl.kt
 * Contains declarations for SegmentedVisualTilesControl.kt.
 *
 * Segmented visual choice tiles with elevation, high-contrast borders, and single/multi-choice support.
 */
package io.healthplatform.chartcam.ui.sdc.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_choice_tile_selected
import chartcam.chartcam.generated.resources.cd_choice_tile_unselected
import chartcam.chartcam.generated.resources.not_answered
import io.healthplatform.chartcam.ui.components.FormLabel
import io.healthplatform.chartcam.ui.theme.AppSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * Segmented visual choice tiles with elevation, high-contrast borders, and single/multi-choice support.
 *
 * @param selectedOptions The list of currently selected option strings.
 * @param options The complete list of selectable option strings.
 * @param onOptionToggled Callback invoked when an option tile is clicked.
 * @param label The localized title/label for the questionnaire item.
 * @param isMultiSelect Whether multiple options can be chosen simultaneously.
 * @param isRequired Whether an answer is required.
 * @param isError Whether this control is in an error state.
 * @param errorMessage Localized error message to announce when [isError] is true.
 * @param readOnly Whether the control is rendered in read-only / review mode.
 * @param modifier The modifier to apply to the root layout.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SegmentedVisualTilesControl(
    selectedOptions: List<String>,
    options: List<String>,
    onOptionToggled: (String) -> Unit,
    label: String,
    isMultiSelect: Boolean,
    isRequired: Boolean,
    isError: Boolean,
    errorMessage: String?,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
) {
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
            modifier = Modifier.padding(bottom = AppSpacing.sm),
        )

        if (readOnly) {
            if (selectedOptions.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().testTag("SegmentedTilesReadOnly"),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    selectedOptions.forEach { opt ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(Res.string.not_answered),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        } else {
            FlowRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (!isMultiSelect) Modifier.selectableGroup() else Modifier)
                        .testTag("SegmentedTilesGroup"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    val isSelected = selectedOptions.contains(option)
                    val tileDesc =
                        if (isSelected) {
                            stringResource(Res.string.cd_choice_tile_selected, option)
                        } else {
                            stringResource(Res.string.cd_choice_tile_unselected, option)
                        }

                    OutlinedCard(
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("SegmentedTile_$option")
                                .clickable(role = if (isMultiSelect) Role.Checkbox else Role.RadioButton) {
                                    onOptionToggled(option)
                                }.semantics(mergeDescendants = true) {
                                    contentDescription = tileDesc
                                    selected = isSelected
                                },
                        shape = MaterialTheme.shapes.small,
                        colors =
                            CardDefaults.outlinedCardColors(
                                containerColor =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                            ),
                        border =
                            if (isSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            },
                        elevation =
                            CardDefaults.outlinedCardElevation(
                                defaultElevation = if (isSelected) 3.dp else 1.dp,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AppSpacing.moderate, vertical = AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector =
                                    if (isSelected) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Outlined.Circle
                                    },
                                contentDescription = null,
                                tint =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
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
 * Convenience overload for [SegmentedVisualTilesControl] defaulting validation and error states.
 *
 * @param selectedOptions The subset of currently selected option keys/labels.
 * @param options The full collection of available option labels.
 * @param onOptionToggled Callback invoked with the toggled option label.
 * @param label The localized title/label for the questionnaire item.
 * @param isMultiSelect Whether multiple selections are permitted.
 * @param readOnly Whether the control is rendered in read-only / review mode.
 * @param modifier The modifier to apply to the root layout.
 */
@Composable
fun SegmentedVisualTilesControl(
    selectedOptions: List<String>,
    options: List<String>,
    onOptionToggled: (String) -> Unit,
    label: String,
    isMultiSelect: Boolean = false,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    SegmentedVisualTilesControl(
        selectedOptions = selectedOptions,
        options = options,
        onOptionToggled = onOptionToggled,
        label = label,
        isMultiSelect = isMultiSelect,
        isRequired = false,
        isError = false,
        errorMessage = null,
        readOnly = readOnly,
        modifier = modifier,
    )
}
