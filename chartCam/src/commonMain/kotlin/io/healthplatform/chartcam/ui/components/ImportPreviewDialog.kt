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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.models.ConflictResolutionStrategy
import io.healthplatform.chartcam.models.ConflictType
import io.healthplatform.chartcam.models.ImportCategory
import io.healthplatform.chartcam.models.ImportFilterOptions
import io.healthplatform.chartcam.models.ImportPreviewSummary
import io.healthplatform.chartcam.models.familyName
import io.healthplatform.chartcam.models.givenName
import io.healthplatform.chartcam.models.mrn

/**
 * Interactive dialog displaying import summary, category toggles, batch patient selection, and conflict resolutions.
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Review & Merge",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = "Data Categories to Import:",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    ImportCategory.entries.forEach { category ->
                        val isChecked = filterOptions.isCategoryEnabled(category)
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleCategory(category, !isChecked) }
                                    .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggleCategory(category, it) },
                            )
                            Text(
                                text = category.name.replace('_', ' '),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Patients to Ingest (${selectedPatientIds.size}/${preview.stagedPatients.size}):",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        val allSelected = selectedPatientIds.size == preview.stagedPatients.size
                        TextButton(onClick = { onToggleSelectAll(!allSelected) }) {
                            Text(if (allSelected) "Deselect All" else "Select All")
                        }
                    }
                }

                items(preview.stagedPatients) { stagingItem ->
                    val pid = stagingItem.incomingPatient.id ?: ""
                    val isSelected = selectedPatientIds.contains(pid)
                    val patientName =
                        stagingItem.incomingPatient.name.firstOrNull()?.let {
                            "${it.givenName} ${it.familyName}"
                        } ?: "Unknown Patient"
                    val currentResolution = conflictResolutions[pid] ?: stagingItem.resolutionStrategy

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onTogglePatient(pid, !isSelected) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onTogglePatient(pid, it) },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$patientName (MRN: ${stagingItem.incomingPatient.mrn})",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text =
                                        "Visits: ${stagingItem.encounterCount} | " +
                                            "Conflict: ${stagingItem.conflictType.name}",
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
                            Column(modifier = Modifier.padding(start = 32.dp, top = 4.dp)) {
                                Text(
                                    text = "Resolution Strategy:",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                ConflictResolutionStrategy.entries.forEach { strategy ->
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable { onSetResolution(pid, strategy) },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = currentResolution == strategy,
                                            onClick = { onSetResolution(pid, strategy) },
                                        )
                                        Text(
                                            text = strategy.name.replace('_', ' '),
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
            Button(onClick = onConfirm) {
                Text("Confirm Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
