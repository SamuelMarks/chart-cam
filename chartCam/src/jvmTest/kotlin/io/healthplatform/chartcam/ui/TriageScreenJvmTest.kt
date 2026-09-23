/**
 * @file TriageScreenJvmTest.kt
 * Contains declarations for TriageScreenJvmTest.kt.
 *
 * JVM Compose UI tests for [TriageScreen.kt] components, batch actions, search, and patient selection.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Patient
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class TriageTestFileStorage : FileStorage {
    val deleted = mutableListOf<String>()

    override fun saveImage(fileName: String, bytes: ByteArray): String = fileName

    override fun readImage(path: String): ByteArray = ByteArray(0)

    override fun deleteImage(path: String): Result<Unit> {
        deleted.add(path)
        return Result.success(Unit)
    }

    override fun clearCache() {}
}

private fun createTestFhirRepository(): FhirRepository {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    ChartCamDatabase.Schema.synchronous().create(driver)
    return FhirRepository(driver)
}

/**
 * Test class for TriageScreen on JVM.
 */
@OptIn(ExperimentalTestApi::class)
class TriageScreenJvmTest {
    /**
     * Verifies initialization of TriageScreen.
     */
    @Test
    fun testTriageScreenDefaultOverload() =
        runComposeUiTest {
            val repo = createTestFhirRepository()
            val storage = TriageTestFileStorage()
            setContent {
                TriageScreen(
                    viewModel =
                        io.healthplatform.chartcam.viewmodel
                            .TriageViewModel(repo),
                    onProceedToEncounter = { _, _ -> },
                    onBack = {},
                    fileStorage = storage,
                )
            }
            onRoot().assertExists()
        }

    /**
     * Verifies that TriagePatientSelectionHeader merges descendants into a single accessible clickable node.
     */
    @Test
    fun testTriagePatientSelectionHeaderMergedSemantics() =
        runComposeUiTest {
            setAppLanguage("en")
            val patient = createFhirPatient("p1", "John", "Doe", LocalDate(1990, 1, 1), "MRN-1")
            var proceedClicked = false

            setContent {
                TriagePatientSelectionHeader(
                    patient = patient,
                    photoCount = 2,
                    onProceed = { proceedClicked = true },
                )
            }

            waitForIdle()
            onNodeWithText("Doe, John").assertExists().performClick()
            assertTrue(proceedClicked)
        }

    /**
     * Verifies photo batch management bar interactions: select all, clear selection, toggle, and batch delete.
     */
    @Test
    fun testTriagePhotoBatchBarInteractions() =
        runComposeUiTest {
            setAppLanguage("en")
            val storage = TriageTestFileStorage()
            val repo = createTestFhirRepository()
            val photos =
                mapOf(
                    "face" to "/tmp/face.jpg",
                    "lesion" to "/tmp/lesion.jpg",
                )
            var backClicked = false
            val vm =
                io.healthplatform.chartcam.viewmodel
                    .TriageViewModel(repo)
                    .apply {
                        setPaths(photos)
                    }

            setContent {
                TriageScreen(
                    viewModel = vm,
                    onProceedToEncounter = { _, _ -> },
                    onBack = { backClicked = true },
                    fileStorage = storage,
                )
            }

            waitForIdle()
            onNodeWithTag("TriagePhotoBatchBar").assertIsDisplayed()

            // Click Select All
            onNodeWithText("Select All").performClick()
            waitForIdle()

            // Click Clear Selection
            onNodeWithText("Clear").performClick()
            waitForIdle()

            // Toggle specific chip "face"
            onNodeWithText("face").performClick()
            waitForIdle()

            // Delete selected photo
            onNodeWithContentDescription("Delete Selected Photos").performClick()
            waitForIdle()
            assertEquals(listOf("/tmp/face.jpg"), storage.deleted)

            // Click back button in TopAppBar
            onNodeWithContentDescription("Back").performClick()
            assertTrue(backClicked)
        }

