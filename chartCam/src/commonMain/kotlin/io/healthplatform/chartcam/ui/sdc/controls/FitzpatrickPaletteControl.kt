/**
 * @file FitzpatrickPaletteControl.kt
 * Contains declarations for FitzpatrickPaletteControl.kt.
 *
 * Interactive dermatological Fitzpatrick skin phototyping swatch palette control.
 */
package io.healthplatform.chartcam.ui.sdc.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_fitzpatrick_swatch
import chartcam.chartcam.generated.resources.fitzpatrick_desc_1
import chartcam.chartcam.generated.resources.fitzpatrick_desc_2
import chartcam.chartcam.generated.resources.fitzpatrick_desc_3
import chartcam.chartcam.generated.resources.fitzpatrick_desc_4
import chartcam.chartcam.generated.resources.fitzpatrick_desc_5
import chartcam.chartcam.generated.resources.fitzpatrick_desc_6
import chartcam.chartcam.generated.resources.fitzpatrick_type_1
import chartcam.chartcam.generated.resources.fitzpatrick_type_2
import chartcam.chartcam.generated.resources.fitzpatrick_type_3
import chartcam.chartcam.generated.resources.fitzpatrick_type_4
import chartcam.chartcam.generated.resources.fitzpatrick_type_5
import chartcam.chartcam.generated.resources.fitzpatrick_type_6
import chartcam.chartcam.generated.resources.not_answered
import io.healthplatform.chartcam.models.FitzpatrickScaleDefaults
import io.healthplatform.chartcam.models.FitzpatrickSkinType
import io.healthplatform.chartcam.ui.components.FormLabel
import io.healthplatform.chartcam.ui.theme.AppSpacing
import io.healthplatform.chartcam.ui.theme.calculateContrastRatio
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Returns the localized title [StringResource] for a given Fitzpatrick phototype (1 to 6).
 *
 * @param type The type number.
 * @return The corresponding title string resource.
 */
fun getFitzpatrickTitleResource(type: Int): StringResource =
    when (type) {
        1 -> Res.string.fitzpatrick_type_1
        2 -> Res.string.fitzpatrick_type_2
        3 -> Res.string.fitzpatrick_type_3
        4 -> Res.string.fitzpatrick_type_4
        5 -> Res.string.fitzpatrick_type_5
        else -> Res.string.fitzpatrick_type_6
    }

/**
 * Returns the localized description [StringResource] for a given Fitzpatrick phototype (1 to 6).
 *
 * @param type The type number.
 * @return The corresponding description string resource.
 */
fun getFitzpatrickDescResource(type: Int): StringResource =
    when (type) {
        1 -> Res.string.fitzpatrick_desc_1
        2 -> Res.string.fitzpatrick_desc_2
        3 -> Res.string.fitzpatrick_desc_3
        4 -> Res.string.fitzpatrick_desc_4
        5 -> Res.string.fitzpatrick_desc_5
        else -> Res.string.fitzpatrick_desc_6
    }

/**
 * Parses a hexadecimal RGB color string (e.g. "#F8D9C8") into a Compose [Color].
 *
 * @param hex Color string.
 * @return The parsed Color object.
 */
fun parseHexColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    val rgb = clean.toLongOrNull(16) ?: 0xFFFFFFFF
    return Color(0xFF000000 or rgb)
}

/**
 * Interactive dermatological Fitzpatrick skin phototyping swatch palette control.
 *
 * @param selectedType The currently selected [FitzpatrickSkinType], or null.
 * @param onTypeSelected Callback invoked when a phototype is selected.
 * @param label The localized title/label for the questionnaire item.
 * @param isRequired Whether an answer is required.
 * @param isError Whether this control is in an error state.
 * @param errorMessage Localized error message to announce when [isError] is true.
 * @param readOnly Whether the control is rendered in read-only / review mode.
 * @param modifier The modifier to apply to the root layout.
 */
@Composable
fun FitzpatrickPaletteControl(
    selectedType: FitzpatrickSkinType?,
    onTypeSelected: (FitzpatrickSkinType) -> Unit,
    label: String,
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
                .padding(vertical = 8.dp)
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
            modifier = Modifier.padding(bottom = 6.dp),
        )

        if (readOnly) {
            if (selectedType != null) {
                val title = stringResource(getFitzpatrickTitleResource(selectedType.type))
                val desc = stringResource(getFitzpatrickDescResource(selectedType.type))
                val swatchColor = parseHexColor(selectedType.hexColor)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().testTag("FitzpatrickReadOnly"),
                ) {
                    Row(
                        modifier = Modifier.padding(AppSpacing.moderate),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .background(swatchColor, CircleShape),
                        )
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .testTag("FitzpatrickPaletteGroup"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FitzpatrickScaleDefaults.ALL_TYPES.forEach { item ->
                    val isSelected = selectedType?.type == item.type
                    val title = stringResource(getFitzpatrickTitleResource(item.type))
                    val desc = stringResource(getFitzpatrickDescResource(item.type))
                    val swatchColor = parseHexColor(item.hexColor)
                    val cardDescription = stringResource(Res.string.cd_fitzpatrick_swatch, title, desc)

                    OutlinedCard(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("FitzpatrickOption_${item.type}")
                                .clickable(role = Role.RadioButton) { onTypeSelected(item) }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = cardDescription
                                    selected = isSelected
                                },
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            CardDefaults.outlinedCardColors(
                                containerColor =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
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
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AppSpacing.moderate, vertical = AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.moderate),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .background(swatchColor, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    val contrastWithWhite = calculateContrastRatio(Color.White, swatchColor)
                                    val contrastWithBlack = calculateContrastRatio(Color.Black, swatchColor)
                                    val checkTint =
                                        if (contrastWithWhite >= contrastWithBlack) Color.White else Color.Black
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = checkTint,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.semantics { heading() },
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Convenience overload for [FitzpatrickPaletteControl] defaulting validation and error states.
 *
 * @param selectedType The currently selected [FitzpatrickSkinType], or null.
 * @param onTypeSelected Callback invoked when a phototype is selected.
 * @param label The localized title/label for the questionnaire item.
 * @param readOnly Whether the control is rendered in read-only / review mode.
 * @param modifier The modifier to apply to the root layout.
 */
@Composable
fun FitzpatrickPaletteControl(
    selectedType: FitzpatrickSkinType?,
    onTypeSelected: (FitzpatrickSkinType) -> Unit,
    label: String,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    FitzpatrickPaletteControl(
        selectedType = selectedType,
        onTypeSelected = onTypeSelected,
        label = label,
        isRequired = false,
        isError = false,
        errorMessage = null,
        readOnly = readOnly,
        modifier = modifier,
    )
}
