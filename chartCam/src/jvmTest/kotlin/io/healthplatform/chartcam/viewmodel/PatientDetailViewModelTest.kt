/**
 * @file PatientDetailViewModelTest.kt
 * Contains declarations for PatientDetailViewModelTest.kt.
 *
 * Comprehensive tests for [PatientDetailViewModel].
 *
 * Provides verification that patient details and encounters are loaded
 * accurately from the repository into the view model's UI state.
 */
package io.healthplatform.chartcam.viewmodel

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.FhirRepository
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Automated test suite covering [PatientDetailViewModel] behaviors.
 *
 * Uses an in-memory SQL database for quick and reproducible validation of data loading logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PatientDetailViewModelTest {
    /**
     * Dispatcher to allow synchronized control of coroutine executions during tests.
     */
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Main data access object hooked up to the in-memory testing database.
     */
    private lateinit var repo: FhirRepository

    /**
     * Prepares the execution environment prior to running each test case.
     *
     * Hooks the coroutine main dispatcher to [testDispatcher] and initializes
     * an isolated in-memory database configuration.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        kotlinx.coroutines.runBlocking { ChartCamDatabase.Schema.awaitCreate(driver) }
        repo = FhirRepository(ChartCamDatabase(driver))
    }

    /**
     * Releases environmental overrides post-test execution.
     *
     * Resets the coroutines main dispatcher to clean up after test execution.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Verifies successful loading of patient data and associated encounters.
     *
     * Pre-populates the repository with a specific patient and encounter, then forces
     * the view model to load it, subsequently asserting that the state contains the correct data.
     */
    @Test
    fun testPatientDetailLoad() =
        runTest {
            val patientId = "pat-1"
            repo.savePatient(createFhirPatient(patientId, "John", "Doe", kotlinx.datetime.LocalDate(1990, 1, 1), "123"))

            repo.saveEncounter(createFhirEncounter("enc-1", patientId, "prac-1", "2023-10-25T10:00:00+00:00"))

            val vm = PatientDetailViewModel(repo)
            vm.loadPatientData(patientId)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = vm.uiState.value
            assertNotNull(state.patient)
            assertEquals(patientId, state.patient.id)
            // Check notes via repo because Encounter object itself doesn't hold the notes in our simplified approach

            assertEquals(1, state.encounters.size)
            assertEquals(false, state.isLoading)
        }

    /**
     * Verifies robust loading behavior when a patient has no pre-existing encounters.
     *
     * Ensures that the view model correctly handles cases where a patient is found
     * but zero associated encounters exist in the database.
     */
    @Test
    fun testEmptyEncounters() =
        runTest {
            val patientId = "pat-empty"
            repo.savePatient(createFhirPatient(patientId, "Guy", "Empty", kotlinx.datetime.LocalDate(1990, 1, 1), "321"))

            val vm = PatientDetailViewModel(repo)
            vm.loadPatientData(patientId)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = vm.uiState.value
            assertNotNull(state.patient)
            assertTrue(state.encounters.isEmpty())
        }

    /**
     * Verifies successful deletion of patient data.
     */
    @Test
    fun testPatientDeleteSuccess() =
        runTest {
            val patientId = "pat-del"
            repo.savePatient(createFhirPatient(patientId, "Guy", "Del", kotlinx.datetime.LocalDate(1990, 1, 1), "444"))

            val vm = PatientDetailViewModel(repo)
            vm.loadPatientData(patientId)
            testDispatcher.scheduler.advanceUntilIdle()

            var successCalled = false
            vm.deletePatient {
                successCalled = true
            }
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(successCalled)
            val deletedPatient = repo.getPatient(patientId)
            assertEquals(null, deletedPatient)
        }

    /**
     * Verifies deletePatient when patient is null.
     */
    @Test
    fun testPatientDeleteNullPatient() =
        runTest {
            val vm = PatientDetailViewModel(repo)
            var successCalled = false
            vm.deletePatient {
                successCalled = true
            }
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(false, successCalled)
        }

    /**
     * Verifies that deletePatient cascades through fileStorage to clean up physical media files.
     */
    @Test
    fun testPatientDeleteWithFileStorageCascade() =
        runTest {
            val patientId = "pat-cascade"
            repo.savePatient(createFhirPatient(patientId, "Cascade", "Patient", kotlinx.datetime.LocalDate(1990, 1, 1), "999"))
            repo.saveEncounter(createFhirEncounter("enc-casc-1", patientId, "prac-1", "2024-01-01T10:00:00Z"))

            val storage =
                object : io.healthplatform.chartcam.files.FileStorage {
                    val storedFiles = mutableMapOf<String, ByteArray>()

                    override fun saveImage(
                        fileName: String,
                        bytes: ByteArray,
                    ): String {
                        storedFiles[fileName] = bytes
                        return fileName
                    }

                    override fun readImage(path: String): ByteArray = storedFiles[path] ?: ByteArray(0)

                    override fun deleteImage(path: String): Result<Unit> {
                        storedFiles.remove(path)
                        return Result.success(Unit)
                    }

                    override fun clearCache() {
                        storedFiles.clear()
                    }
                }

            storage.saveImage("photo-1.jpg", byteArrayOf(1, 2, 3))
            repo.saveDocumentReference(
                io.healthplatform.chartcam.models.createFhirDocumentReference(
                    io.healthplatform.chartcam.models.DocumentReferenceCreationParams(
                        id = "doc-casc-1",
                        patientId = patientId,
                        encounterId = "enc-casc-1",
                        dateStr = "2024-01-01",
                        desc = "Photo 1",
                        mime = "image/jpeg",
                        urlPath = "photo-1.jpg",
                        answerCode = "c1",
                    ),
                ),
            )

            val vm = PatientDetailViewModel(repo, storage)
            vm.loadPatientData(patientId)
            testDispatcher.scheduler.advanceUntilIdle()

            var deleted = false
            vm.deletePatient { deleted = true }
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(deleted)
            assertEquals(0, storage.storedFiles.size)
            assertEquals(null, repo.getPatient(patientId))
            assertEquals(null, repo.getEncounter("enc-casc-1"))
        }

    /**
     * Verifies deletePatient failure branches with custom and fallback error messages.
     */
    @Test
    fun testPatientDeleteFailureHandling() =
        runTest {
            val patientId = "pat-fail"
            repo.savePatient(createFhirPatient(patientId, "Fail", "User", kotlinx.datetime.LocalDate(1990, 1, 1), "001"))

            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.awaitCreate(driver)
            val db = ChartCamDatabase(driver)

            val failingRepo =
                object : FhirRepository(db) {
                    override suspend fun getPatient(id: String): dev.ohs.fhir.model.r4.Patient =
                        createFhirPatient(patientId, "Fail", "User", kotlinx.datetime.LocalDate(1990, 1, 1), "001")

                    override suspend fun deletePatient(
                        id: String,
                        fileStorage: io.healthplatform.chartcam.files.FileStorage?,
                    ): Result<Unit> = Result.failure(RuntimeException("Disk write error"))
                }

            val vm = PatientDetailViewModel(failingRepo)
            vm.loadPatientData(patientId)
            testDispatcher.scheduler.advanceUntilIdle()

            var successCalled = false
            vm.deletePatient { successCalled = true }
            testDispatcher.scheduler.advanceUntilIdle()

            kotlin.test.assertFalse(successCalled)
            assertEquals("Disk write error", vm.uiState.value.error)

            // Fallback message when exception message is null
            val nullMsgRepo =
                object : FhirRepository(db) {
                    override suspend fun getPatient(id: String): dev.ohs.fhir.model.r4.Patient =
                        createFhirPatient(patientId, "Fail", "User", kotlinx.datetime.LocalDate(1990, 1, 1), "001")

                    override suspend fun deletePatient(
                        id: String,
                        fileStorage: io.healthplatform.chartcam.files.FileStorage?,
                    ): Result<Unit> = Result.failure(Exception(null as String?))
                }

            val vm2 = PatientDetailViewModel(nullMsgRepo)
            vm2.loadPatientData(patientId)
            testDispatcher.scheduler.advanceUntilIdle()

            vm2.deletePatient { successCalled = true }
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("Failed to delete patient", vm2.uiState.value.error)
        }

    /**
     * Verifies deletePatient when loaded patient has null id.
     */
    @Test
    fun testPatientDeleteWhenPatientIdIsNull() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.awaitCreate(driver)
            val db = ChartCamDatabase(driver)

            val noIdRepo =
                object : FhirRepository(db) {
                    override suspend fun getPatient(id: String): dev.ohs.fhir.model.r4.Patient =
                        dev.ohs.fhir.model.r4
                            .Patient(id = null)
                }

            val vm = PatientDetailViewModel(noIdRepo)
            vm.loadPatientData("any-id")
            testDispatcher.scheduler.advanceUntilIdle()

            var successCalled = false
            vm.deletePatient { successCalled = true }
            testDispatcher.scheduler.advanceUntilIdle()

            kotlin.test.assertFalse(successCalled)
        }

    /**
     * Verifies data class contract of PatientDetailUiState.
     */
    @Test
    fun testPatientDetailUiStateContract() {
        val s1 = PatientDetailUiState()
        val s2 = PatientDetailUiState()
        val p = createFhirPatient("p1", "First", "Last", kotlinx.datetime.LocalDate(1990, 1, 1), "123")
        val s3 = PatientDetailUiState(patient = p, isLoading = true, error = "err")

        assertEquals(s1, s2)
        kotlin.test.assertNotEquals(s1, s3)
        kotlin.test.assertFalse(s1.equals(null))
        kotlin.test.assertFalse(s1.equals("different type"))
        assertEquals(s1.hashCode(), s2.hashCode())
        assertTrue(s3.toString().contains("err"))

        val copy1 = s3.copy(isLoading = false)
        kotlin.test.assertFalse(copy1.isLoading)

        assertEquals(p, s3.component1())
        assertEquals(emptyList(), s3.component2())
        assertTrue(s3.component3())
        assertEquals("err", s3.component4())
    }
}