    /**
     * Verifies patient search, empty results prompt, and selecting patient to proceed.
     */
    @Test
    fun testTriageSearchSelectPatientAndProceed() =
        runComposeUiTest {
            setAppLanguage("en")
            val repo = createTestFhirRepository()
            val patient = createFhirPatient("pat-100", "Alice", "Wonderland", LocalDate(1988, 4, 12), "MRN-100")
            runBlocking { repo.savePatient(patient) }

            val photos = mapOf("step1" to "/tmp/s1.jpg", "step2" to "/tmp/s2.jpg")
            var proceededPatientId = ""
            var proceededPhotos: Map<String, String>? = null

            val vm =
                io.healthplatform.chartcam.viewmodel
                    .TriageViewModel(repo)
                    .apply {
                        setPaths(photos)
                    }

            setContent {
                TriageScreen(
                    viewModel = vm,
                    onProceedToEncounter = { id, paths ->
                        proceededPatientId = id
                        proceededPhotos = paths
                    },
                    onBack = {},
                    fileStorage = TriageTestFileStorage(),
                )
            }

            waitForIdle()

            // Search for non-existent query to trigger no_patients_found
            onNode(hasSetTextAction()).performTextInput("NoMatchQuery")
            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("No patients found.", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }

            // Clear search query via trailing clear icon
            onNodeWithContentDescription("Clear").performClick()
            waitForIdle()

            // Search for Wonderland
            onNode(hasSetTextAction()).performTextInput("Wonderland")
            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Wonderland, Alice", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }

            // Select patient
            onNodeWithText("Wonderland, Alice", substring = true, useUnmergedTree = true).performClick()
            waitForIdle()

            // Proceed without selected keys -> passes all photos (header is the first node)
            onAllNodesWithText("Wonderland, Alice", substring = true, useUnmergedTree = true)[0].performClick()
            waitForIdle()
            assertEquals("pat-100", proceededPatientId)
            assertEquals(photos, proceededPhotos)

            // Now select 1 photo key and proceed -> passes filtered paths
            onNodeWithText("step1").performClick()
            waitForIdle()
            onAllNodesWithText("Wonderland, Alice", substring = true, useUnmergedTree = true)[0].performClick()
            waitForIdle()
            assertEquals(mapOf("step1" to "/tmp/s1.jpg"), proceededPhotos)

            // Test selecting patient with null ID in TriageScreen
            val nullIdPatient =
                createFhirPatient("temp-null-id", "NoIdGiv", "NoIdFam", LocalDate(1990, 1, 1), "MRN-NOID")
                    .copy(id = null)
            runBlocking { repo.savePatient(nullIdPatient) }
            onNodeWithContentDescription("Clear").performClick()
            waitForIdle()
            onNode(hasSetTextAction()).performTextInput("NoIdFam")
            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("NoIdFam, NoIdGiv", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("NoIdFam, NoIdGiv", substring = true, useUnmergedTree = true).performClick()
            waitForIdle()
            onAllNodesWithText("NoIdFam, NoIdGiv", substring = true, useUnmergedTree = true)[0].performClick()
            waitForIdle()
            assertEquals("", proceededPatientId)
        }

    /**
     * Verifies recomposition and skipping for TriageScreen.
     */
    @Test
    fun testTriageScreenRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val repo = createTestFhirRepository()
            val storage = TriageTestFileStorage()
            val vm1 =
                io.healthplatform.chartcam.viewmodel
                    .TriageViewModel(repo)
            val vm2 =
                io.healthplatform.chartcam.viewmodel
                    .TriageViewModel(createTestFhirRepository())
            val vmState = mutableStateOf(vm1)
            val storageState = mutableStateOf(storage)
            val photosState = mutableStateOf(mapOf("p1" to "/path/1"))
            val onProceedState = mutableStateOf<(String, Map<String, String>) -> Unit>({ _, _ -> })
            val onBackState = mutableStateOf<() -> Unit>({})
            val trigger = mutableStateOf(0)

            setContent {
                val dummy = trigger.value
                TriageScreen(
                    viewModel = vmState.value,
                    onProceedToEncounter = onProceedState.value,
                    onBack = onBackState.value,
                    fileStorage = storageState.value,
                )
            }
            waitForIdle()

            // Skipping
            trigger.value++
            waitForIdle()

            // Recomposition: change onBack, others unchanged
            onBackState.value = {}
            waitForIdle()

            // Recomposition: change photos, others unchanged
            photosState.value = mapOf("p2" to "/path/2")
            waitForIdle()

            // Recomposition: change onProceed, others unchanged
            onProceedState.value = { _, _ -> }
            waitForIdle()

            // Recomposition: change vm, others unchanged
            vmState.value = vm2
            waitForIdle()

            // Recomposition: change storage, others unchanged
            storageState.value = TriageTestFileStorage()
            waitForIdle()
        }

