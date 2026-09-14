/**
 * @file DicomExportDialog.kt
 * Dialog component for configuring and triggering DICOM PACS exports.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.dicom_anonymize_patient
import chartcam.chartcam.generated.resources.dicom_anonymize_patient_desc
import chartcam.chartcam.generated.resources.dicom_anonymous_patient
import chartcam.chartcam.generated.resources.dicom_metadata_summary_title
import chartcam.chartcam.generated.resources.dicom_modality
import chartcam.chartcam.generated.resources.dicom_patient_id
import chartcam.chartcam.generated.resources.dicom_sop_class
import chartcam.chartcam.generated.resources.export
import io.healthplatform.chartcam.ui.theme.AppSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * Dialog presenting DICOM PACS export metadata summary with an anonymization toggle.
 *
 * @param patientId Displayed Patient ID or MRN string.
 * @param modality DICOM Modality code (e.g. "XC", "DOC").
 * @param sopClass Target DICOM SOP Class name or UID.
 * @param onDismiss Request to dismiss the dialog without exporting.
 * @param onConfirm Export confirmation callback passing whether anonymization was requested.
 */
@Composable
fun DicomExportDialog(
    patientId: String,
    modality: String,
    sopClass: String,
    onDismiss: () -> Unit,
    onConfirm: (anonymize: Boolean) -> Unit,
) {
    var anonymize by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.dicom_metadata_summary_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                val anonymousStr = stringResource(Res.string.dicom_anonymous_patient)
                Text(
                    text =
                        stringResource(
                            Res.string.dicom_patient_id,
                            if (anonymize) anonymousStr else patientId,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(Res.string.dicom_modality, modality),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(Res.string.dicom_sop_class, sopClass),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .toggleable(
                                value = anonymize,
                                role = Role.Switch,
                                onValueChange = { anonymize = it },
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = AppSpacing.sm)) {
                        Text(
                            text = stringResource(Res.string.dicom_anonymize_patient),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(Res.string.dicom_anonymize_patient_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = anonymize,
                        onCheckedChange = null,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(anonymize) },
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(stringResource(Res.string.export))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
