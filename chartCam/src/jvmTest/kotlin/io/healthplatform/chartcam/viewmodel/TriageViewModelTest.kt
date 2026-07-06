/**
 * Comprehensive tests for [TriageViewModel].
 *
 * This file contains test cases designed to verify the correct functionality
 * of the triage workflow, including patient search, selection, and photo paths management.
 */
package io.healthplatform.chartcam.viewmodel

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.models.familyName
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test class covering the [TriageViewModel].
 *
 * Sets up an in-memory database to test the view model's interactions with patient data
 * and UI state modifications such as photo paths.
 */
class TriageViewModelTest {
    /**
     * Dispatcher used to control the execution of coroutines in tests.
     */
    private val testDispatcher = StandardTestDispatcher()

    /**
     * A repository handling FHIR operations connected to an in-memory database.
     */
    private lateinit var repo: FhirRepository

    /**
     * Initializes the test environment before each test runs.
     *
     * Configures the main coroutine dispatcher and sets up the in-memory
     * SQLite database and [FhirRepository] for accurate, isolated testing.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        kotlinx.coroutines.runBlocking { ChartCamDatabase.Schema.awaitCreate(driver) }
        repo = FhirRepository(ChartCamDatabase(driver))
    }

    /**
     * Cleans up the test environment after each test runs.
     *
     * Resets the main coroutine dispatcher to prevent bleeding state across tests.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Verifies that the view model can properly store and expose photo paths.
     *
     * Asserts that paths passed to [TriageViewModel.setPaths] are accurately reflected
     * within the view model's UI state.
     */
    @Test
    fun testSetPaths() =
        runTest {
            val vm = TriageViewModel(repo)
            val paths = mapOf("FRONT" to "/tmp/a.jpg")
            vm.setPaths(paths)
            assertEquals(paths, vm.uiState.value.capturedPhotoPaths)
        }

    /**
     * Verifies the view model's patient search and selection capabilities.
     *
     * Tests creating a patient, ensuring the patient becomes automatically selected,
     * and verifying that searching by the new patient's name yields correct results.
     */
    @Test
    fun testPatientSearchAndSelection() =
        runTest {
            val vm = TriageViewModel(repo)

            // Use VM to create patient
            vm.createPatient("Bob", "Builder", "999", kotlinx.datetime.LocalDate(1990, 1, 1), "m")
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert selected
            assertNotNull(vm.uiState.value.selectedPatient)
            assertEquals(
                "Builder",
                vm.uiState.value.selectedPatient
                    ?.name
                    ?.first()
                    ?.familyName,
            )

            // Assert search finds it
            vm.onSearchQueryChanged("Builder")
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, vm.uiState.value.searchResults.size)
        }
}
