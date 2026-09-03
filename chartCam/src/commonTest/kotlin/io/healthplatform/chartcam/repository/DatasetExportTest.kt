/**
 * @file DatasetExportTest.kt
 * Contains tests for dataset export serialization (CSV/JSON).
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.String
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test class for verifying dataset export serialization, including CSV mappings.
 */
class DatasetExportTest {
    /**
     * Verifies that FHIR Patients can be correctly mapped and serialized to CSV format.
     */
    @Test
    fun testPatientCsvMapping() {
        val patient1 =
            Patient
                .Builder()
                .apply {
                    id = "pat-123"
                    active = Boolean.Builder().apply { value = true }
                    name.add(
                        HumanName.Builder().apply {
                            family = String.Builder().apply { value = "Smith" }
                            given.add(String.Builder().apply { value = "John" })
                        },
                    )
                }.build()

        val patient2 =
            Patient
                .Builder()
                .apply {
                    id = "pat-456"
                    active = Boolean.Builder().apply { value = false }
                    name.add(
                        HumanName.Builder().apply {
                            family = String.Builder().apply { value = "Doe" }
                            given.add(String.Builder().apply { value = "Jane" })
                        },
                    )
                }.build()

        val csvOutput = CsvExporter.exportPatientsToCsv(listOf(patient1, patient2))

        val expectedHeader = "ID,FamilyName,GivenName,Active"
        assertTrue(csvOutput.contains(expectedHeader), "CSV should contain headers")
        assertTrue(csvOutput.contains("pat-123,Smith,John,true"), "CSV should contain patient 1 data")
        assertTrue(csvOutput.contains("pat-456,Doe,Jane,false"), "CSV should contain patient 2 data")

        val lines = csvOutput.trim().split("\n")
        assertEquals(3, lines.size, "CSV should have exactly 3 lines (1 header + 2 data rows)")
    }
}
