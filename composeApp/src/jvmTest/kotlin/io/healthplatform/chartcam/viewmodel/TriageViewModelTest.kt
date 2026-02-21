
package io.healthplatform.chartcam.viewmodel
import app.cash.sqldelight.async.coroutines.await

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate
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

class TriageViewModelTest {

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
    fun testSetPaths() = runTest {
        val vm = TriageViewModel(repo)
        val paths = mapOf("FRONT" to "/tmp/a.jpg")
        vm.setPaths(paths)
        assertEquals(paths, vm.uiState.value.capturedPhotoPaths)
    }

    @Test
    fun testPatientSearchAndSelection() = runTest {
        val vm = TriageViewModel(repo)
        
        // Use VM to create patient
        vm.createPatient("Bob", "Builder", "999", kotlinx.datetime.LocalDate(1990,1,1), "m")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert selected
        assertNotNull(vm.uiState.value.selectedPatient)
        assertEquals("Builder", vm.uiState.value.selectedPatient?.name?.first()?.family)
        
        // Assert search finds it
        vm.onSearchQueryChanged("Builder")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.uiState.value.searchResults.size)
    }
}