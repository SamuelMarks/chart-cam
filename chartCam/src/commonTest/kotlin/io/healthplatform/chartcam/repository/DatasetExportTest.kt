/**
 * @file DatasetExportTest.kt
 * Contains tests for dataset export serialization (CSV/JSON).
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Date
import com.google.fhir.model.r4.FhirDate
import com.google.fhir.model.r4.FhirR4Json
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.String
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Test class for verifying dataset export serialization, including CSV mappings.
 */
class DatasetExportTest {
    /**
     * Verifies that FHIR Patients can be correctly mapped and serialized to CSV format,
     * including ISO 8601 date formatting.
     */
    @Test
    fun testPatientCsvMappingWithIso8601Dates() {
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
                    birthDate = Date.Builder().apply { value = FhirDate.fromString("1990-05-15") }
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
                    birthDate = Date.Builder().apply { value = FhirDate.fromString("1985-12-10") }
                }.build()

        val csvOutput = CsvExporter.exportPatientsToCsv(listOf(patient1, patient2))

        val expectedHeader = "ID,FamilyName,GivenName,Active,BirthDate"
        assertTrue(csvOutput.contains(expectedHeader), "CSV should contain headers including BirthDate")
        assertTrue(csvOutput.contains("pat-123,Smith,John,true,1990-05-15"), "CSV should contain patient 1 data with ISO 8601 date")
        assertTrue(csvOutput.contains("pat-456,Doe,Jane,false,1985-12-10"), "CSV should contain patient 2 data with ISO 8601 date")

        val lines = csvOutput.trim().split("\n")
        assertEquals(3, lines.size, "CSV should have exactly 3 lines (1 header + 2 data rows)")
    }

    /**
     * Tests JSON Export accuracy. We verify that the export engine produces a valid string that contains
     * the requested resources. We mock the behavior since full integration is done in ExportImportServiceJvmTest.
     */
    @Test
    fun testPatientJsonExportAccuracy() {
        val fhirJson = FhirR4Json()

        val patient =
            Patient
                .Builder()
                .apply {
                    id = "test-json-id"
                    birthDate = Date.Builder().apply { value = FhirDate.fromString("2020-01-01") }
                }.build()

        val jsonOutput = fhirJson.encodeToString(patient)
        println(jsonOutput)
        assertTrue(
            jsonOutput.contains("\"resourceType\":\"Patient\"") || jsonOutput.contains("\"resourceType\": \"Patient\""),
            "JSON should identify as Patient",
        )
        assertTrue(
            jsonOutput.contains("\"id\":\"test-json-id\"") || jsonOutput.contains("\"id\": \"test-json-id\""),
            "JSON should contain the accurate ID",
        )
        assertTrue(
            jsonOutput.contains("\"birthDate\":\"2020-01-01\"") || jsonOutput.contains("\"birthDate\": \"2020-01-01\""),
            "JSON should contain accurately formatted ISO 8601 dates",
        )
    }

    /**
     * Test export generation under heavy data loads to ensure it processes correctly.
     * We simulate 1000 records.
     */
    @Test
    fun testHeavyDataLoadExport() =
        runTest {
            val patients = mutableListOf<Patient>()
            for (i in 1..1000) {
                val patient =
                    Patient
                        .Builder()
                        .apply {
                            id = "pat-$i"
                            name.add(
                                HumanName.Builder().apply {
                                    family = String.Builder().apply { value = "Family-$i" }
                                    given.add(String.Builder().apply { value = "Given-$i" })
                                },
                            )
                            birthDate = Date.Builder().apply { value = FhirDate.fromString("1990-01-01") }
                        }.build()
                patients.add(patient)
            }

            val startTime = TimeSource.Monotonic.markNow()
            val csvOutput = CsvExporter.exportPatientsToCsv(patients)
            val duration = startTime.elapsedNow()

            val lines = csvOutput.trim().split("\n")
            assertEquals(1001, lines.size, "CSV should contain 1001 lines (header + 1000 rows)")
            assertTrue(
                duration.inWholeMilliseconds < 5000,
                "Heavy data load export should complete reasonably fast, took ${duration.inWholeMilliseconds} ms",
            )
        }
}
