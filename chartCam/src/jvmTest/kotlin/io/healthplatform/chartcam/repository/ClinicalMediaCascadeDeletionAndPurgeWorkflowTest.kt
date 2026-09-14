/**
 * @file ClinicalMediaCascadeDeletionAndPurgeWorkflowTest.kt
 * Contains declarations for ClinicalMediaCascadeDeletionAndPurgeWorkflowTest.kt.
 *
 * Validates cascading deletion of encounters, linked document references,
 * questionnaire responses, and on-disk clinical media files to prevent orphan resource leaks.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.terminologies.AdministrativeGender
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.DocumentReferenceCreationParams
import io.healthplatform.chartcam.models.createFhirDocumentReference
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPractitioner
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.viewmodel.EncounterDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.google.fhir.model.r4.String as FhirString

/**
 * End-to-End workflow tests verifying cascade deletion of clinical media and FHIR records.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClinicalMediaCascadeDeletionAndPurgeWorkflowTest {
    /**
     * FileStorage double tracking physical disk deletion.
     */
    private class TrackingFileStorage : FileStorage {
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
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: ChartCamDatabase
    private lateinit var fhirRepo: FhirRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var questionnaireRepo: QuestionnaireRepository
    private lateinit var fileStorage: TrackingFileStorage

    /**
     * Initializes test database and tracking storage.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
        val storage = JvmSecureStorage("test_cascade_${java.util.UUID.randomUUID()}")
        authRepo = AuthRepository(storage)
        questionnaireRepo = QuestionnaireRepository()
        fileStorage = TrackingFileStorage()
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
     * Verifies that deleting an encounter deletes linked DocumentReferences, QuestionnaireResponses, and image files.
     */
    @Test
    fun testEncounterCascadeDeletionPurgesMediaAndReferences() =
        runTest(testDispatcher) {
            val prac = createFhirPractitioner("prac-gandalf", "Gandalf", "The Grey", true)
            fhirRepo.savePractitioner(prac)
            authRepo.login("prac-gandalf", "magic")
            testDispatcher.scheduler.advanceUntilIdle()

            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = "p-cascade"
                        name.add(
                            HumanName.Builder().apply {
                                family = FhirString.Builder().apply { value = "Baggins" }
                                given.add(FhirString.Builder().apply { value = "Bilbo" })
                            },
                        )
                        gender = Enumeration(value = AdministrativeGender.Male)
                    }.build()
            fhirRepo.savePatient(patient)

            val enc =
                createFhirEncounter(
                    id = "enc-del",
                    patientId = "p-cascade",
                    practitionerId = "prac-gandalf",
                    dateStr = "2024-06-01T10:00:00Z",
                )
            fhirRepo.saveEncounter(enc)

            val dummyPhoto = byteArrayOf(0x01, 0x02, 0x03)
            fileStorage.saveImage("pic1.jpg", dummyPhoto)
            fileStorage.saveImage("pic2.jpg", dummyPhoto)
            fileStorage.saveImage("pic3.jpg", dummyPhoto)

            val d1 =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams("d1", "p-cascade", "enc-del", "2024-06-01", "P1", "image/jpeg", "pic1.jpg", "code1"),
                )
            val d2 =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams("d2", "p-cascade", "enc-del", "2024-06-01", "P2", "image/jpeg", "pic2.jpg", "code2"),
                )
            val d3 =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams("d3", "p-cascade", "enc-del", "2024-06-01", "P3", "image/jpeg", "pic3.jpg", "code3"),
                )
            fhirRepo.saveDocumentReference(d1)
            fhirRepo.saveDocumentReference(d2)
            fhirRepo.saveDocumentReference(d3)

            val qr =
                QuestionnaireResponse
                    .Builder(Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
                    .apply {
                        id = "qr-del"
                        encounter =
                            com.google.fhir.model.r4.Reference
                                .Builder()
                                .apply {
                                    reference = FhirString.Builder().apply { value = "Encounter/enc-del" }
                                }
                    }.build()
            fhirRepo.saveQuestionnaireResponse(qr)

            // Verify resources exist initially
            assertEquals(3, fhirRepo.getPhotosForEncounter("enc-del").size)
            assertEquals(1, fhirRepo.getQuestionnaireResponsesForEncounter("enc-del").size)
            assertEquals(3, fileStorage.files.size)

            val vm = EncounterDetailViewModel(fhirRepo, authRepo, questionnaireRepo, fileStorage = fileStorage)
            vm.initialize("p-cascade", "enc-del", emptyMap())
            testDispatcher.scheduler.advanceUntilIdle()

            var backCalled = false
            vm.deleteEncounter { backCalled = true }
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(backCalled)
            assertNull(fhirRepo.getEncounter("enc-del"))
            assertEquals(0, fhirRepo.getPhotosForEncounter("enc-del").size)
            assertEquals(0, fhirRepo.getQuestionnaireResponsesForEncounter("enc-del").size)
            assertEquals(0, fileStorage.files.size, "Physical files on disk must be completely purged")
        }

    /**
     * Verifies that deleting a patient cascades deletion to all encounters, documents, and storage files.
     */
    @Test
    fun testPatientCascadeDeletionPurgesAllChildResources() =
        runTest(testDispatcher) {
            val patient =
                Patient
                    .Builder()
                    .apply {
                        id = "p-full-delete"
                    }.build()
            fhirRepo.savePatient(patient)

            val enc =
                createFhirEncounter(
                    id = "enc-child",
                    patientId = "p-full-delete",
                    practitionerId = "prac-1",
                    dateStr = "2024-06-01T10:00:00Z",
                )
            fhirRepo.saveEncounter(enc)

            fileStorage.saveImage("child_photo.jpg", byteArrayOf(0x05, 0x06))
            val d =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        "doc-child",
                        "p-full-delete",
                        "enc-child",
                        "2024-06-01",
                        "Child photo",
                        "image/jpeg",
                        "child_photo.jpg",
                        "c1",
                    ),
                )
            fhirRepo.saveDocumentReference(d)

            // Delete patient with cascading fileStorage
            fhirRepo.deletePatient("p-full-delete", fileStorage)

            assertNull(fhirRepo.getPatient("p-full-delete"))
            assertNull(fhirRepo.getEncounter("enc-child"))
            assertEquals(0, fhirRepo.getPhotosForEncounter("enc-child").size)
            assertFalse(fileStorage.files.containsKey("child_photo.jpg"))
        }
}