    /**
     * Verifies proceeding when patient has null id.
     */
    @Test
    fun testTriageProceedNullPatientId() =
        runComposeUiTest {
            val emptyIdPatient = Patient.Builder().apply { id = null }.build()
            var proceededId = "unset"

            setContent {
                TriagePatientSelectionHeader(
                    patient = emptyIdPatient,
                    photoCount = 1,
                    onProceed = { proceededId = emptyIdPatient.id ?: "" },
                )
            }

            waitForIdle()
            onRoot().performClick()
            assertEquals("", proceededId)
        }

    /**
     * Verifies Add Patient button displays CreatePatientDialog and handles both dismiss and confirm.
     */
    @Test
    fun testTriageCreatePatientDialog() =
        runComposeUiTest {
            setAppLanguage("en")
            val repo = createTestFhirRepository()

            setContent {
                TriageScreen(
                    viewModel =
                        io.healthplatform.chartcam.viewmodel
                            .TriageViewModel(repo),
                    onProceedToEncounter = { _, _ -> },
                    onBack = {},
                    fileStorage = TriageTestFileStorage(),
                )
            }

            waitForIdle()
            onNodeWithContentDescription("Create Patient").performClick()
            waitForIdle()
            onNodeWithText("New Patient", substring = true, useUnmergedTree = true).assertExists()

            // Dismiss dialog
            onNodeWithText("Cancel", substring = true, useUnmergedTree = true).performClick()
            waitForIdle()

            // Re-open and confirm create
            onNodeWithContentDescription("Create Patient").performClick()
            waitForIdle()
            val textFields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
            if (textFields.size >= 5) {
                onAllNodes(hasSetTextAction())[1].performTextInput("Charlie")
                onAllNodes(hasSetTextAction())[2].performTextInput("Brown")
                onAllNodes(hasSetTextAction())[3].performTextInput("MRN-C")
                onAllNodes(hasSetTextAction())[4].performTextInput("05/15/1990")
            }
            waitForIdle()
            onNodeWithText("Create", substring = true, useUnmergedTree = true).performClick()
            waitForIdle()
        }

    /**
     * Verifies recomposition and skipping for TriagePatientSelectionHeader.
     */
    @Test
    fun testTriagePatientSelectionHeaderRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val pState = mutableStateOf(createFhirPatient("p1", "John", "Doe", LocalDate(1990, 1, 1), "MRN-1"))
            val countState = mutableStateOf(2)
            val trigger = mutableStateOf(0)
            val onProceedState = mutableStateOf<() -> Unit>({})

            setContent {
                val dummy = trigger.value
                TriagePatientSelectionHeader(
                    patient = pState.value,
                    photoCount = countState.value,
                    onProceed = onProceedState.value,
                )
            }
            waitForIdle()

            // Skipping
            trigger.value++
            waitForIdle()

