/**
 * @file BatchPatientMergeChecklistWorkflowTest.kt
 * End-to-end workflow test verifying batch patient checklist selection and selective ingestion.
 */
package io.healthplatform.chartcam.workflow

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
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
import kotlin.test.assertNull

/**
 * Validates batch patient selection workflows where clinicians selectively import a subset from an archive.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BatchPatientMergeChecklistWorkflowTest {
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
     * Initializes test databases.
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
     * Cleans up driver connections.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driverA.close()
        driverB.close()
    }

    /**
     * Verifies that selecting 2 patients out of 5 ingests only the selected patients and their associated encounters.
     */
    @Test
    fun testSelectiveBatchPatientImport() =
        runTest(testDispatcher) {
            // Populate 5 patients with encounters on Device A
            for (i in 1..5) {
                val pid = "patient-batch-$i"
                val pat = createFhirPatient(pid, "Given$i", "Family$i", LocalDate(1990, 1, i), "MRN-BATCH-$i")
                repoA.savePatient(pat)
                val enc = createFhirEncounter("enc-batch-$i", pid, "dr-who", "2026-09-14T10:0$i:00Z")
                repoA.saveEncounter(enc)
            }

            val pass = "PasswordBatch2026"
            val archive = exportImportA.exportData(pass, exportAll = true).getOrThrow()

            // Step 1: Inspect archive on Device B
            val preview = exportImportB.inspectArchive(archive, pass).getOrThrow()
            assertEquals(5, preview.stagedPatients.size)

            // Step 2: Select only patient-batch-2 and patient-batch-4
            val chosenBatch = setOf("patient-batch-2", "patient-batch-4")
            exportImportB
                .importDataSelective(
                    encryptedData = archive,
                    password = pass,
                    selectedPatientIds = chosenBatch,
                ).getOrThrow()

            // Step 3: Assert selected patients are present
            assertNotNull(repoB.getPatient("patient-batch-2"))
            assertNotNull(repoB.getPatient("patient-batch-4"))
            assertNotNull(repoB.getEncounter("enc-batch-2"))
            assertNotNull(repoB.getEncounter("enc-batch-4"))

            // Step 4: Assert unselected patients are completely absent
            assertNull(repoB.getPatient("patient-batch-1"))
            assertNull(repoB.getPatient("patient-batch-3"))
            assertNull(repoB.getPatient("patient-batch-5"))
            assertNull(repoB.getEncounter("enc-batch-1"))
            assertNull(repoB.getEncounter("enc-batch-3"))
            assertNull(repoB.getEncounter("enc-batch-5"))
        }
}
