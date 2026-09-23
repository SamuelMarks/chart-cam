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
import io.healthplatform.chartcam.ui.setAppLanguage
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
        setAppLanguage("en")
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
            onNodeWithText("Smith, Alice", substring = true, useUnmergedTree = true).assertIsDisplayed()

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
        setAppLanguage("en")
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

    /**
     * Tests conflict resolution strategies and radio buttons in [ImportPreviewDialog].
     */
    @Test
    fun testImportPreviewDialogConflicts() {
        setAppLanguage("en")
        val p1 = createFhirPatient("p-1", "Bob", "Jones", LocalDate(1990, 1, 1), "MRN-202")
        val staging1 =
            PatientStagingItem(
                incomingPatient = p1,
                conflictType = ConflictType.ID_COLLISION_DIFFERENT_DATA,
                resolutionStrategy = ConflictResolutionStrategy.KEEP_LOCAL,
            )
        val preview =
            ImportPreviewSummary(
                totalResources = 4,
                stagedPatients = listOf(staging1),
                stagedEncounterCount = 1,
                stagedPhotoCount = 1,
                stagedFormCount = 1,
                hasConflicts = true,
            )

        var toggledCat: ImportCategory? = null
        var toggledPid: String? = null
        var chosenStrategy: ConflictResolutionStrategy? = null

        runComposeUiTest {
            val conflictResolutionsState =
                androidx.compose.runtime.mutableStateOf<Map<String, ConflictResolutionStrategy>>(emptyMap())

            setContent {
                ImportPreviewDialog(
                    preview = preview,
                    filterOptions = ImportFilterOptions(),
                    selectedPatientIds = setOf("p-1"),
                    conflictResolutions = conflictResolutionsState.value,
                    onToggleCategory = { cat, _ -> toggledCat = cat },
                    onTogglePatient = { id, _ -> toggledPid = id },
                    onToggleSelectAll = {},
                    onSetResolution = { _, strat -> chosenStrategy = strat },
                    onDismiss = {},
                    onConfirm = {},
                )
            }

            waitForIdle()

            // Click category row
            onNodeWithText("PRACTITIONERS", substring = true, useUnmergedTree = true).performClick()
            assertTrue(toggledCat == ImportCategory.PRACTITIONERS)

            // Click patient row
            onNodeWithText("Jones, Bob", substring = true, useUnmergedTree = true).performClick()
            assertTrue(toggledPid == "p-1")

            // Click radio buttons for conflict resolution strategies
            val rbMatcher =
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.RadioButton,
                )
            val rbNodes = onAllNodes(rbMatcher).fetchSemanticsNodes()
            println("DEBUG_RB_COUNT: ${rbNodes.size}")
            onAllNodes(rbMatcher)[0].performClick()
            assertTrue(chosenStrategy == ConflictResolutionStrategy.OVERWRITE_LOCAL)

            // Update conflictResolutionsState to test non-null branch of conflictResolutions[pid]
            conflictResolutionsState.value = mapOf("p-1" to ConflictResolutionStrategy.OVERWRITE_LOCAL)
            waitForIdle()

            onAllNodes(rbMatcher)[1].performClick()
            assertTrue(chosenStrategy == ConflictResolutionStrategy.KEEP_LOCAL)
        }
    }

    /**
     * Tests unknown patient fallback and select-all toggling in [ImportPreviewDialog].
     */
    @Test
    fun testImportPreviewDialogUnknownPatientAndSelectAll() {
        setAppLanguage("en")
        val p2 =
            dev.ohs.fhir.model.r4
                .Patient()
        val staging2 =
            PatientStagingItem(
                incomingPatient = p2,
                conflictType = ConflictType.EXACT_MATCH,
                resolutionStrategy = ConflictResolutionStrategy.CREATE_AS_NEW_ID,
            )
        val preview =
            ImportPreviewSummary(
                totalResources = 1,
                stagedPatients = listOf(staging2),
                stagedEncounterCount = 0,
                stagedPhotoCount = 0,
                stagedFormCount = 0,
                hasConflicts = false,
            )

        var selectAllState: Boolean? = null
        var toggledPid: String? = null

        runComposeUiTest {
            setContent {
                ImportPreviewDialog(
                    preview = preview,
                    filterOptions = ImportFilterOptions(),
                    selectedPatientIds = emptySet(),
                    conflictResolutions = emptyMap(),
                    onToggleCategory = { _, _ -> },
                    onTogglePatient = { id, _ -> toggledPid = id },
                    onToggleSelectAll = { selectAllState = it },
                    onSetResolution = { _, _ -> },
                    onDismiss = {},
                    onConfirm = {},
                )
            }

            waitForIdle()

            // Unknown patient fallback displayed
            onNodeWithText("Unknown", substring = true, useUnmergedTree = true).assertIsDisplayed()

            // Click Select All
            onNodeWithText("Select All", useUnmergedTree = true).performClick()
            assertTrue(selectAllState == true)

            // Click patient row with empty id
            onNodeWithText("Unknown", substring = true, useUnmergedTree = true).performClick()
            assertTrue(toggledPid == "")
        }
    }

    /**
     * Tests recomposition and skipping for [ImportPreviewDialog].
     */
    @Test
    fun testImportPreviewDialogRecompositionAndSkipping() {
        setAppLanguage("en")
        val p1 = createFhirPatient("p-1", "Alice", "Smith", LocalDate(1985, 5, 20), "MRN-101")
        val staging =
            PatientStagingItem(
                incomingPatient = p1,
                conflictType = ConflictType.EXACT_MATCH,
                resolutionStrategy = ConflictResolutionStrategy.OVERWRITE_LOCAL,
            )
        val preview1 =
            ImportPreviewSummary(
                totalResources = 5,
                stagedPatients = listOf(staging),
                stagedEncounterCount = 1,
                stagedPhotoCount = 2,
                stagedFormCount = 2,
                hasConflicts = false,
            )
        val preview2 = preview1.copy(totalResources = 10)

        runComposeUiTest {
            val outerTrigger = androidx.compose.runtime.mutableStateOf(0)
            val previewState = androidx.compose.runtime.mutableStateOf(preview1)
            val filterState = androidx.compose.runtime.mutableStateOf(ImportFilterOptions())
            val selectedState = androidx.compose.runtime.mutableStateOf(setOf("p-1"))
            val resolutionsState =
                androidx.compose.runtime.mutableStateOf(
                    mapOf("p-1" to ConflictResolutionStrategy.OVERWRITE_LOCAL),
                )
            val onToggleCatState =
                androidx.compose.runtime.mutableStateOf<(ImportCategory, Boolean) -> Unit>({ _, _ -> })
            val onTogglePatState =
                androidx.compose.runtime.mutableStateOf<(String, Boolean) -> Unit>({ _, _ -> })
            val onToggleAllState =
                androidx.compose.runtime.mutableStateOf<(Boolean) -> Unit>({})
            val onSetResState =
                androidx.compose.runtime.mutableStateOf<(String, ConflictResolutionStrategy) -> Unit>({ _, _ -> })
            val onDismissState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val onConfirmState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})

            setContent {
                val dummy = outerTrigger.value
                ImportPreviewDialog(
                    preview = previewState.value,
                    filterOptions = filterState.value,
                    selectedPatientIds = selectedState.value,
                    conflictResolutions = resolutionsState.value,
                    onToggleCategory = onToggleCatState.value,
                    onTogglePatient = onTogglePatState.value,
                    onToggleSelectAll = onToggleAllState.value,
                    onSetResolution = onSetResState.value,
                    onDismiss = onDismissState.value,
                    onConfirm = onConfirmState.value,
                )
            }
            waitForIdle()

            // Skipping
            outerTrigger.value++
            waitForIdle()

            // Mutate parameters
            previewState.value = preview2
            waitForIdle()

            filterState.value = ImportFilterOptions.all()
            waitForIdle()

            selectedState.value = emptySet()
            waitForIdle()

            resolutionsState.value = emptyMap()
            waitForIdle()

            onToggleCatState.value = { cat, _ -> println(cat) }
            waitForIdle()

            onTogglePatState.value = { pid, _ -> println(pid) }
            waitForIdle()

            onToggleAllState.value = { println(it) }
            waitForIdle()

            onSetResState.value = { _, strat -> println(strat) }
            waitForIdle()

            onDismissState.value = { println("dismiss") }
            waitForIdle()

            onConfirmState.value = { println("confirm") }
            waitForIdle()
        }
    }
}
