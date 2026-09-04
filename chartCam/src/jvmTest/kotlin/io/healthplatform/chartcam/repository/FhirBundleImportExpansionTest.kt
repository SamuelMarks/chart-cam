/**
 * @file FhirBundleImportExpansionTest.kt
 * Contains declarations for FhirBundleImportExpansionTest.kt.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Bundle
import com.google.fhir.model.r4.Canonical
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Extension
import com.google.fhir.model.r4.FhirR4Json
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.Reference
import com.google.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.utils.CryptoService
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.google.fhir.model.r4.Boolean as FhirBoolean
import com.google.fhir.model.r4.String as FhirString

/**
 * Unit tests covering Section 2: FHIR Bundle Large Imports & Streaming.
 */
class FhirBundleImportExpansionTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var fhirRepo: FhirRepository
    private lateinit var service: ExportImportService
    private lateinit var fileStorage: FakeFileStorage
    private val cryptoService = CryptoService()
    private val fhirJson = FhirR4Json()
    private val password = "TestPassword123"

    /**
     * In-memory test storage for images.
     */
    private class FakeFileStorage : FileStorage {
        val files = mutableMapOf<String, ByteArray>()

        override fun saveImage(
            fileName: String,
            bytes: ByteArray,
        ): String {
            files[fileName] = bytes
            return "/fake/path/$fileName"
        }

        override fun readImage(path: String): ByteArray = files[path.substringAfterLast("/")] ?: ByteArray(0)

        override fun clearCache() {
            files.clear()
        }
    }

    /**
     * Setup in-memory database and test services.
     */
    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
        fileStorage = FakeFileStorage()
        service = ExportImportService(db, fileStorage)
    }

    /**
     * Test parsing and persistence of multi-megabyte FHIR bundles (1000+ resources)
     * without triggering OutOfMemoryError.
     */
    @Test
    fun testLargeBundleMemoryPressure() =
        runTest {
            val bundleBuilder = Bundle.Builder(Enumeration(value = Bundle.BundleType.Collection))

            // Build 1000 total resources: 500 Patients + 500 Encounters
            val resourceCount = 500
            for (i in 1..resourceCount) {
                val patient =
                    Patient
                        .Builder()
                        .apply {
                            id = "patient_large_$i"
                            active = FhirBoolean.Builder().apply { value = true }
                            name.add(
                                HumanName.Builder().apply {
                                    family = FhirString.Builder().apply { value = "LargeFamily_$i" }
                                    given.add(FhirString.Builder().apply { value = "LargeGiven_$i" })
                                },
                            )
                        }.build()
                bundleBuilder.entry.add(Bundle.Entry.Builder().apply { resource = patient.toBuilder() })

                val encounter =
                    createFhirEncounter(
                        id = "encounter_large_$i",
                        patientId = "patient_large_$i",
                        practitionerId = "practitioner_1",
                        dateStr = "2026-09-01T10:00:00Z",
                    )
                bundleBuilder.entry.add(Bundle.Entry.Builder().apply { resource = encounter.toBuilder() })
            }

            val bundle = bundleBuilder.build()
            assertEquals(1000, bundle.entry.size)

            val jsonString = fhirJson.encodeToString(bundle)
            assertTrue(jsonString.length > 100_000, "Bundle JSON should be substantial")

            val encrypted = cryptoService.encrypt(jsonString, password)
            service.importData(encrypted, password)

            val importedPatients = fhirRepo.getAllPatients()
            val importedEncounters = fhirRepo.getAllEncounters()

            assertEquals(500, importedPatients.size)
            assertEquals(500, importedEncounters.size)

            val samplePatient = fhirRepo.getPatient("patient_large_250")
            assertNotNull(samplePatient)
            assertEquals(
                "LargeFamily_250",
                samplePatient.name
                    .first()
                    .family
                    ?.value,
            )
        }

    /**
     * Ensure the import pipeline logs errors, skips corrupt entries, and commits valid resources
     * rather than failing the entire transaction.
     */
    @Test
    fun testMalformedResourceRecovery() =
        runTest {
            val bundleBuilder = Bundle.Builder(Enumeration(value = Bundle.BundleType.Collection))

            // 1. Valid Patient 1
            val validPatient1 =
                Patient
                    .Builder()
                    .apply {
                        id = "valid_patient_1"
                        active = FhirBoolean.Builder().apply { value = true }
                    }.build()
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { resource = validPatient1.toBuilder() })

            // 2. Corrupt Binary (null ID)
            val corruptBinary =
                com.google.fhir.model.r4.Binary
                    .Builder(
                        contentType =
                            com.google.fhir.model.r4.Code
                                .Builder()
                                .apply { value = "image/jpeg" },
                    ).apply {
                        // ID intentionally omitted / null
                    }.build()
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { resource = corruptBinary.toBuilder() })

            // 3. Valid Patient 2
            val validPatient2 =
                Patient
                    .Builder()
                    .apply {
                        id = "valid_patient_2"
                        active = FhirBoolean.Builder().apply { value = true }
                    }.build()
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { resource = validPatient2.toBuilder() })

            val bundle = bundleBuilder.build()
            val encrypted = cryptoService.encrypt(fhirJson.encodeToString(bundle), password)

            // Importing must not throw and must commit both valid patients
            service.importData(encrypted, password)

            val patient1 = fhirRepo.getPatient("valid_patient_1")
            val patient2 = fhirRepo.getPatient("valid_patient_2")
            assertNotNull(patient1, "Valid patient 1 should be committed")
            assertNotNull(patient2, "Valid patient 2 should be committed")
        }

    /**
     * Validate handling of cyclic references across Patient, Encounter, Questionnaire, and QuestionnaireResponse.
     */
    @Test
    fun testCircularAndSelfReferencingResources() =
        runTest {
            val patient1 =
                Patient
                    .Builder()
                    .apply {
                        id = "patient_cyclic_1"
                        link.add(
                            Patient.Link
                                .Builder(
                                    other =
                                        Reference.Builder().apply {
                                            reference =
                                                FhirString.Builder().apply { value = "Patient/patient_cyclic_2" }
                                        },
                                    type = Enumeration(value = Patient.LinkType.Seealso),
                                ),
                        )
                    }.build()

            val patient2 =
                Patient
                    .Builder()
                    .apply {
                        id = "patient_cyclic_2"
                        link.add(
                            Patient.Link
                                .Builder(
                                    other =
                                        Reference.Builder().apply {
                                            reference =
                                                FhirString.Builder().apply { value = "Patient/patient_cyclic_1" }
                                        },
                                    type = Enumeration(value = Patient.LinkType.Seealso),
                                ),
                        )
                    }.build()

            val questionnaire =
                Questionnaire
                    .Builder(status = Enumeration(value = PublicationStatus.Active))
                    .apply {
                        id = "q_cyclic_1"
                    }.build()

            val encounter =
                createFhirEncounter(
                    id = "enc_cyclic_1",
                    patientId = "patient_cyclic_1",
                    practitionerId = "practitioner_1",
                    dateStr = "2026-09-01T10:00:00Z",
                )

            val qr =
                QuestionnaireResponse
                    .Builder(status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
                    .apply {
                        this.id = "qr_cyclic_1"
                        this.subject =
                            Reference.Builder().apply { reference = FhirString.Builder().apply { value = "Patient/patient_cyclic_1" } }
                        this.encounter =
                            Reference.Builder().apply { reference = FhirString.Builder().apply { value = "Encounter/enc_cyclic_1" } }
                        this.questionnaire = Canonical.Builder().apply { value = "Questionnaire/q_cyclic_1" }
                    }.build()

            fhirRepo.savePatient(patient1)
            fhirRepo.savePatient(patient2)
            fhirRepo.saveQuestionnaire(questionnaire)
            fhirRepo.saveEncounter(encounter)
            fhirRepo.saveQuestionnaireResponse(qr)

            val encryptedExport = service.exportData(password, exportAll = true)
            assertTrue(encryptedExport.isNotEmpty())

            val driverFresh = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driverFresh)
            val dbFresh = ChartCamDatabase(driverFresh)
            val serviceFresh = ExportImportService(dbFresh, fileStorage)

            serviceFresh.importData(encryptedExport, password)

            val fhirRepoFresh = FhirRepository(dbFresh)
            val importedP1 = fhirRepoFresh.getPatient("patient_cyclic_1")
            val importedP2 = fhirRepoFresh.getPatient("patient_cyclic_2")
            val importedEnc = fhirRepoFresh.getEncounter("enc_cyclic_1")
            val allQrs = fhirRepoFresh.getAllQuestionnaireResponses()
            val importedQr = allQrs.firstOrNull { it.id == "qr_cyclic_1" }

            assertNotNull(importedP1)
            assertNotNull(importedP2)
            assertNotNull(importedEnc)
            assertNotNull(importedQr)

            assertEquals(
                "Patient/patient_cyclic_2",
                importedP1.link
                    .first()
                    .other.reference
                    ?.value,
            )
            assertEquals(
                "Patient/patient_cyclic_1",
                importedP2.link
                    .first()
                    .other.reference
                    ?.value,
            )
            assertEquals("patient_cyclic_1", importedEnc.subject?.reference?.value)
            assertEquals("Encounter/enc_cyclic_1", importedQr.encounter?.reference?.value)
        }

    /**
     * Test handling of unknown FHIR attributes or unsupported extension namespaces without crashing.
     */
    @Test
    fun testSchemaVersionDiscrepanciesAndExtensions() =
        runTest {
            val bundleBuilder = Bundle.Builder(Enumeration(value = Bundle.BundleType.Collection))

            val futureExtensionBuilder =
                Extension
                    .Builder(url = "http://future-fhir-spec.org/fhir/extensions/teleportation-telemetry")
                    .apply {
                        value = Extension.Value.String(FhirString.Builder().apply { value = "QuantumChannel_42" }.build())
                    }

            val patientWithUnknownExt =
                Patient
                    .Builder()
                    .apply {
                        id = "patient_future_ext"
                        active = FhirBoolean.Builder().apply { value = true }
                        extension.add(futureExtensionBuilder)
                    }.build()

            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { resource = patientWithUnknownExt.toBuilder() })

            val bundle = bundleBuilder.build()
            val encrypted = cryptoService.encrypt(fhirJson.encodeToString(bundle), password)

            service.importData(encrypted, password)

            val imported = fhirRepo.getPatient("patient_future_ext")
            assertNotNull(imported)
            assertEquals("patient_future_ext", imported.id)
            assertEquals(1, imported.extension.size)
            assertEquals("http://future-fhir-spec.org/fhir/extensions/teleportation-telemetry", imported.extension.first().url)
        }
}
