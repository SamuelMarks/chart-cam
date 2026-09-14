/**
 * @file MultiPractitionerDataIsolationTest.kt
 * Contains declarations for MultiPractitionerDataIsolationTest.kt.
 *
 * Validates data isolation boundaries, patient roster scoping, encounter attribution,
 * and secure credential isolation across distinct clinical practitioners.
 */
package io.healthplatform.chartcam.security

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.viewmodel.EncounterDetailViewModel
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-End workflow tests verifying strict multi-tenant practitioner isolation,
 * session wiping upon logout, and practitioner attribution on clinical encounters.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MultiPractitionerDataIsolationTest {
    /**
     * In-memory file storage mock.
     */
    private class MockIsolationFileStorage : FileStorage {
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

    /**
     * Sets up test dispatchers.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Cleans up test dispatchers.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Tests multi-practitioner clinical boundaries:
     * 1. Dr. Alice logs in, provisions Patient A, creates Encounter A1.
     * 2. Dr. Alice logs out (verifies session state wiped).
     * 3. Dr. Bob logs in, asserts default scoped roster excludes Patient A.
     * 4. Dr. Bob provisions Patient B, verifies Encounter B1 attributes to Dr. Bob.
     * 5. Asserts storage isolation between practitioner cryptographic credential keys.
     */
    @Test
    fun testMultiPractitionerDataScopingAndCredentialIsolation() =
        runTest(testDispatcher) {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val database = ChartCamDatabase(driver)
            val fhirRepo = FhirRepository(database)
            val fileStorage = MockIsolationFileStorage()
            val exportImportService = ExportImportService(database, fileStorage)
            val questionnaireRepo = QuestionnaireRepository()
            questionnaireRepo.loadDefaultForms()

            val storage = JvmSecureStorage("multi_prac_${java.util.UUID.randomUUID()}")
            val authRepo = AuthRepository(storage)

            // Step 1: Authenticate Dr. Alice (ID: prac_alice)
            authRepo.login("dr_alice", "AliceSecretPass123!")
            testDispatcher.scheduler.advanceUntilIdle()
            val alicePractitioner = authRepo.currentUser.value
            assertNotNull(alicePractitioner)
            val aliceId = alicePractitioner.id ?: ""

            // Dr. Alice creates Patient A
            val alicePatientListVm = PatientListViewModel(fhirRepo, exportImportService, authRepo)
            alicePatientListVm.setShowAllPatients(false)
            testDispatcher.scheduler.advanceUntilIdle()

            var patientAId: String? = null
            alicePatientListVm.createPatient(
                firstName = "Arthur",
                lastName = "Dent",
                mrn = "MRN-ALICE-01",
                dob = LocalDate(1980, 1, 1),
            ) { createdId ->
                patientAId = createdId
            }
            testDispatcher.scheduler.advanceUntilIdle()
            assertNotNull(patientAId)

            // Dr. Alice creates Encounter A1
            val aliceEncounterVm = EncounterDetailViewModel(fhirRepo, authRepo, questionnaireRepo)
            aliceEncounterVm.initialize(patientAId, "new", emptyMap())
            testDispatcher.scheduler.advanceUntilIdle()
            val encounterA = aliceEncounterVm.uiState.value.encounter
            assertNotNull(encounterA)
            assertEquals(
                aliceId,
                encounterA.participant
                    .firstOrNull()
                    ?.individual
                    ?.reference
                    ?.value
                    ?.removePrefix("Practitioner/"),
            )

            // Verify Dr. Alice sees Patient A in scoped roster
            alicePatientListVm.loadPatients()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(
                alicePatientListVm.uiState.value.patients
                    .any { it.id == patientAId },
            )

            // Step 2: Dr. Alice logs out
            authRepo.logout()
            testDispatcher.scheduler.advanceUntilIdle()
            assertNull(authRepo.currentUser.value, "Current user must be wiped upon logout")

            // Step 3: Authenticate Dr. Bob (ID: prac_bob)
            authRepo.login("dr_bob", "BobSecretPass456!")
            testDispatcher.scheduler.advanceUntilIdle()
            val bobPractitioner = authRepo.currentUser.value
            assertNotNull(bobPractitioner)
            val bobId = bobPractitioner.id ?: ""
            assertTrue(bobId != aliceId, "Dr. Bob must have distinct practitioner ID from Dr. Alice")

            // Dr. Bob inspects scoped patient roster (showAllPatients = false)
            val bobPatientListVm = PatientListViewModel(fhirRepo, exportImportService, authRepo)
            bobPatientListVm.setShowAllPatients(false)
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(
                bobPatientListVm.uiState.value.patients
                    .any { it.id == patientAId },
                "Dr. Bob must not see Dr. Alice's private patients in default scoped list",
            )

            // Step 4: Dr. Bob creates Patient B and Encounter B1
            var patientBId: String? = null
            bobPatientListVm.createPatient(
                firstName = "Ford",
                lastName = "Prefect",
                mrn = "MRN-BOB-01",
                dob = LocalDate(1982, 2, 2),
            ) { createdId ->
                patientBId = createdId
            }
            testDispatcher.scheduler.advanceUntilIdle()
            assertNotNull(patientBId)

            val bobEncounterVm = EncounterDetailViewModel(fhirRepo, authRepo, questionnaireRepo)
            bobEncounterVm.initialize(patientBId, "new", emptyMap())
            testDispatcher.scheduler.advanceUntilIdle()
            val encounterB = bobEncounterVm.uiState.value.encounter
            assertNotNull(encounterB)
            assertEquals(
                bobId,
                encounterB.participant
                    .firstOrNull()
                    ?.individual
                    ?.reference
                    ?.value
                    ?.removePrefix("Practitioner/"),
            )

            // Step 5: Secure credential and salt isolation in SecureStorage
            val aliceCredentialHash = storage.getString("hash_dr_alice")
            val bobCredentialHash = storage.getString("hash_dr_bob")
            assertNotNull(aliceCredentialHash)
            assertNotNull(bobCredentialHash)
            assertTrue(
                aliceCredentialHash != bobCredentialHash,
                "Cryptographic credential hashes must be strictly isolated per practitioner",
            )

            driver.close()
            storage.clearAll()
        }
}
