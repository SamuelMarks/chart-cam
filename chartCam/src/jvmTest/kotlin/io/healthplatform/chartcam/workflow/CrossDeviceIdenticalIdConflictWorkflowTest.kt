/**
 * @file CrossDeviceIdenticalIdConflictWorkflowTest.kt
 * End-to-end workflow tests for cross-device identical ID collision handling and merge resolution.
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
import io.healthplatform.chartcam.models.createFhirPractitioner
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
 * End-to-end workflow suite testing cross-device patient ID collisions and non-destructive resolution strategies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CrossDeviceIdenticalIdConflictWorkflowTest {
    /**
     * In-memory mock storage implementation for tests.
     */
    private class MockFileStorage : FileStorage {
        val files = mutableMapOf<String, ByteArray>()

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            files[fileName] = bytes
            return fileName
        }

        override fun readImage(path: String): ByteArray = files[path] ?: ByteArray(0)

        override fun deleteImage(path: String): Result<Unit> =
            if (files.remove(path) != null) Result.success(Unit) else Result.failure(Exception("File not found: $path"))

        override fun clearCache() {
            files.clear()
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var driverA: JdbcSqliteDriver
    private lateinit var driverB: JdbcSqliteDriver
    private lateinit var repoA: FhirRepository
    private lateinit var repoB: FhirRepository
    private lateinit var exportImportA: ExportImportService
    private lateinit var exportImportB: ExportImportService

    /**
     * Initializes simulated isolated devices A and B.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Device A (e.g. Android)
        driverA = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driverA)
        val dbA = ChartCamDatabase(driverA)
        repoA = FhirRepository(dbA)
        exportImportA = ExportImportService(dbA, MockFileStorage())

        // Device B (e.g. iOS)
        driverB = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driverB)
        val dbB = ChartCamDatabase(driverB)
        repoB = FhirRepository(dbB)
        exportImportB = ExportImportService(dbB, MockFileStorage())
    }

    /**
     * Cleans up driver resources and resets test dispatchers.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driverA.close()
        driverB.close()
    }

    /**
     * Tests that an incoming archive with identical patient ID and differing details
     * is detected as an ID collision and can be safely merged without losing local data.
     */
    @Test
    fun testIdenticalIdConflictMergeWorkflow() =
        runTest(testDispatcher) {
            val practitioner = createFhirPractitioner("dr-carter", "Carter", "John", true)
            repoA.savePractitioner(practitioner)
            repoB.savePractitioner(practitioner)

            // Device A creates patient-0 with baseline visit
            val patientA =
                createFhirPatient(
                    id = "patient-0",
                    firstName = "Alice",
                    lastName = "Smith",
                    dob = LocalDate(1985, 5, 20),
                    mrnValue = "MRN-ALICE",
                )
            repoA.savePatient(patientA)
            val encA = createFhirEncounter("enc-device-a", "patient-0", "dr-carter", "2026-09-14T10:00:00Z")
            repoA.saveEncounter(encA)

            // Device B creates patient-0 with updated demographics and followup visit
            val patientB =
                createFhirPatient(
                    id = "patient-0",
                    firstName = "Alice",
                    lastName = "Smith-Jones",
                    dob = LocalDate(1985, 5, 20),
                    mrnValue = "MRN-ALICE",
                )
            repoB.savePatient(patientB)
            val encB = createFhirEncounter("enc-device-b", "patient-0", "dr-carter", "2026-09-14T11:00:00Z")
            repoB.saveEncounter(encB)

            // Step 1: Device A exports archive
            val archivePassword = "TestPassword2026!"
            val exportedArchive = exportImportA.exportData(archivePassword, exportAll = true).getOrThrow()

            // Step 2: Device B inspects archive and verifies collision detection
            val preview = exportImportB.inspectArchive(exportedArchive, archivePassword).getOrThrow()
            assertTrue(preview.hasConflicts, "Archive should flag collision on patient-0")
            assertEquals(1, preview.stagedPatients.size)
            assertEquals(ConflictType.ID_COLLISION_DIFFERENT_DATA, preview.stagedPatients[0].conflictType)

            // Step 3: Device B imports with MERGE_RECORDS strategy
            exportImportB
                .importDataSelective(
                    encryptedData = exportedArchive,
                    password = archivePassword,
                    resolutionMap = mapOf("patient-0" to ConflictResolutionStrategy.MERGE_RECORDS),
                ).getOrThrow()

            // Step 4: Verify Device B retains local demographic state and contains BOTH encounters
            val resolvedPatient = repoB.getPatient("patient-0")
            assertNotNull(resolvedPatient)
            assertEquals(
                "Smith-Jones",
                resolvedPatient.name
                    .firstOrNull()
                    ?.family
                    ?.value,
            )

            val bEncounters = repoB.getEncountersForPatient("patient-0")
            assertEquals(2, bEncounters.size, "Both visits from Device A and Device B should be linked to patient-0")
            val encounterIds = bEncounters.mapNotNull { it.id }.toSet()
            assertTrue(encounterIds.contains("enc-device-a"))
            assertTrue(encounterIds.contains("enc-device-b"))
        }

    /**
     * Tests that KEEP_LOCAL strategy preserves local records and skips overwriting with incoming conflicting data.
     */
    @Test
    fun testIdenticalIdConflictKeepLocalWorkflow() =
        runTest(testDispatcher) {
            val patientA = createFhirPatient("p-same", "Bob", "Old", LocalDate(1980, 1, 1), "MRN-BOB")
            repoA.savePatient(patientA)
            val encA = createFhirEncounter("enc-a", "p-same", "dr", "2026-09-14T10:00:00Z")
            repoA.saveEncounter(encA)

            val patientB = createFhirPatient("p-same", "Bob", "Current", LocalDate(1980, 1, 1), "MRN-BOB")
            repoB.savePatient(patientB)
            val encB = createFhirEncounter("enc-b", "p-same", "dr", "2026-09-14T11:00:00Z")
            repoB.saveEncounter(encB)

            val exported = exportImportA.exportData("TestPass123", exportAll = true).getOrThrow()
            exportImportB
                .importDataSelective(
                    encryptedData = exported,
                    password = "TestPass123",
                    resolutionMap = mapOf("p-same" to ConflictResolutionStrategy.KEEP_LOCAL),
                ).getOrThrow()

            val localBob = repoB.getPatient("p-same")
            assertNotNull(localBob)
            assertEquals(
                "Current",
                localBob.name
                    .firstOrNull()
                    ?.family
                    ?.value,
            )
            val encounters = repoB.getEncountersForPatient("p-same")
            assertEquals(1, encounters.size)
            assertEquals("enc-b", encounters[0].id)
        }
}
