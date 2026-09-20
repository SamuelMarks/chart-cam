/**
 * @file ImportPreviewDialogJvmTest.kt
 * Contains declarations for ImportPreviewDialogJvmTest.kt.
 *
 * JVM Compose UI tests for [ImportPreviewDialog].
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.models.ConflictResolutionStrategy
import io.healthplatform.chartcam.models.ConflictType
import io.healthplatform.chartcam.models.ImportCategory
import io.healthplatform.chartcam.models.ImportFilterOptions
import io.healthplatform.chartcam.models.ImportPreviewSummary
import io.healthplatform.chartcam.models.PatientStagingItem
import io.healthplatform.chartcam.models.createFhirPatient
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test suite for [ImportPreviewDialog].
 */
@OptIn(ExperimentalTestApi::class)
class ImportPreviewDialogJvmTest {
    /**
     * Tests rendering, category toggling, patient selection, and confirmation of ImportPreviewDialog.
     */
    @Test
    fun testImportPreviewDialogInteractions() {
        val p1 = createFhirPatient("p-1", "Alice", "Smith", LocalDate(1985, 5, 20), "MRN-101")
        val staging =
            PatientStagingItem(
                incomingPatient = p1,
                conflictType = ConflictType.EXACT_MATCH,
                resolutionStrategy = ConflictResolutionStrategy.OVERWRITE_LOCAL,
            )
        val preview =
            ImportPreviewSummary(
                totalResources = 5,
                stagedPatients = listOf(staging),
                stagedEncounterCount = 1,
                stagedPhotoCount = 2,
                stagedFormCount = 2,
                hasConflicts = false,
            )

        var dismissed = false
        var confirmed = false
        var toggledCategory: ImportCategory? = null
        var toggledPatient: String? = null
        var selectAllToggled = false
        var resolutionSet = false

        runComposeUiTest {
            setContent {
                ImportPreviewDialog(
                    preview = preview,
                    filterOptions = ImportFilterOptions(),
                    selectedPatientIds = setOf("p-1"),
                    conflictResolutions = mapOf("p-1" to ConflictResolutionStrategy.OVERWRITE_LOCAL),
                    onToggleCategory = { cat, _ -> toggledCategory = cat },
                    onTogglePatient = { id, _ -> toggledPatient = id },
                    onToggleSelectAll = { selectAllToggled = true },
                    onSetResolution = { _, _ -> resolutionSet = true },
                    onDismiss = { dismissed = true },
                    onConfirm = { confirmed = true },
                )
            }

            waitForIdle()
            onNodeWithText("Alice Smith", substring = true, useUnmergedTree = true).assertIsDisplayed()

            // Confirm import
            onNodeWithText("Confirm Import", useUnmergedTree = true).performClick()
            assertTrue(confirmed)
        }
    }

    /**
     * Tests dismissal of ImportPreviewDialog.
     */
    @Test
    fun testImportPreviewDialogDismiss() {
        val preview =
            ImportPreviewSummary(
                totalResources = 0,
                stagedPatients = emptyList(),
                stagedEncounterCount = 0,
                stagedPhotoCount = 0,
                stagedFormCount = 0,
                hasConflicts = false,
            )
        var dismissed = false

        runComposeUiTest {
            setContent {
                ImportPreviewDialog(
                    preview = preview,
                    filterOptions = ImportFilterOptions(),
                    selectedPatientIds = emptySet(),
                    conflictResolutions = emptyMap(),
                    onToggleCategory = { _, _ -> },
                    onTogglePatient = { _, _ -> },
                    onToggleSelectAll = {},
                    onSetResolution = { _, _ -> },
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
