/**
 * @file SelectiveImportEntityFilterWorkflowTest.kt
 * End-to-end workflow test verifying granular category filtering during archive ingestion.
 */
package io.healthplatform.chartcam.workflow

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.ImportCategory
import io.healthplatform.chartcam.models.ImportFilterOptions
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.models.createFhirPractitioner
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
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
 * Validates selective ingestion filters allowing clinicians to ingest specific categories.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectiveImportEntityFilterWorkflowTest {
    /**
     * In-memory mock storage.
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
    private lateinit var driverSource: JdbcSqliteDriver
    private lateinit var driverTarget: JdbcSqliteDriver
    private lateinit var repoSource: FhirRepository
    private lateinit var repoTarget: FhirRepository
    private lateinit var exportImportSource: ExportImportService
    private lateinit var exportImportTarget: ExportImportService

    /**
     * Initializes database instances.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        driverSource = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driverSource)
        val dbSource = ChartCamDatabase(driverSource)
        repoSource = FhirRepository(dbSource)
        exportImportSource = ExportImportService(dbSource, DummyFileStorage())

        driverTarget = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driverTarget)
        val dbTarget = ChartCamDatabase(driverTarget)
        repoTarget = FhirRepository(dbTarget)
        exportImportTarget = ExportImportService(dbTarget, DummyFileStorage())
    }

    /**
     * Cleans up drivers.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driverSource.close()
        driverTarget.close()
    }

    /**
     * Verifies that when only PATIENTS category is enabled, external practitioners and encounters are excluded.
     */
    @Test
    fun testSelectivePatientsOnlyImport() =
        runTest(testDispatcher) {
            val prac = createFhirPractitioner("foreign-dr", "Strange", "Stephen", true)
            repoSource.savePractitioner(prac)

            val patient = createFhirPatient("foreign-pat-1", "Peter", "Parker", LocalDate(2001, 8, 10), "MRN-SPIDER")
            repoSource.savePatient(patient)

            val enc = createFhirEncounter("foreign-enc-1", "foreign-pat-1", "foreign-dr", "2026-09-14T10:00:00Z")
            repoSource.saveEncounter(enc)

            val qRepo = QuestionnaireRepository()
            qRepo.loadDefaultForms()
            val customForm = qRepo.createQuestionnaire("Spider Form", 1, "Spider Bite")
            repoSource.saveQuestionnaire(customForm)

            val pass = "Marvel2026!"
            val archive = exportImportSource.exportData(pass, exportAll = true).getOrThrow()

            // Target imports with ONLY Patients enabled
            val filter = ImportFilterOptions(enabledCategories = setOf(ImportCategory.PATIENTS))
            exportImportTarget
                .importDataSelective(
                    encryptedData = archive,
                    password = pass,
                    filterOptions = filter,
                ).getOrThrow()

            // Assert: Patient is imported
            assertNotNull(repoTarget.getPatient("foreign-pat-1"))

            // Assert: Practitioner is NOT imported
            assertNull(repoTarget.getPractitioner("foreign-dr"))

            // Assert: Encounter is NOT imported
            assertNull(repoTarget.getEncounter("foreign-enc-1"))
            assertEquals(0, repoTarget.getEncountersForPatient("foreign-pat-1").size)

            // Assert: Questionnaire is NOT imported
            assertNull(repoTarget.getQuestionnaire(customForm.id ?: ""))
        }
}
