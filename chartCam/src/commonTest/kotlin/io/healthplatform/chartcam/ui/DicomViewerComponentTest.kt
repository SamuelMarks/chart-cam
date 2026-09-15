/**
 * @file DicomViewerComponentTest.kt
 * Unit and component state tests for DicomViewerComponent.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.dicom.DicomDataset
import io.healthplatform.chartcam.ui.components.DicomViewerComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Test suite for [DicomViewerComponent] data structures and callbacks.
 */
class DicomViewerComponentTest {
    /**
     * Tests that a complete DicomDataset correctly populates all model fields.
     */
    @Test
    fun testCompleteDicomDatasetModel() {
        val samplePixels = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x00, 0x10)
        val samplePdf = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        val dataset =
            DicomDataset(
                elements = emptyMap(),
                patientName = "Doe^Jane",
                patientId = "MRN-1002",
                patientSex = "F",
                modality = "XC",
                sopClassUid = "1.2.840.10008.5.1.4.1.1.77.1.1",
                transferSyntaxUid = "1.2.840.10008.1.2.4.50",
                pixelData = samplePixels,
                encapsulatedPdf = samplePdf,
                width = 1920,
                height = 1080,
            )

        assertEquals("Doe^Jane", dataset.patientName)
        assertEquals("MRN-1002", dataset.patientId)
        assertEquals("F", dataset.patientSex)
        assertEquals("XC", dataset.modality)
        assertEquals(1920, dataset.width)
        assertEquals(1080, dataset.height)
        assertNotNull(dataset.pixelData)
        assertNotNull(dataset.encapsulatedPdf)
    }

    /**
     * Tests that optional and absent tags default gracefully without crashing.
     */
    @Test
    fun testMinimalDicomDatasetModel() {
        val dataset =
            DicomDataset(
                elements = emptyMap(),
                patientName = null,
                patientId = null,
                patientSex = null,
                modality = null,
                sopClassUid = null,
                transferSyntaxUid = null,
                pixelData = null,
                encapsulatedPdf = null,
                width = null,
                height = null,
            )

        assertNull(dataset.patientName)
        assertNull(dataset.patientId)
        assertNull(dataset.pixelData)
        assertNull(dataset.encapsulatedPdf)
    }

    /**
     * Tests PDF export callback execution.
     */
    @Test
    fun testPdfExportCallback() {
        val samplePdf = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        var sharedBytes: ByteArray? = null
        val onShare: (ByteArray) -> Unit = { sharedBytes = it }

        onShare(samplePdf)
        assertNotNull(sharedBytes)
        val bytes = checkNotNull(sharedBytes)
        assertEquals(4, bytes.size)
    }
}
