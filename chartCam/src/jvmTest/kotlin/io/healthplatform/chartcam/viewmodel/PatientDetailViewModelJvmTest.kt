/**
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
class PatientDetailViewModelJvmTest {
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
            repo.savePatient(createFhirPatient(patientId, "John", "Doe", kotlinx.datetime.LocalDate(1990, 1, 1), "123", "male"))

            repo.saveEncounter(createFhirEncounter("enc-1", patientId, "prac-1", "2023-10-25T10:00:00+00:00", "finished"))

            val vm = PatientDetailViewModel(repo)
            vm.loadPatientData(patientId)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = vm.uiState.value
            assertNotNull(state.patient)
            assertEquals(patientId, state.patient!!.id)
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
            repo.savePatient(createFhirPatient(patientId, "Guy", "Empty", kotlinx.datetime.LocalDate(1990, 1, 1), "321", "male"))

            val vm = PatientDetailViewModel(repo)
            vm.loadPatientData(patientId)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = vm.uiState.value
            assertNotNull(state.patient)
            assertTrue(state.encounters.isEmpty())
        }
}
