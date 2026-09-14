/**
 * @file MultiDeviceBiDirectionalMergeRoundTripTest.kt
 * End-to-end workflow test verifying bidirectional multi-device data exchange, concurrent editing, and reconciliation.
 */
package io.healthplatform.chartcam.workflow

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.ConflictResolutionStrategy
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
import kotlin.test.assertTrue

/**
 * Validates bidirectional round-trip data reconciliation between two distinct mobile devices.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MultiDeviceBiDirectionalMergeRoundTripTest {
    /**
     * FileStorage stub.
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
     * Prepares dual device test databases.
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
     * Cleans up driver instances.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driverA.close()
        driverB.close()
    }

    /**
     * Tests full bidirectional synchronization and reconciliation:
     * A -> B, concurrent edits on both, B -> A with merge, A -> B with merge.
     */
    @Test
    fun testBidirectionalSyncAndMergeRoundTrip() =
        runTest(testDispatcher) {
            val pass = "RoundTripPass2026!"

            // Step 1: Device A creates patient and Visit 1
            val patient =
                createFhirPatient(
                    id = "p-roundtrip",
                    firstName = "Diana",
                    lastName = "Prince",
                    dob = LocalDate(1984, 1, 1),
                    mrnValue = "MRN-DIANA",
                )
            repoA.savePatient(patient)
            val visit1 = createFhirEncounter("visit-1-initial", "p-roundtrip", "dr-who", "2026-09-14T10:00:00Z")
            repoA.saveEncounter(visit1)

            // Step 2: Device A exports to Device B
            val exportAtoB = exportImportA.exportData(pass, exportAll = true).getOrThrow()
            exportImportB.importData(exportAtoB, pass).getOrThrow()

            // Verify Device B has patient and Visit 1
            assertEquals(1, repoB.getEncountersForPatient("p-roundtrip").size)

            // Step 3: Concurrent edits:
            // Device A records Visit 2
            val visit2 = createFhirEncounter("visit-2-device-a", "p-roundtrip", "dr-who", "2026-09-14T11:00:00Z")
            repoA.saveEncounter(visit2)

            // Device B records Visit 3
            val visit3 = createFhirEncounter("visit-3-device-b", "p-roundtrip", "dr-who", "2026-09-14T12:00:00Z")
            repoB.saveEncounter(visit3)

            // Step 4: Device B exports and Device A imports with MERGE_RECORDS
            val exportBtoA = exportImportB.exportData(pass, exportAll = true).getOrThrow()
            exportImportA
                .importDataSelective(
                    encryptedData = exportBtoA,
                    password = pass,
                    resolutionMap = mapOf("p-roundtrip" to ConflictResolutionStrategy.MERGE_RECORDS),
                ).getOrThrow()

            // Verify Device A now has all 3 visits
            val aEncounters = repoA.getEncountersForPatient("p-roundtrip")
            assertEquals(3, aEncounters.size, "Device A should have merged visits 1, 2, and 3")
            val aEncIds = aEncounters.mapNotNull { it.id }.toSet()
            assertTrue(aEncIds.contains("visit-1-initial"))
            assertTrue(aEncIds.contains("visit-2-device-a"))
            assertTrue(aEncIds.contains("visit-3-device-b"))

            // Step 5: Device A exports back to Device B
            val finalExportAtoB = exportImportA.exportData(pass, exportAll = true).getOrThrow()
            exportImportB
                .importDataSelective(
                    encryptedData = finalExportAtoB,
                    password = pass,
                    resolutionMap = mapOf("p-roundtrip" to ConflictResolutionStrategy.MERGE_RECORDS),
                ).getOrThrow()

            // Verify Device B also converges to all 3 visits
            val bEncounters = repoB.getEncountersForPatient("p-roundtrip")
            assertEquals(3, bEncounters.size, "Device B should have converged to all 3 visits")
            val bEncIds = bEncounters.mapNotNull { it.id }.toSet()
            assertTrue(bEncIds.contains("visit-1-initial"))
            assertTrue(bEncIds.contains("visit-2-device-a"))
            assertTrue(bEncIds.contains("visit-3-device-b"))
        }
}
