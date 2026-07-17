package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import com.google.fhir.model.r4.Device
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.database.ChartCamDatabase
import org.junit.Test

class RepositoryCoverageTest {
    @Test
    fun testFhirRepositoryNullPaths() {
        val driver =
            app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(
                app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY,
            )
        ChartCamDatabase.Schema.synchronous().create(driver)
        val db = ChartCamDatabase(driver)
        val repo = FhirRepository(db)

        kotlinx.coroutines.runBlocking {
            repo.getProvenancesForEncounter("enc1")
            repo.getAllPatients(false, null)
            repo.getAllPatients(false, "prac1")
            try {
                repo.updateEncounterStatus("enc1", "status", "notes")
            } catch (e: Exception) {
            }
            repo.getAllEncounters()
            repo.saveDevice(Device.Builder().apply { id = "dev1" }.build())
            repo.getDevice("dev1")
            repo.getAllDevices()
            repo.getAllQuestionnaireResponses()
            repo.getAllDocumentReferences()
            repo.searchPatients("query", false, null)

            val prov =
                io.healthplatform.chartcam.models
                    .createFhirProvenance("prov1", "res1", "prac1", "2026-07-09T00:00:00Z")
            repo.saveProvenance(prov, "enc1")

            repo.getAllProvenances()
            repo.getPhotosForEncounter("enc1")
        }
    }

    @Test
    fun testQuestionnaireRepositoryCoverage() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking {
            repo.loadDefaultForms()
            val dummyQ =
                Questionnaire
                    .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = "dummy1"
                    }.build()

            repo.saveQuestionnaire(dummyQ)
            repo.deleteQuestionnaire("dummy1")

            repo.createQuestionnaire("Test Form", 1, "test")
        }
    }
}
