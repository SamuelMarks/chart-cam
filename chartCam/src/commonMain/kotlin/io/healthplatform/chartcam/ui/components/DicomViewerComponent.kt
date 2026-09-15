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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.dicom.DicomDataset
import org.jetbrains.compose.resources.decodeToImageBitmap

private const val MIN_ZOOM = 1.0f
private const val MAX_ZOOM = 5.0f

/**
 * Renders a DICOM metadata summary, image raster with pan/zoom, and PDF document actions.
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
        modifier = modifier.fillMaxWidth().padding(8.dp).testTag("DicomViewerCard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "DICOM Dataset Inspector",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            DicomMetadataSummary(dataset)

            if (imageBitmap != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Pixel Frame (Pinch to Zoom & Drag to Pan)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
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
                        contentDescription = "DICOM Pixel Data",
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
                Spacer(modifier = Modifier.height(16.dp))
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
        text = "Patient: ${dataset.patientName ?: "Unknown"}",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = "ID: ${dataset.patientId ?: "N/A"}",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = "Sex: ${dataset.patientSex ?: "O"}",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = "Modality: ${dataset.modality ?: "XC"}",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = "Transfer Syntax: ${dataset.transferSyntaxUid ?: "N/A"}",
        style = MaterialTheme.typography.bodySmall,
    )
    if (dataset.width != null && dataset.height != null) {
        Text(
            text = "Dimensions: ${dataset.width} x ${dataset.height}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (dataset.pixelData != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Pixel Data: ${dataset.pixelData.size} bytes",
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
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Encapsulated PDF Document: ${pdfData.size} bytes",
                style = MaterialTheme.typography.bodySmall,
            )
            if (onSharePdf != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onSharePdf(pdfData) },
                    modifier = Modifier.fillMaxWidth().testTag("SharePdfButton"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Export / Share PDF")
                }
            }
        }
    }
}