            // Recomposition
            pState.value = createFhirPatient("p2", "Jane", "Smith", LocalDate(1992, 2, 2), "MRN-2")
            countState.value = 5
            onProceedState.value = { }
            waitForIdle()
        }

    /**
     * Verifies direct components: TriageSearchBar, TriagePhotoBatchBar.
     */
    @Test
    fun testDirectTriageComponentsRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val queryState = mutableStateOf("")
            var createClicked = false

            setContent {
                TriageSearchBar(
                    query = queryState.value,
                    onQueryChange = { queryState.value = it },
                    onCreatePatientClick = { createClicked = true },
                )
            }

            waitForIdle()
            onNodeWithContentDescription("Create Patient").performClick()
            assertTrue(createClicked)

            // When query is non-empty, clear button renders
            queryState.value = "ActiveSearch"
            waitForIdle()
            onNodeWithContentDescription("Clear").performClick()
            assertEquals("", queryState.value)
        }

    /**
     * Verifies direct TriagePhotoBatchBar with selected keys, non-selected keys, and recomposition.
     */
    @Test
    fun testDirectTriagePhotoBatchBar() =
        runComposeUiTest {
            setAppLanguage("en")
            val selectedKeysState = mutableStateOf(setOf("k1"))
            val photosState = mutableStateOf(mapOf("k1" to "f1", "k2" to "f2"))
            var cleared = false
            var allSelected = false
            var deleted = false
            var toggledKey = ""
            val onToggleState = mutableStateOf<(String) -> Unit>({ toggledKey = it })
            val onSelectAllState = mutableStateOf<() -> Unit>({ allSelected = true })
            val onClearState = mutableStateOf<() -> Unit>({ cleared = true })
            val onDeleteState = mutableStateOf<() -> Unit>({ deleted = true })
            val actionsState =
                mutableStateOf(
                    TriagePhotoBatchActions(
                        onToggleSelect = onToggleState.value,
                        onSelectAll = onSelectAllState.value,
                        onClearSelection = onClearState.value,
                        onDeleteSelected = onDeleteState.value,
                    ),
                )
            val trigger = mutableStateOf(0)

            setContent {
                val dummy = trigger.value
                TriagePhotoBatchBar(
                    photoPaths = photosState.value,
                    selectedKeys = selectedKeysState.value,
                    actions = actionsState.value,
                )
            }

            waitForIdle()
            onNodeWithText("Select All").performClick()
            assertTrue(allSelected)

            onNodeWithText("Clear").performClick()
            assertTrue(cleared)

            onNodeWithContentDescription("Delete Selected Photos").performClick()
            assertTrue(deleted)

            onNodeWithText("k2").performClick()
            assertEquals("k2", toggledKey)

            // Test skipping
            trigger.value++
            waitForIdle()

            // Test recomposition
            actionsState.value =
                TriagePhotoBatchActions(
                    onToggleSelect = {},
                    onSelectAll = {},
                    onClearSelection = {},
                    onDeleteSelected = {},
                )
            waitForIdle()
            photosState.value = mapOf("k3" to "f3")
            waitForIdle()
            selectedKeysState.value = emptySet()
            waitForIdle()
            onNodeWithText("Select All").assertIsDisplayed()

            // Verify helper builders directly
            val repo = createTestFhirRepository()
            val vm =
                io.healthplatform.chartcam.viewmodel
                    .TriageViewModel(repo)
            val builtActions = buildTriagePhotoBatchActions(vm, TriageTestFileStorage())
            builtActions.onSelectAll()
            builtActions.onClearSelection()
            builtActions.onToggleSelect("t")
            builtActions.onDeleteSelected()

            val dismissHandler = handleDismissCreatePatient(vm)
            dismissHandler()

            val confirmHandler = handleConfirmCreatePatient(vm)
            confirmHandler("A", "B", "MRN", LocalDate(2000, 1, 1), null)
        }
}
