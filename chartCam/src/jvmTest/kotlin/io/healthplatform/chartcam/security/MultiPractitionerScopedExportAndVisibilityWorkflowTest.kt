/**
 * @file MultiPractitionerScopedExportAndVisibilityWorkflowTest.kt
 * Contains declarations for MultiPractitionerScopedExportAndVisibilityWorkflowTest.kt.
 *
 * Validates practitioner-scoped data segregation, patient roster visibility before and after encounters,
 * and scoped encrypted bundle export integrity.
 */
package io.healthplatform.chartcam.security

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Bundle
import com.google.fhir.model.r4.FhirR4Json
import com.google.fhir.model.r4.Patient
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPractitioner
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.utils.CryptoService
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * End-to-End workflow tests verifying multi-practitioner data boundaries, patient list filtering, and export scoping.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MultiPractitionerScopedExportAndVisibilityWorkflowTest {
    /**
     * Minimal in-memory file storage double.
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
    private lateinit var db: ChartCamDatabase
    private lateinit var fhirRepo: FhirRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var exportImportService: ExportImportService
    private val cryptoService = CryptoService()
    private val fhirJson = FhirR4Json()

    /**
     * Initializes test database and authentication repository.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
        val storage = JvmSecureStorage("test_multi_${java.util.UUID.randomUUID()}")
        authRepo = AuthRepository(storage)
        exportImportService = ExportImportService(db, DummyFileStorage())
    }

    /**
     * Tears down test environment.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    /**
     * Verifies scoped patient creation, roster visibility, and practitioner-filtered bundle export.
     */
    @Test
    fun testPractitionerScopedVisibilityAndExport() =
        runTest(testDispatcher) {
            val prac1 = createFhirPractitioner("prac-carter", "Carter", "John", true)
            val prac2 = createFhirPractitioner("prac-ross", "Ross", "Doug", true)
            fhirRepo.savePractitioner(prac1)
            fhirRepo.savePractitioner(prac2)

            // Step 1: Login as Dr. Carter
            val carterPrac = authRepo.login("prac-carter", "password").getOrThrow()
            val carterId = carterPrac.id ?: ""
            testDispatcher.scheduler.advanceUntilIdle()

            val carterPatientListVm = PatientListViewModel(fhirRepo, exportImportService, authRepo)
            carterPatientListVm.setShowAllPatients(false)
            testDispatcher.scheduler.advanceUntilIdle()

            // Step 2: Create Patient Zero by Dr. Carter
            var createdPatientId = ""
            carterPatientListVm.createPatient(
                firstName = "Zero",
                lastName = "Patient",
                mrn = "MRN-000",
                dob = LocalDate(1990, 1, 1),
                onSuccess = { createdPatientId = it },
            )
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(createdPatientId.isNotEmpty())
            val savedPatient = fhirRepo.getPatient(createdPatientId)
            assertEquals(carterId, savedPatient?.managingOrganization?.reference?.value)

            // Patient Zero must appear on Carter's roster even with showAll = false and no encounters yet
            carterPatientListVm.loadPatients()
            testDispatcher.scheduler.advanceUntilIdle()
            val carterPatients = carterPatientListVm.uiState.value.patients
            assertTrue(carterPatients.any { it.id == createdPatientId })

            // Step 3: Export data for Dr. Carter
            val password = "SecKey123"
            val encryptedCarterExport =
                exportImportService
                    .exportData(
                        password = password,
                        exportAll = false,
                        practitionerId = carterId,
                    ).getOrThrow()
            val decryptedCarterJson = cryptoService.decrypt(encryptedCarterExport, password)
            val carterBundle = fhirJson.decodeFromString(decryptedCarterJson) as Bundle
            val exportedPatientIds =
                carterBundle.entry
                    .mapNotNull { (it.resource as? Patient)?.id }
            assertTrue(exportedPatientIds.contains(createdPatientId))

            // Step 4: Login as Dr. Ross
            val rossPrac = authRepo.login("prac-ross", "password").getOrThrow()
            val rossId = rossPrac.id ?: ""
            testDispatcher.scheduler.advanceUntilIdle()

            val rossPatientListVm = PatientListViewModel(fhirRepo, exportImportService, authRepo)
            rossPatientListVm.setShowAllPatients(false)
            testDispatcher.scheduler.advanceUntilIdle()

            // Patient Zero must NOT appear on Dr. Ross's scoped roster
            val rossPatients = rossPatientListVm.uiState.value.patients
            assertFalse(rossPatients.any { it.id == createdPatientId })

            // Dr. Ross export must not contain Patient Zero
            val encryptedRossExport =
                exportImportService
                    .exportData(
                        password = password,
                        exportAll = false,
                        practitionerId = rossId,
                    ).getOrThrow()
            val decryptedRossJson = cryptoService.decrypt(encryptedRossExport, password)
            val rossBundle = fhirJson.decodeFromString(decryptedRossJson) as Bundle
            val rossExportedPatientIds =
                rossBundle.entry
                    .mapNotNull { (it.resource as? Patient)?.id }
            assertFalse(rossExportedPatientIds.contains(createdPatientId))

            // Step 5: Dr. Ross sees Patient Zero in an encounter
            val rossEncounter =
                createFhirEncounter(
                    id = "enc-ross-1",
                    patientId = createdPatientId,
                    practitionerId = rossId,
                    dateStr = "2024-05-01T12:00:00Z",
                )
            fhirRepo.saveEncounter(rossEncounter)

            // Now Patient Zero should appear in Dr. Ross's scoped roster
            rossPatientListVm.loadPatients()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(
                rossPatientListVm.uiState.value.patients
                    .any { it.id == createdPatientId },
            )

            // Re-export for Dr. Ross now includes Patient Zero
            val updatedRossExport =
                exportImportService
                    .exportData(
                        password = password,
                        exportAll = false,
                        practitionerId = rossId,
                    ).getOrThrow()
            val updatedRossJson = cryptoService.decrypt(updatedRossExport, password)
            val updatedRossBundle = fhirJson.decodeFromString(updatedRossJson) as Bundle
            val updatedRossPatientIds =
                updatedRossBundle.entry
                    .mapNotNull { (it.resource as? Patient)?.id }
            assertTrue(updatedRossPatientIds.contains(createdPatientId))
        }
}
