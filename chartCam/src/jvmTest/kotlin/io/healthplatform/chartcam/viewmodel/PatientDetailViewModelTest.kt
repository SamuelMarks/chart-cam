package io.healthplatform.chartcam.viewmodel
import app.cash.sqldelight.async.coroutines.await

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Encounter
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
        repo.savePatient(createFhirPatient(patientId, "John", "Doe", kotlinx.datetime.LocalDate(1990,1,1), "123", "male"))
        
        repo.saveEncounter(createFhirEncounter("enc-1", patientId, "prac-1", "2023-10-25T10:00:00+00:00", "finished"))

        val vm = PatientDetailViewModel(repo)
        vm.loadPatientData(patientId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.patient)
        assertEquals(patientId, state.patient!!.id)
        // Check notes via repo because Encounter object itself doesn't hold the notes in our simplified approach

        assertEquals(1, state.encounters.size)
        assertEquals(1, state.encounters.size)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun testEmptyEncounters() = runTest {
        val patientId = "pat-empty"
        repo.savePatient(createFhirPatient(patientId, "Guy", "Empty", kotlinx.datetime.LocalDate(1990,1,1), "321", "male"))
        
        val vm = PatientDetailViewModel(repo)
        vm.loadPatientData(patientId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.patient)
        assertTrue(state.encounters.isEmpty())
    }
}
