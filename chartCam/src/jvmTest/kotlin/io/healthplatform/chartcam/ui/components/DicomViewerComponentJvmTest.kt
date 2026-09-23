/**
 * @file DicomViewerComponentJvmTest.kt
 * Contains declarations for DicomViewerComponentJvmTest.kt.
 *
 * JVM Compose UI tests for [DicomViewerComponent].
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
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
     * Valid 4x4 PNG byte array used for image rendering tests.
     */
    private val validPngBytes =
        byteArrayOf(
            (137).toByte(),
            80,
            78,
            71,
            13,
            10,
            26,
            10,
            0,
            0,
            0,
            13,
            73,
            72,
            68,
            82,
            0,
            0,
            0,
            4,
            0,
            0,
            0,
            4,
            8,
            2,
            0,
            0,
            0,
            38,
            (147).toByte(),
            9,
            41,
            0,
            0,
            0,
            19,
            73,
            68,
            65,
            84,
            120,
            (156).toByte(),
            99,
            100,
            96,
            (248).toByte(),
            (207).toByte(),
            0,
            3,
            76,
            112,
            22,
            94,
            14,
            0,
            49,
            (211).toByte(),
            1,
            7,
            (214).toByte(),
            (206).toByte(),
            (231).toByte(),
            (150).toByte(),
            0,
            0,
            0,
            0,
            73,
            69,
            78,
            68,
            (174).toByte(),
            66,
            96,
            (130).toByte(),
        )

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

    /**
     * Tests rendering when pixel data contains a valid image with touch/pinch interactions.
     */
    @Test
    fun testDicomViewerComponentWithPixelData() {
        setAppLanguage("en")
        val dataset =
            DicomDataset(
                elements = emptyMap(),
                patientName = null,
                patientId = null,
                patientSex = null,
                modality = null,
                transferSyntaxUid = "1.2.840.10008.1.2.4.50",
                width = 512,
                height = 512,
                pixelData = validPngBytes,
                encapsulatedPdf = null,
            )

        runComposeUiTest {
            setContent {
                DicomViewerComponent(
                    dataset = dataset,
                    modifier = Modifier,
                    onSharePdf = null,
                )
            }

            waitForIdle()

            // Verify default metadata labels for null values
            onNodeWithText("Unknown", substring = true, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithText("N/A", substring = true, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithText("512 x 512", substring = true, useUnmergedTree = true).assertIsDisplayed()

            // Verify image canvas exists and perform gestures
            onNodeWithTag("DicomImageCanvas").assertIsDisplayed()
            onNodeWithTag("DicomImageCanvas").performTouchInput {
                swipe(
                    start = Offset(centerX, centerY),
                    end = Offset(centerX + 50f, centerY + 50f),
                    durationMillis = 200,
                )
            }
            waitForIdle()
        }
    }

    /**
     * Tests invalid pixel data and asymmetric dimension branches.
     */
    @Test
    fun testDicomViewerComponentCorruptedPixelDataAndDimensionBranches() {
        setAppLanguage("en")
        val datasetOnlyWidth =
            DicomDataset(
                elements = emptyMap(),
                patientName = "Test^Width",
                width = 256,
                height = null,
                pixelData = byteArrayOf(1, 2, 3, 4, 5), // Corrupted pixel data triggering decode failure
            )

        runComposeUiTest {
            setContent {
                DicomViewerComponent(
                    dataset = datasetOnlyWidth,
                )
            }

            waitForIdle()
            onNodeWithText("Test^Width", substring = true, useUnmergedTree = true).assertIsDisplayed()
            // Canvas should not exist since decoding fails
            onNodeWithTag("DicomImageCanvas").assertDoesNotExist()
        }
    }

    /**
     * Tests encapsulated PDF section when onSharePdf callback is null.
     */
    @Test
    fun testDicomViewerComponentEncapsulatedPdfNullShare() {
        setAppLanguage("en")
        val samplePdf = byteArrayOf(1, 2, 3, 4)
        val dataset =
            DicomDataset(
                elements = emptyMap(),
                patientName = "Smith^Jane",
                encapsulatedPdf = samplePdf,
            )

        runComposeUiTest {
            setContent {
                DicomViewerComponent(
                    dataset = dataset,
                    onSharePdf = null,
                )
            }

            waitForIdle()
            onNodeWithTag("DicomPdfCard").assertIsDisplayed()
            // Verify share button is not present
            onNodeWithTag("SharePdfButton").assertDoesNotExist()
        }
    }

    /**
     * Tests recomposition of DicomViewerComponent with mutated parameters.
     */
    @Test
    fun testDicomViewerComponentRecomposition() {
        setAppLanguage("en")
        val dataset1 =
            DicomDataset(
                elements = emptyMap(),
                patientName = "Recompose^One",
                encapsulatedPdf = byteArrayOf(1, 2, 3),
            )
        val dataset2 =
            DicomDataset(
                elements = emptyMap(),
                patientName = "Recompose^Two",
                encapsulatedPdf = byteArrayOf(4, 5, 6),
            )

        runComposeUiTest {
            val datasetState = androidx.compose.runtime.mutableStateOf(dataset1)
            val modifierState = androidx.compose.runtime.mutableStateOf<Modifier>(Modifier)
            val onShareState = androidx.compose.runtime.mutableStateOf<((ByteArray) -> Unit)?>({ _ -> })

            setContent {
                DicomViewerComponent(
                    dataset = datasetState.value,
                    modifier = modifierState.value,
                    onSharePdf = onShareState.value,
                )
            }
            waitForIdle()

            // Mutate dataset
            datasetState.value = dataset2
            waitForIdle()

            // Mutate modifier
            modifierState.value = Modifier.then(Modifier)
            waitForIdle()

            // Mutate onSharePdf
            onShareState.value = null
            waitForIdle()

            onShareState.value = { println("shared: $it") }
            waitForIdle()
        }
    }

    /**
     * Tests skipping recomposition of DicomViewerComponent when inputs are unchanged.
     */
    @Test
    fun testDicomViewerComponentRecompositionSkipping() {
        setAppLanguage("en")
        val dataset =
            DicomDataset(
                elements = emptyMap(),
                patientName = "Skip^Patient",
                encapsulatedPdf = byteArrayOf(1, 2),
            )

        runComposeUiTest {
            val outerTrigger = androidx.compose.runtime.mutableStateOf(0)

            setContent {
                val dummy = outerTrigger.value
                DicomViewerComponent(
                    dataset = dataset,
                )
            }
            waitForIdle()

            outerTrigger.value++
            waitForIdle()
        }
    }

    /**
     * Tests direct recomposition and skipping for [EncapsulatedPdfSection].
     */
    @Test
    fun testEncapsulatedPdfSectionDirect() {
        setAppLanguage("en")
        val pdf1 = byteArrayOf(1, 2)
        val pdf2 = byteArrayOf(3, 4)

        runComposeUiTest {
            val outerTrigger = androidx.compose.runtime.mutableStateOf(0)
            val pdfState = androidx.compose.runtime.mutableStateOf(pdf1)
            val onShareState = androidx.compose.runtime.mutableStateOf<((ByteArray) -> Unit)?>({ _ -> })

            setContent {
                val dummy = outerTrigger.value
                EncapsulatedPdfSection(
                    pdfData = pdfState.value,
                    onSharePdf = onShareState.value,
                )
            }
            waitForIdle()

            // Skipping
            outerTrigger.value++
            waitForIdle()

            // Mutate pdfData
            pdfState.value = pdf2
            waitForIdle()

            // Mutate onSharePdf
            onShareState.value = null
            waitForIdle()

            onShareState.value = { println("shared: $it") }
            waitForIdle()
        }
    }
}
