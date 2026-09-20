/**
 * @file ClinicalMrnAndTriageSearchWorkflowTest.kt
 * Contains declarations for ClinicalMrnAndTriageSearchWorkflowTest.kt.
 *
 * Validates clinical MRN indexing, token searches, single-character search resilience,
 * and photo association via the TriageViewModel workflow.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Identifier
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.terminologies.AdministrativeGender
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.viewmodel.PatientListViewModel
import io.healthplatform.chartcam.viewmodel.TriageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Workflow tests ensuring reliable patient searching across MRN tokens, full names, and special character queries.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClinicalMrnAndTriageSearchWorkflowTest {
    /**
     * Minimal in-memory file storage double.
     */
    private class DummyFileStorage : FileStorage {
        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String = fileName

        override fun readImage(path: String): ByteArray = ByteArray(0)

        override fun deleteImage(path: String): Result<Unit> = Result.failure(Exception("Not supported in stub"))

        override fun clearCache() {}
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: ChartCamDatabase
    private lateinit var fhirRepo: FhirRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var exportImportService: ExportImportService

    /**
     * Initializes test database and coroutine dispatcher before each test.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
        val storage = JvmSecureStorage("test_mrn_${java.util.UUID.randomUUID()}")
        authRepo = AuthRepository(storage)
        exportImportService = ExportImportService(db, DummyFileStorage())
    }

    /**
     * Cleans up coroutine dispatchers and database connections after each test.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    /**
     * Helper to create and persist a test patient.
     *
     * @param id The FHIR ID.
     * @param first The given name.
     * @param last The family name.
     * @param mrn The MRN string.
     */
    private suspend fun insertPatient(
        id: String,
        first: String,
        last: String,
        mrn: String,
    ) {
        val patient =
            Patient
                .Builder()
                .apply {
                    this.id = id
                    name.add(
                        HumanName.Builder().apply {
                            family = FhirString.Builder().apply { value = last }
                            given.add(FhirString.Builder().apply { value = first })
                        },
                    )
                    identifier.add(
                        Identifier.Builder().apply {
                            value = FhirString.Builder().apply { value = mrn }
                        },
                    )
                    birthDate =
                        dev.ohs.fhir.model.r4.Date
                            .Builder()
                            .apply {
                                value = FhirDate.fromString("1980-01-01")
                            }
                    gender = Enumeration(value = AdministrativeGender.Unknown)
                }.build()
        fhirRepo.savePatient(patient)
    }

    /**
     * Verifies MRN searching in PatientListViewModel and TriageViewModel.
     */
    @Test
    fun testPatientListAndTriageSearchByMrn() =
        runTest(testDispatcher) {
            insertPatient("p1", "Eleanor", "Vance", "MRN-90210")
            insertPatient("p2", "Arthur", "Dent", "MRN-42424")
            insertPatient("p3", "Ford", "Prefect", "MRN-90211")

            val patientListVm = PatientListViewModel(fhirRepo, exportImportService, authRepo)
            testDispatcher.scheduler.advanceUntilIdle()

            // Exact MRN search
            patientListVm.onSearchQueryChanged("MRN-90210")
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, patientListVm.uiState.value.patients.size)
            assertEquals(
                "p1",
                patientListVm.uiState.value.patients
                    .first()
                    .id,
            )

            // Partial MRN search
            patientListVm.onSearchQueryChanged("902")
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(2, patientListVm.uiState.value.patients.size)
            val ids =
                patientListVm.uiState.value.patients
                    .map { it.id }
                    .toSet()
            assertTrue(ids.contains("p1"))
            assertTrue(ids.contains("p3"))

            // Triage search by MRN
            val triageVm = TriageViewModel(fhirRepo)
            triageVm.setPaths(mapOf("step1" to "file1.jpg"))
            triageVm.onSearchQueryChanged("42424")
            testDispatcher.scheduler.advanceUntilIdle()

            val triageResults = triageVm.uiState.value.searchResults
            assertEquals(1, triageResults.size)
            assertEquals("p2", triageResults.first().id)
        }

    /**
     * Verifies single-character and short surname searching without suppression.
     */
    @Test
    fun testSingleCharacterAndShortNameSearch() =
        runTest(testDispatcher) {
            insertPatient("p4", "Peter", "O", "MRN-0001")
            insertPatient("p5", "Alice", "Wu", "MRN-0002")

            val triageVm = TriageViewModel(fhirRepo)

            triageVm.onSearchQueryChanged("O")
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(
                triageVm.uiState.value.searchResults
                    .any { it.id == "p4" },
            )

            triageVm.onSearchQueryChanged("Wu")
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(
                triageVm.uiState.value.searchResults
                    .any { it.id == "p5" },
            )
        }

    /**
     * Verifies handling of special characters and delimiters in MRN strings.
     */
    @Test
    fun testSpecialCharactersInMrnSearch() =
        runTest(testDispatcher) {
            insertPatient("p6", "John", "Doe", "MRN/2026-A#9")

            val patientListVm = PatientListViewModel(fhirRepo, exportImportService, authRepo)
            patientListVm.onSearchQueryChanged("MRN/2026")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, patientListVm.uiState.value.patients.size)
            assertEquals(
                "p6",
                patientListVm.uiState.value.patients
                    .first()
                    .id,
            )
        }
}
