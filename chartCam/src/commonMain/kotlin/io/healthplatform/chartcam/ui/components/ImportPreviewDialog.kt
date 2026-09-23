/**
 * @file ImportPreviewDialog.kt
 * Interactive dialog for previewing staged import data, selecting categories,
 * choosing batch patients, and resolving conflicts.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.action_confirm_import
import chartcam.chartcam.generated.resources.action_deselect_all
import chartcam.chartcam.generated.resources.action_select_all
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.label_data_categories_to_import
import chartcam.chartcam.generated.resources.label_resolution_strategy
import chartcam.chartcam.generated.resources.patient_import_conflict_format
import chartcam.chartcam.generated.resources.patients_to_ingest_count_format
import chartcam.chartcam.generated.resources.title_import_review_merge
import io.healthplatform.chartcam.models.ConflictResolutionStrategy
import io.healthplatform.chartcam.models.ConflictType
import io.healthplatform.chartcam.models.ImportCategory
import io.healthplatform.chartcam.models.ImportFilterOptions
import io.healthplatform.chartcam.models.ImportPreviewSummary
import io.healthplatform.chartcam.models.fullName
import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.ui.theme.AppSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * Interactive dialog displaying import summary, category toggles, batch patient selection, and conflict resolutions.
 *
 * **State & Side Effects:**
 * Hoists user selection actions through callback parameters. Modifiers apply to the root dialog elements.
 *
 * @param preview The staged import metadata and candidate patients.
 * @param filterOptions Current category filter configuration.
 * @param selectedPatientIds Set of incoming patient IDs currently chosen for import.
 * @param conflictResolutions Map of conflict resolution strategies by patient ID.
 * @param onToggleCategory Callback when a category checkbox is clicked.
 * @param onTogglePatient Callback when an individual patient selection is toggled.
 * @param onToggleSelectAll Callback to toggle selection of all candidate patients.
 * @param onSetResolution Callback when a conflict resolution strategy is selected.
 * @param onDismiss Callback when dismissing the dialog.
 * @param onConfirm Callback when confirming the import.
 */
@Composable
fun ImportPreviewDialog(
    preview: ImportPreviewSummary,
    filterOptions: ImportFilterOptions,
    selectedPatientIds: Set<String>,
    conflictResolutions: Map<String, ConflictResolutionStrategy>,
    onToggleCategory: (ImportCategory, Boolean) -> Unit,
    onTogglePatient: (String, Boolean) -> Unit,
    onToggleSelectAll: (Boolean) -> Unit,
    onSetResolution: (String, ConflictResolutionStrategy) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val cancelText = stringResource(Res.string.cancel)
    val confirmText = stringResource(Res.string.action_confirm_import)
    val titleText = stringResource(Res.string.title_import_review_merge)
    val categoriesLabel = stringResource(Res.string.label_data_categories_to_import)
    val resolutionLabel = stringResource(Res.string.label_resolution_strategy)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                item {
                    Text(
                        text = categoriesLabel,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    ImportCategory.entries.forEach { category ->
                        val isChecked = filterOptions.isCategoryEnabled(category)
                        val categoryName = category.name.replace('_', ' ')
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable(
                                        role = Role.Checkbox,
                                        onClickLabel = categoryName,
                                    ) { onToggleCategory(category, !isChecked) }
                                    .padding(vertical = AppSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null,
                            )
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                stringResource(
                                    Res.string.patients_to_ingest_count_format,
                                    selectedPatientIds.size,
                                    preview.stagedPatients.size,
                                ),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        val allSelected = selectedPatientIds.size == preview.stagedPatients.size
                        val toggleAllText =
                            stringResource(
                                if (allSelected) Res.string.action_deselect_all else Res.string.action_select_all,
                            )
                        TextButton(
                            onClick = { onToggleSelectAll(!allSelected) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Text(toggleAllText)
                        }
                    }
                }

                items(preview.stagedPatients) { stagingItem ->
                    val pid = stagingItem.incomingPatient.id ?: ""
                    val isSelected = selectedPatientIds.contains(pid)
                    val patientName = stagingItem.incomingPatient.fullName
                    val currentResolution = conflictResolutions[pid] ?: stagingItem.resolutionStrategy

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppSpacing.xs),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable(
                                        role = Role.Checkbox,
                                        onClickLabel = patientName,
                                    ) { onTogglePatient(pid, !isSelected) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$patientName (MRN: ${stagingItem.incomingPatient.mrn})",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text =
                                        stringResource(
                                            Res.string.patient_import_conflict_format,
                                            stagingItem.encounterCount,
                                            stagingItem.conflictType.name,
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (stagingItem.conflictType == ConflictType.EXACT_MATCH) {
                                            MaterialTheme.colorScheme.outline
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                )
                            }
                        }

                        if (isSelected && stagingItem.conflictType != ConflictType.EXACT_MATCH) {
                            Column(modifier = Modifier.padding(start = AppSpacing.xl, top = AppSpacing.xs)) {
                                Text(
                                    text = resolutionLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.semantics { heading() },
                                )
                                ConflictResolutionStrategy.entries.forEach { strategy ->
                                    val strategyName = strategy.name.replace('_', ' ')
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .minimumInteractiveComponentSize()
                                                .clickable(
                                                    role = Role.RadioButton,
                                                    onClickLabel = strategyName,
                                                ) { onSetResolution(pid, strategy) },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = currentResolution == strategy,
                                            onClick = null,
                                        )
                                        Text(
                                            text = strategyName,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(cancelText)
            }
        },
    )
}
