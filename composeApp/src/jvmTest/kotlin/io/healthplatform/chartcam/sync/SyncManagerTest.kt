
package io.healthplatform.chartcam.sync
import app.cash.sqldelight.async.coroutines.await

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitCreate
import io.healthplatform.chartcam.models.Encounter
import io.healthplatform.chartcam.models.HumanName
import io.healthplatform.chartcam.models.Patient
import io.healthplatform.chartcam.models.Period
import io.healthplatform.chartcam.network.NetworkClient
import io.healthplatform.chartcam.repository.FhirRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertTrue

class SyncManagerTest {

    @Test
    fun testSyncEncounterFlow() = runTest {
        // 1. Setup DB
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        kotlinx.coroutines.runBlocking { ChartCamDatabase.Schema.awaitCreate(driver) }
        val fhirRepo = FhirRepository(ChartCamDatabase(driver))

        // 2. Setup Network (Always succeeds)
        val mockEngine = MockEngine { respond("OK") }
        val client = NetworkClient.create(mockEngine)

        val syncManager = SyncManager(fhirRepo, client)

        // 3. Seed Data
        val patId = "pat_sync_1"
        fhirRepo.savePatient(
            Patient(patId, listOf(HumanName("Test", listOf("Sync"))), kotlinx.datetime.LocalDate(2000,1,1), "m", "123")
        )

        val encId = "enc_sync_1"
        fhirRepo.saveEncounter(
            Encounter(
                id = encId,
                status = "finished",
                subjectReference = patId,
                participantReference = "prac_1",
                period = Period(start = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())),
                text = "Sync Test"
            )
        )

        // 4. Act
        val result = syncManager.syncEncounter(encId)

        // 5. Assert
        assertTrue(result, "Sync should return true when data is valid and network succeeds")
    }
}