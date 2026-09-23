/**
 * @file PatientDetailScreenJvmTest.kt
 * Contains declarations for PatientDetailScreenJvmTest.kt.
 *
 * JVM Compose UI tests for [PatientDetailScreen.kt] components, menus, delete dialog, and encounter list.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Narrative
import dev.ohs.fhir.model.r4.Xhtml
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.viewmodel.PatientDetailUiState
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class PatientDetailTestFileStorage : FileStorage {
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
 * Test class for PatientDetailScreen on JVM.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class PatientDetailScreenJvmTest {
    /**
     * Tests PatientDetailScreen initialization.
     */
    @Test
    fun testPatientDetailScreenDefaultOverload() =
        runComposeUiTest {
            val repo = createTestFhirRepository()
            val storage = PatientDetailTestFileStorage()
            setContent {
                PatientDetailScreen(
                    viewModel =
                        io.healthplatform.chartcam.viewmodel
                            .PatientDetailViewModel(repo, storage),
                    onBack = {},
                    onNewVisit = {},
                    onVisitSelected = {},
                )
            }
            onRoot().assertExists()
        }

    /**
     * Tests full PatientDetailScreen flow including encounters, dialogs, FAB, and deletion.
     */
    @Test
    fun testPatientDetailScreenFullFlow() =
        runComposeUiTest {
            setAppLanguage("en")
            val repo = createTestFhirRepository()
            val storage = PatientDetailTestFileStorage()
            val patient = createFhirPatient("pat-1", "John", "Doe", LocalDate(1980, 5, 10), "MRN-1")

            val encWithNotes =
                createFhirEncounter("enc-1", "pat-1", "dr1", "2026-09-01T10:00:00Z").copy(
                    text =
                        Narrative(
                            status = Enumeration(value = Narrative.NarrativeStatus.Generated),
                            div = Xhtml(value = "<div>Follow-up checkup completed</div>"),
                        ),
                )
            val encNoNotes = createFhirEncounter("enc-2", "pat-1", "dr1", "2026-09-02T10:00:00Z")
            val encNullDiv =
                createFhirEncounter("enc-3", "pat-1", "dr1", "2026-09-04T10:00:00Z").copy(
                    text =
                        Narrative(
                            status = Enumeration(value = Narrative.NarrativeStatus.Generated),
                            div = Xhtml(value = ""),
                        ),
                )

            runBlocking {
                repo.savePatient(patient)
                repo.saveEncounter(encWithNotes)
                repo.saveEncounter(encNoNotes)
                repo.saveEncounter(encNullDiv)
            }

            var backCalled = false
            var newVisitCalled = false
            var selectedVisitId = ""
            val vm =
                io.healthplatform.chartcam.viewmodel
                    .PatientDetailViewModel(repo, storage)
            vm.loadPatientData("pat-1")

            setContent {
                PatientDetailScreen(
                    viewModel = vm,
                    onBack = { backCalled = true },
                    onNewVisit = { newVisitCalled = true },
                    onVisitSelected = { selectedVisitId = it },
                )
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Doe, John", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }

            // Click encounter with notes
            onNodeWithText("Follow-up checkup completed", substring = true, useUnmergedTree = true).performClick()
            assertEquals("enc-1", selectedVisitId)

            // Verify no notes fallback encounter renders
            onNodeWithText("No notes", substring = true, useUnmergedTree = true).assertIsDisplayed()

            // Click FAB for new visit
            onNodeWithContentDescription("New Visit").performClick()
            assertTrue(newVisitCalled)

            // Click Back in TopAppBar
            onNodeWithContentDescription("Back").performClick()
            assertTrue(backCalled)

            // Open More menu and cancel delete
            onNodeWithContentDescription("More options").performClick()
            waitForIdle()
            onNodeWithText("Delete Patient").performClick()
            waitForIdle()
            onNodeWithText("Cancel").performClick()
            waitForIdle()

            // Open More menu and confirm delete
            backCalled = false
            onNodeWithContentDescription("More options").performClick()
            waitForIdle()
            onNodeWithText("Delete Patient").performClick()
            waitForIdle()
            onNodeWithText("Delete").performClick()
            waitForIdle()
            assertTrue(backCalled)
        }

    /**
     * Tests empty visits state prompt.
     */
    @Test
    fun testPatientDetailEmptyVisits() =
        runComposeUiTest {
            setAppLanguage("en")
            val repo = createTestFhirRepository()
            val storage = PatientDetailTestFileStorage()
            val patient = createFhirPatient("pat-empty", "Bob", "Empty", LocalDate(1995, 1, 1), "MRN-EMPTY")
            runBlocking { repo.savePatient(patient) }

            val vm =
                io.healthplatform.chartcam.viewmodel
                    .PatientDetailViewModel(repo, storage)
            vm.loadPatientData("pat-empty")

            setContent {
                PatientDetailScreen(
                    viewModel = vm,
                    onBack = {},
                    onNewVisit = {},
                    onVisitSelected = {},
                )
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("No visits found", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("No visits found", substring = true, useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Direct component test for PatientDeleteConfirmDialog.
     */
    @Test
    fun testDirectDeleteConfirmDialog() =
        runComposeUiTest {
            setAppLanguage("en")
            var dismissed = false
            var confirmed = false

            setContent {
                PatientDeleteConfirmDialog(
                    onDismiss = { dismissed = true },
                    onDeletePatient = { confirmed = true },
                )
            }

            waitForIdle()
            onNodeWithText("Delete").performClick()
            assertTrue(dismissed)
            assertTrue(confirmed)
        }

    /**
     * Direct component test for PatientDetailContent with null encounter ID fallback.
     */
    @Test
    fun testDirectPatientDetailContentWithNullEncounterId() =
        runComposeUiTest {
            setAppLanguage("en")
            val patient = createFhirPatient("pat-direct", "Direct", "Test", LocalDate(2000, 1, 1), "MRN-DIR")
            val encNullId = createFhirEncounter(null, "pat-direct", "dr1", "2026-09-03T10:00:00Z")
            var clickedId = "unset"

            setContent {
                PatientDetailContent(
                    padding = PaddingValues(0.dp),
                    state =
                        PatientDetailUiState(
                            patient = patient,
                            encounters = listOf(encNullId),
                        ),
                    onVisitSelected = { clickedId = it },
                )
            }

            waitForIdle()
            onNodeWithText("No notes", substring = true, useUnmergedTree = true).performClick()
            assertEquals("", clickedId)
        }

    /**
     * Recomposition test for PatientDetailTopBar.
     */
    @Test
    fun testPatientDetailTopBarRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val onBackState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val onDeleteState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val trigger = androidx.compose.runtime.mutableStateOf(0)

            setContent {
                val dummy = trigger.value
                val scrollBehavior =
                    androidx.compose.material3.TopAppBarDefaults
                        .pinnedScrollBehavior()
                PatientDetailTopBar(
                    onBack = onBackState.value,
                    onDeletePatient = onDeleteState.value,
                    scrollBehavior = scrollBehavior,
                )
            }
            waitForIdle()

            // Skipping
            trigger.value++
            waitForIdle()

            // Recomposition
            onBackState.value = {}
            onDeleteState.value = {}
            waitForIdle()

            // Dismiss menu
            onNodeWithContentDescription("More options").performClick()
            waitForIdle()
            onNodeWithText("Patient Detail").performClick()
            waitForIdle()
        }

    /**
     * Recomposition test for PatientInfo.
     */
    @Test
    fun testPatientInfoRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val pState =
                androidx.compose.runtime.mutableStateOf(
                    createFhirPatient("pat-info", "Info", "First", LocalDate(1990, 1, 1), "MRN-INFO"),
                )
            val trigger = androidx.compose.runtime.mutableStateOf(0)

            setContent {
                val dummy = trigger.value
                PatientInfo(patient = pState.value)
            }
            waitForIdle()

            // Skipping
            trigger.value++
            waitForIdle()

            // Recomposition
            pState.value = createFhirPatient("pat-info2", "Info2", "Second", LocalDate(1995, 2, 2), "MRN-INFO2")
            waitForIdle()
        }

    /**
     * Recomposition test for PatientDetailScreen.
     */
    @Test
    fun testPatientDetailScreenRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val repo = createTestFhirRepository()
            val storage = PatientDetailTestFileStorage()
            val vm1 =
                io.healthplatform.chartcam.viewmodel
                    .PatientDetailViewModel(repo, storage)
            val vm2 =
                io.healthplatform.chartcam.viewmodel
                    .PatientDetailViewModel(createTestFhirRepository(), storage)
            val vmState = androidx.compose.runtime.mutableStateOf(vm1)
            val patientIdState = androidx.compose.runtime.mutableStateOf("pat-recomp-1")
            val onBackState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val onNewVisitState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val onVisitSelectedState = androidx.compose.runtime.mutableStateOf<(String) -> Unit>({})
            val trigger = androidx.compose.runtime.mutableStateOf(0)

            setContent {
                val dummy = trigger.value
                PatientDetailScreen(
                    viewModel = vmState.value,
                    onBack = onBackState.value,
                    onNewVisit = onNewVisitState.value,
                    onVisitSelected = onVisitSelectedState.value,
                )
            }
            waitForIdle()

            // Skipping
            trigger.value++
            waitForIdle()

            // Recomposition: onBack changed, others unchanged
            onBackState.value = {}
            waitForIdle()

            // Recomposition: onNewVisit changed, others unchanged
            onNewVisitState.value = {}
            waitForIdle()

            // Recomposition: onVisitSelected changed, others unchanged
            onVisitSelectedState.value = {}
            waitForIdle()

            // Recomposition: vm changed, others unchanged
            vmState.value = vm2
            waitForIdle()
        }

    /**
     * Recomposition test for PatientDetailContent.
     */
    @Test
    fun testPatientDetailContentRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val stateState =
                androidx.compose.runtime.mutableStateOf(
                    PatientDetailUiState(
                        patient = createFhirPatient("pat-c", "Content", "Test", LocalDate(1990, 1, 1), "MRN-C"),
                        encounters = emptyList(),
                    ),
                )
            val onVisitSelectedState = androidx.compose.runtime.mutableStateOf<(String) -> Unit>({})
            val trigger = androidx.compose.runtime.mutableStateOf(0)

            setContent {
                val dummy = trigger.value
                PatientDetailContent(
                    padding = PaddingValues(0.dp),
                    state = stateState.value,
                    onVisitSelected = onVisitSelectedState.value,
                )
            }
            waitForIdle()

            // Skipping
            trigger.value++
            waitForIdle()

            // Recomposition
            stateState.value =
                PatientDetailUiState(
                    patient = createFhirPatient("pat-c2", "Content2", "Test2", LocalDate(1992, 2, 2), "MRN-C2"),
                    encounters = emptyList(),
                )
            onVisitSelectedState.value = {}
            waitForIdle()
        }

    /**
     * Recomposition test for PatientDeleteConfirmDialog.
     */
    @Test
    fun testPatientDeleteConfirmDialogRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val onDismissState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val onDeleteState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val trigger = androidx.compose.runtime.mutableStateOf(0)

            setContent {
                val dummy = trigger.value
                PatientDeleteConfirmDialog(
                    onDismiss = onDismissState.value,
                    onDeletePatient = onDeleteState.value,
                )
            }
            waitForIdle()

            // Skipping
            trigger.value++
            waitForIdle()

            // Recomposition
            onDismissState.value = {}
            onDeleteState.value = {}
            waitForIdle()
        }
}
