package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.DocumentReferenceCreationParams
import io.healthplatform.chartcam.models.createFhirDocumentReference
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.utils.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for reproducing and verifying fixes for crashes during data export.
 */
class ExportImportServiceCrashTest {
    /**
     * Verifies that the export does not crash when a binary file is missing or reading it throws an exception.
     */
    @Test
    fun testExportWithMissingImageDoesNotCrash() {
        runBlocking {
            val driver = DatabaseDriverFactory().createDriver()
            val db = ChartCamDatabase(driver)
            val fhirRepo = FhirRepository(db)

            val failingStorage =
                object : FileStorage {
                    override fun saveImage(
                        fileName: String,
                        bytes: ByteArray,
                    ): String = ""

                    override fun readImage(path: String): ByteArray = throw IOException("Simulated crash on missing/corrupted file")

                    override fun clearCache() {}
                }

            val service = ExportImportService(db, failingStorage)

            val pId = UUID.randomUUID()
            val encId = UUID.randomUUID()

            val patient = createFhirPatient(pId, "John", "Doe", LocalDate(1990, 1, 1), "MRN123")
            fhirRepo.savePatient(patient)

            val docRef =
                createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = UUID.randomUUID(),
                        patientId = pId,
                        encounterId = encId,
                        dateStr = "2023-01-01T12:05:00Z",
                        desc = "Image",
                        mime = "image/jpeg",
                        urlPath = "file://missing.jpg",
                    ),
                )
            fhirRepo.saveDocumentReference(docRef)

            // This will crash if we don't catch Exception in ExportImportService
            val exportedString = service.exportData("password", true, null)
            assertTrue(exportedString.isNotEmpty(), "Export should succeed and return data despite the file reading error")
        }
    }
}
