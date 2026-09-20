/**
 * @file DicomExportDialogJvmTest.kt
 * Contains declarations for DicomExportDialogJvmTest.kt.
 *
 * JVM Compose UI tests for [DicomExportDialog].
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test suite for [DicomExportDialog].
 */
@OptIn(ExperimentalTestApi::class)
class DicomExportDialogJvmTest {
    /**
     * Tests rendering and interactions of DicomExportDialog.
     */
    @Test
    fun testDicomExportDialogInteractions() {
        io.healthplatform.chartcam.ui
            .setAppLanguage("en")
        runComposeUiTest {
            var confirmed = false
            var anonymizedVal = false
            var dismissed = false

            setContent {
                DicomExportDialog(
                    patientId = "PATIENT-123",
                    modality = "XC",
                    sopClass = "1.2.840.10008.5.1.4.1.1.77.1.1",
                    onDismiss = { dismissed = true },
                    onConfirm = {
                        confirmed = true
                        anonymizedVal = it
                    },
                )
            }

            waitForIdle()
            onAllNodesWithText("PATIENT-123", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()
            onAllNodesWithText("XC", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()

            // Toggle anonymization switch/row
            onAllNodesWithText("De-identify Patient", substring = true, useUnmergedTree = true).onFirst().performClick()

            // Confirm export
            onAllNodesWithText("Export", useUnmergedTree = true).onFirst().performClick()
            assertTrue(confirmed)
            assertTrue(anonymizedVal)
        }
    }

    /**
     * Tests dismissal of DicomExportDialog.
     */
    @Test
    fun testDicomExportDialogDismiss() {
        runComposeUiTest {
            var dismissed = false

            setContent {
                DicomExportDialog(
                    patientId = "PATIENT-123",
                    modality = "XC",
                    sopClass = "1.2.840.10008.5.1.4.1.1.77.1.1",
                    onDismiss = { dismissed = true },
                    onConfirm = {},
                )
            }

            waitForIdle()
            onNodeWithText("Cancel", useUnmergedTree = true).performClick()
            assertTrue(dismissed)
        }
    }
}
