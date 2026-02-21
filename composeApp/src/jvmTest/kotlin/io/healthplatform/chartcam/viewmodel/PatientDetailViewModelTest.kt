
package io.healthplatform.chartcam.viewmodel
import app.cash.sqldelight.async.coroutines.await

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate
import io.healthplatform.chartcam.models.Encounter
import io.healthplatform.chartcam.models.HumanName
import io.healthplatform.chartcam.models.Patient
import io.healthplatform.chartcam.models.Period
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PatientDetailViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: FhirRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        kotlinx.coroutines.runBlocking { ChartCamDatabase.Schema.awaitCreate(driver) }
        repo = FhirRepository(ChartCamDatabase(driver))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPatientDetailLoad() = runTest {
        val patientId = "pat-1"
        repo.savePatient(Patient(patientId, listOf(HumanName("Doe", listOf("John"))), kotlinx.datetime.LocalDate(1990,1,1), "m", "123"))
        
        val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        repo.saveEncounter(Encounter("enc-1", "finished", "clinical-photography", patientId, "prac-1", Period(now), "Some notes"))

        val vm = PatientDetailViewModel(repo)
        vm.loadPatientData(patientId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.patient)
        assertEquals(patientId, state.patient!!.id)
        assertEquals(1, state.encounters.size)
        assertEquals("Some notes", state.encounters[0].text)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun testEmptyEncounters() = runTest {
        val patientId = "pat-empty"
        repo.savePatient(Patient(patientId, listOf(HumanName("Empty", listOf("Guy"))), kotlinx.datetime.LocalDate(1990,1,1), "m", "321"))
        
        val vm = PatientDetailViewModel(repo)
        vm.loadPatientData(patientId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.patient)
        assertTrue(state.encounters.isEmpty())
    }
}