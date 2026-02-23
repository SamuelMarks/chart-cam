package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.models.createFhirEncounter
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Encounter
import io.healthplatform.chartcam.models.givenName
import io.healthplatform.chartcam.models.familyName
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
        val patient = createFhirPatient(
            id = patientId,
            firstName = "John",
            lastName = "Doe",
            dob = LocalDate(1980, 1, 1),
            mrnValue = "MRN123",
            genderStr = "male"
        )
        repo.savePatient(patient)

        // 2. Create Encounter
        val encounterId = "enc_01"
        val now = "2023-10-25T10:00:00+00:00"
        val encounter = createFhirEncounter(
            id = encounterId,
            patientId = patientId,
            practitionerId = "prac_01",
            dateStr = now,
            statusStr = "in-progress"
        )
        repo.saveEncounter(encounter)

        // 3. Verify Fetch
        val fetchedEncounter = repo.getEncounter(encounterId)
        // Test update status
        repo.updateEncounterStatus(encounterId, "finished")
        val updated = repo.getEncounter(encounterId)
        assertEquals(Encounter.EncounterStatus.Finished, updated?.status?.value)
    }

    @Test
    fun testPatientSearch() = runTest {
        val repo = createRepository()
        
        repo.savePatient(createFhirPatient("1", "Jane", "Smith", LocalDate(1990,1,1), "A1", "f"))
        repo.savePatient(createFhirPatient("2", "Bob", "Jones", LocalDate(1991,1,1), "A2", "m"))
        
        val results = repo.searchPatients("Smith")
        assertEquals(1, results.size)
        assertEquals("Jane", results[0].name.firstOrNull()?.givenName)
        
        val resultsMrn = repo.searchPatients("A2")
        assertEquals(1, resultsMrn.size)
        assertEquals("Jones", resultsMrn[0].name.firstOrNull()?.familyName)
        
        val all = repo.getAllPatients()
        assertEquals(2, all.size)
    }
    @Test
    fun testQuestionnaireResponses() = runTest {
        val repo = createRepository()
        val qr = com.google.fhir.model.r4.QuestionnaireResponse.Builder(com.google.fhir.model.r4.Enumeration(value = com.google.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus.Completed)).apply { 
            id = "qr1"
            encounter = com.google.fhir.model.r4.Reference.Builder().apply { reference = com.google.fhir.model.r4.String.Builder().apply { value = "Encounter/enc1" } }
        }.build()
        repo.saveQuestionnaireResponse(qr)
        val qrs = repo.getQuestionnaireResponsesForEncounter("Encounter/enc1")
        assertEquals(1, qrs.size)
    }
}
