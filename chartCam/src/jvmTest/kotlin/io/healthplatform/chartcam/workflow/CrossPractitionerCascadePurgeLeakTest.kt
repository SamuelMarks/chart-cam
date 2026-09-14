/**
 * @file CrossPractitionerCascadePurgeLeakTest.kt
 * End-to-end workflow test verifying cross-practitioner account deletion isolation.
 */
package io.healthplatform.chartcam.workflow

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.viewmodel.PatientListViewModel
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
 * Validates that deleting a practitioner account does not purge shared patients or visits authored by colleagues.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CrossPractitionerCascadePurgeLeakTest {
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
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repo: FhirRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var exportImport: ExportImportService

    /**
     * Prepares test database and authentication context.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        val db = ChartCamDatabase(driver)
        repo = FhirRepository(db)
        authRepo = AuthRepository(JvmSecureStorage("test_purge_${java.util.UUID.randomUUID()}"))
        exportImport = ExportImportService(db, DummyFileStorage())
    }

    /**
     * Cleans up driver.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    /**
     * Verifies that deleting Dr. Ross's account preserves the shared patient and Dr. Carter's visit.
     */
    @Test
    fun testSharedPatientPreservedWhenOnePractitionerDeletesAccount() =
        runTest(testDispatcher) {
            // Setup practitioners through auth
            val drRoss = authRepo.login("ross", "Password123").getOrThrow()
            val rossId = drRoss.id ?: ""
            repo.savePractitioner(drRoss)

            val drCarter = authRepo.login("carter", "Password123").getOrThrow()
            val carterId = drCarter.id ?: ""
            repo.savePractitioner(drCarter)

            // Shared patient with encounters from both doctors
            val patient =
                createFhirPatient(
                    id = "shared-patient",
                    firstName = "Sam",
                    lastName = "Shared",
                    dob = LocalDate(1978, 11, 23),
                    mrnValue = "MRN-SHARED-01",
                )
            repo.savePatient(patient)

            val encRoss = createFhirEncounter("enc-ross", "shared-patient", rossId, "2026-09-14T10:00:00Z")
            val encCarter = createFhirEncounter("enc-carter", "shared-patient", carterId, "2026-09-14T11:00:00Z")
            repo.saveEncounter(encRoss)
            repo.saveEncounter(encCarter)

            // Log in as Dr. Ross
            authRepo.login("ross", "Password123")
            testDispatcher.scheduler.advanceUntilIdle()

            val vm = PatientListViewModel(repo, exportImport, authRepo)
            testDispatcher.scheduler.advanceUntilIdle()

            // Trigger deleteAccount as Dr. Ross
            var deletedSuccess = false
            vm.deleteAccount { deletedSuccess = true }
            testDispatcher.scheduler.advanceUntilIdle()

            // Verification:
            // 1. Deletion completed
            assertEquals(true, deletedSuccess)

            // 2. Dr. Ross practitioner record is deleted
            assertNull(repo.getPractitioner(rossId))

            // 3. Dr. Ross's visit is deleted
            assertNull(repo.getEncounter("enc-ross"))

            // 4. The shared patient is NOT deleted
            assertNotNull(repo.getPatient("shared-patient"), "Shared patient must not be wiped out")

            // 5. Dr. Carter's visit remains intact
            assertNotNull(repo.getEncounter("enc-carter"), "Colleague visit must remain intact")
        }
}
