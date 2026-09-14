/**
 * @file SameMrnDifferentUuidDeduplicationTest.kt
 * End-to-end workflow test verifying detection and unification of split-brain patient records with matching MRN.
 */
package io.healthplatform.chartcam.workflow

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.ConflictResolutionStrategy
import io.healthplatform.chartcam.models.ConflictType
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Validates system resilience against split-brain patient records sharing an MRN across distinct UUIDs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SameMrnDifferentUuidDeduplicationTest {
    /**
     * Minimal FileStorage mock.
     */
    private class DummyFileStorage : FileStorage {
        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String = fileName

        override fun readImage(path: String): ByteArray = ByteArray(0)

        override fun deleteImage(path: String): Result<Unit> = Result.failure(Exception("Not supported in stub"))

        override fun clearCache() {}
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var driverA: JdbcSqliteDriver
    private lateinit var driverB: JdbcSqliteDriver
    private lateinit var repoA: FhirRepository
    private lateinit var repoB: FhirRepository
    private lateinit var exportImportA: ExportImportService
    private lateinit var exportImportB: ExportImportService

    /**
     * Sets up isolated device databases.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        driverA = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driverA)
        val dbA = ChartCamDatabase(driverA)
        repoA = FhirRepository(dbA)
        exportImportA = ExportImportService(dbA, DummyFileStorage())

        driverB = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driverB)
        val dbB = ChartCamDatabase(driverB)
        repoB = FhirRepository(dbB)
        exportImportB = ExportImportService(dbB, DummyFileStorage())
    }

    /**
     * Cleans up drivers and dispatchers.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driverA.close()
        driverB.close()
    }

    /**
     * Verifies that matching MRNs across different UUIDs are detected during staging
     * and merged under the canonical local UUID.
     */
    @Test
    fun testSameMrnDifferentUuidMerge() =
        runTest(testDispatcher) {
            // Device A: Patient created with uuid-alpha
            val patientA =
                createFhirPatient(
                    id = "uuid-alpha",
                    firstName = "Charlie",
                    lastName = "Brown",
                    dob = LocalDate(1975, 3, 15),
                    mrnValue = "MRN-SPLIT-99",
                )
            repoA.savePatient(patientA)
            val encA = createFhirEncounter("enc-alpha-visit", "uuid-alpha", "dr-who", "2026-09-14T10:00:00Z")
            repoA.saveEncounter(encA)

            // Device B: Same real-world patient created offline with uuid-beta
            val patientB =
                createFhirPatient(
                    id = "uuid-beta",
                    firstName = "Charles",
                    lastName = "Brown",
                    dob = LocalDate(1975, 3, 15),
                    mrnValue = "MRN-SPLIT-99",
                )
            repoB.savePatient(patientB)
            val encB = createFhirEncounter("enc-beta-visit", "uuid-beta", "dr-who", "2026-09-14T11:00:00Z")
            repoB.saveEncounter(encB)

            val pass = "PasswordSecure123"
            val archive = exportImportA.exportData(pass, exportAll = true).getOrThrow()

            // Step 1: Inspect archive on Device B
            val preview = exportImportB.inspectArchive(archive, pass).getOrThrow()
            assertTrue(preview.hasConflicts, "Should flag MRN collision")
            assertEquals(1, preview.stagedPatients.size)
            assertEquals(ConflictType.MRN_COLLISION_DIFFERENT_ID, preview.stagedPatients[0].conflictType)
            assertEquals("uuid-beta", preview.stagedPatients[0].conflictingLocalPatient?.id)

            // Step 2: Ingest with MERGE_RECORDS
            exportImportB
                .importDataSelective(
                    encryptedData = archive,
                    password = pass,
                    resolutionMap = mapOf("uuid-alpha" to ConflictResolutionStrategy.MERGE_RECORDS),
                ).getOrThrow()

            // Step 3: Verify single patient exists for MRN-SPLIT-99
            val matchedPatient = repoB.getPatientByMrn("MRN-SPLIT-99")
            assertNotNull(matchedPatient)
            assertEquals("uuid-beta", matchedPatient.id, "Canonical ID should be preserved")

            // Step 4: Verify both encounters are attached to canonical uuid-beta
            val encounters = repoB.getEncountersForPatient("uuid-beta")
            assertEquals(2, encounters.size, "Both visits should be linked under canonical uuid-beta")
            val encIds = encounters.mapNotNull { it.id }.toSet()
            assertTrue(encIds.contains("enc-alpha-visit"))
            assertTrue(encIds.contains("enc-beta-visit"))
        }
}
