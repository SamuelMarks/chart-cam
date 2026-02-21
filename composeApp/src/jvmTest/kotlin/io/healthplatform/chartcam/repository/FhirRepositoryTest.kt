
package io.healthplatform.chartcam.repository
import app.cash.sqldelight.async.coroutines.await

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate
import io.healthplatform.chartcam.models.Encounter
import io.healthplatform.chartcam.models.HumanName
import io.healthplatform.chartcam.models.Patient
import io.healthplatform.chartcam.models.Period
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FhirRepositoryTest {

    private fun createRepository(): FhirRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        kotlinx.coroutines.runBlocking { ChartCamDatabase.Schema.awaitCreate(driver) }
        return FhirRepository(ChartCamDatabase(driver))
    }

    @Test
    fun testPatientAndEncounterLinking() = runTest {
        val repo = createRepository()

        // 1. Create Patient
        val patientId = "pat_01"
        val patient = Patient(
            id = patientId,
            name = listOf(HumanName("Doe", listOf("John"))),
            birthDate = LocalDate(1980, 1, 1),
            gender = "male",
            mrn = "MRN123",
            managingOrganization = "Org1"
        )
        repo.savePatient(patient)

        // 2. Create Encounter
        val encounterId = "enc_01"
        val now = LocalDateTime.parse("2023-10-25T10:00:00")
        val encounter = Encounter(
            id = encounterId,
            status = "in-progress",
            type = "clinical-photography",
            subjectReference = patientId,
            participantReference = "prac_01",
            period = Period(start = now),
            text = "Patient complains of leg pain"
        )
        repo.saveEncounter(encounter)

        // 3. Verify Fetch
        val fetchedEncounter = repo.getEncounter(encounterId)
        assertNotNull(fetchedEncounter)
        assertEquals("Patient complains of leg pain", fetchedEncounter.text)
        assertEquals(patientId, fetchedEncounter.subjectReference)
    }

    @Test
    fun testPatientSearch() = runTest {
        val repo = createRepository()
        
        repo.savePatient(Patient("1", listOf(HumanName("Smith", listOf("Jane"))), LocalDate(1990,1,1), "f", "A1"))
        repo.savePatient(Patient("2", listOf(HumanName("Jones", listOf("Bob"))), LocalDate(1991,1,1), "m", "A2"))
        
        val results = repo.searchPatients("Smith")
        assertEquals(1, results.size)
        assertEquals("Jane", results[0].name[0].given[0])
        
        val resultsMrn = repo.searchPatients("A2")
        assertEquals(1, resultsMrn.size)
        assertEquals("Jones", resultsMrn[0].name[0].family)
    }
}