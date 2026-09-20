/**
 * @file DicomViewerComponent.kt
 * Composable component for inspecting and rendering DICOM datasets locally.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.action_export_share_pdf
import chartcam.chartcam.generated.resources.cd_dicom_pixel_data
import chartcam.chartcam.generated.resources.label_dicom_dimensions_format
import chartcam.chartcam.generated.resources.label_dicom_encapsulated_pdf_format
import chartcam.chartcam.generated.resources.label_dicom_id_format
import chartcam.chartcam.generated.resources.label_dicom_modality_format
import chartcam.chartcam.generated.resources.label_dicom_patient_format
import chartcam.chartcam.generated.resources.label_dicom_pixel_data_size_format
import chartcam.chartcam.generated.resources.label_dicom_pixel_frame_hint
import chartcam.chartcam.generated.resources.label_dicom_sex_format
import chartcam.chartcam.generated.resources.label_dicom_transfer_syntax_format
import chartcam.chartcam.generated.resources.title_dicom_dataset_inspector
import io.healthplatform.chartcam.dicom.DicomDataset
import io.healthplatform.chartcam.ui.theme.AppSpacing
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource

private const val MIN_ZOOM = 1.0f
private const val MAX_ZOOM = 5.0f

/**
 * Renders a DICOM metadata summary, image raster with pan/zoom, and PDF document actions.
 *
 * **State & Side Effects:**
 * Maintains local pan and zoom gesture offsets.
 *
 * @param dataset The decoded [DicomDataset] to display.
 * @param modifier The modifier to apply to the container card.
 * @param onSharePdf Optional callback when the user requests to share an encapsulated PDF.
 */
@Composable
fun DicomViewerComponent(
    dataset: DicomDataset,
    modifier: Modifier = Modifier,
    onSharePdf: ((ByteArray) -> Unit)? = null,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val imageBitmap: ImageBitmap? =
        remember(dataset.pixelData) {
            dataset.pixelData?.let {
                runCatching { it.decodeToImageBitmap() }.getOrNull()
            }
        }

    Card(
        modifier = modifier.fillMaxWidth().padding(AppSpacing.sm).testTag("DicomViewerCard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Text(
                text = stringResource(Res.string.title_dicom_dataset_inspector),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            DicomMetadataSummary(dataset)

            if (imageBitmap != null) {
                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text(
                    text = stringResource(Res.string.label_dicom_pixel_frame_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(AppSpacing.sm))
                            .background(MaterialTheme.colorScheme.scrim)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }.testTag("DicomImageCanvas"),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = stringResource(Res.string.cd_dicom_pixel_data),
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY,
                                ),
                    )
                }
            }

            if (dataset.encapsulatedPdf != null) {
                Spacer(modifier = Modifier.height(AppSpacing.md))
                EncapsulatedPdfSection(dataset.encapsulatedPdf, onSharePdf)
            }
        }
    }
}

/**
 * Renders patient demographic and technical metadata attributes from the DICOM dataset.
 *
 * @param dataset The parsed DICOM dataset.
 */
@Composable
private fun DicomMetadataSummary(dataset: DicomDataset) {
    Text(
        text = stringResource(Res.string.label_dicom_patient_format, dataset.patientName ?: "Unknown"),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = stringResource(Res.string.label_dicom_id_format, dataset.patientId ?: "N/A"),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = stringResource(Res.string.label_dicom_sex_format, dataset.patientSex ?: "O"),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = stringResource(Res.string.label_dicom_modality_format, dataset.modality ?: "XC"),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = stringResource(Res.string.label_dicom_transfer_syntax_format, dataset.transferSyntaxUid ?: "N/A"),
        style = MaterialTheme.typography.bodySmall,
    )
    if (dataset.width != null && dataset.height != null) {
        Text(
            text = stringResource(Res.string.label_dicom_dimensions_format, dataset.width, dataset.height),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (dataset.pixelData != null) {
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = stringResource(Res.string.label_dicom_pixel_data_size_format, dataset.pixelData.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/**
 * Renders encapsulated PDF document details and sharing action button.
 *
 * @param pdfData The raw bytes of the encapsulated PDF.
 * @param onSharePdf Optional callback to trigger sharing.
 */
@Composable
private fun EncapsulatedPdfSection(
    pdfData: ByteArray,
    onSharePdf: ((ByteArray) -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("DicomPdfCard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.moderate)) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(AppSpacing.xl),
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = stringResource(Res.string.label_dicom_encapsulated_pdf_format, pdfData.size),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { heading() },
            )
            if (onSharePdf != null) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Button(
                    onClick = { onSharePdf(pdfData) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("SharePdfButton"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(AppSpacing.sm))
                    Text(stringResource(Res.string.action_export_share_pdf))
                }
            }
        }
    }
}
