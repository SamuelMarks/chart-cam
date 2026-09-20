/**
 * @file DicomViewerComponentJvmTest.kt
 * Contains declarations for DicomViewerComponentJvmTest.kt.
 *
 * JVM Compose UI tests for [DicomViewerComponent].
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.dicom.DicomDataset
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for [DicomViewerComponent].
 */
@OptIn(ExperimentalTestApi::class)
class DicomViewerComponentJvmTest {
    /**
     * Tests rendering metadata, image, and PDF share callbacks.
     */
    @Test
    fun testDicomViewerComponentRendering() {
        setAppLanguage("en")
        val samplePdf = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        val dataset =
            DicomDataset(
                elements = emptyMap(),
                patientName = "Doe^John",
                patientId = "MRN-101",
                patientSex = "M",
                modality = "DOC",
                sopClassUid = "1.2.840.10008.5.1.4.1.1.104.1",
                encapsulatedPdf = samplePdf,
            )

        var sharedPdf: ByteArray? = null

        runComposeUiTest {
            setContent {
                DicomViewerComponent(
                    dataset = dataset,
                    onSharePdf = { sharedPdf = it },
                )
            }

            waitForIdle()
            onNodeWithText("Doe^John", substring = true, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithText("MRN-101", substring = true, useUnmergedTree = true).assertIsDisplayed()

            // Click share PDF button
            onNodeWithText("Export / Share PDF", useUnmergedTree = true).performClick()
            assertEquals(samplePdf, sharedPdf)
        }
    }
}
