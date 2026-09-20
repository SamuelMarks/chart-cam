/**
 * @file CsvExporterTest.kt
 * Tests for CsvExporter utility covering line, branch, and functional edge cases.
 */

package io.healthplatform.chartcam.repository

import dev.ohs.fhir.model.r4.Date
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Patient
import kotlin.test.Test
import kotlin.test.assertEquals
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Unit tests verifying [CsvExporter] RFC 4180 escaping and patient list serialization.
 */
class CsvExporterTest {
    /**
     * Verifies field escaping for commas, quotes, newlines, spaces, and plain strings.
     */
    @Test
    fun testEscapeCsvField() {
        val q = "\""
        val dq = "\"\""
        val nl = "\n"
        val cr = "\r"
        assertEquals("simple", CsvExporter.escapeCsvField("simple"))
        assertEquals(q + "hello,world" + q, CsvExporter.escapeCsvField("hello,world"))
        assertEquals(q + "hello" + dq + "world" + q, CsvExporter.escapeCsvField("hello\"world"))
        assertEquals(q + "line1" + nl + "line2" + q, CsvExporter.escapeCsvField("line1" + nl + "line2"))
        assertEquals(q + "line1" + cr + "line2" + q, CsvExporter.escapeCsvField("line1" + cr + "line2"))
        assertEquals(q + " leading" + q, CsvExporter.escapeCsvField(" leading"))
        assertEquals(q + "trailing " + q, CsvExporter.escapeCsvField("trailing "))
        assertEquals("", CsvExporter.escapeCsvField(""))
    }

    /**
     * Verifies export of empty patient list generates only header line.
     */
    @Test
    fun testExportEmptyPatientList() {
        val nl = "\n"
        val csv = CsvExporter.exportPatientsToCsv(emptyList())
        assertEquals(CsvExporter.PATIENT_CSV_HEADER + nl, csv)
    }

    /**
     * Verifies patient export with complete demographic details.
     */
    @Test
    fun testExportCompletePatient() {
        val q = "\""
        val nl = "\n"
        val patient =
            Patient(
                id = "p-101",
                active = FhirBoolean(value = true),
                name =
                    listOf(
                        HumanName(
                            family = FhirString(value = "O'Connor, Jr."),
                            given = listOf(FhirString(value = "Alice"), FhirString(value = "Marie")),
                        ),
                    ),
                birthDate = Date(value = FhirDate.fromString("1992-05-12")),
            )

        val csv = CsvExporter.exportPatientsToCsv(listOf(patient))
        val lines = csv.trim().split(nl)
        assertEquals(2, lines.size)
        assertEquals(CsvExporter.PATIENT_CSV_HEADER, lines[0])
        val expectedSecondLine = "p-101," + q + "O'Connor, Jr." + q + ",Alice Marie,true,1992-05-12"
        assertEquals(expectedSecondLine, lines[1])
    }

    /**
     * Verifies patient export with empty/null fields to ensure graceful fallback.
     */
    @Test
    fun testExportSparsePatient() {
        val nl = "\n"
        val patient =
            Patient(
                id = null,
                active = null,
                name = emptyList(),
                birthDate = null,
            )

        val csv = CsvExporter.exportPatientsToCsv(listOf(patient))
        val lines = csv.trim().split(nl)
        assertEquals(2, lines.size)
        assertEquals(CsvExporter.PATIENT_CSV_HEADER, lines[0])
        assertEquals(",,,false,", lines[1])
    }

    /**
     * Verifies patient export when name given list has null values or single value.
     */
    @Test
    fun testExportPatientWithSingleNameAndNullGivens() {
        val q = "\""
        val nl = "\n"
        val patient =
            Patient(
                id = "p-202",
                active = FhirBoolean(value = false),
                name =
                    listOf(
                        HumanName(
                            family = null,
                            given = listOf(FhirString(value = "Bob"), FhirString(value = null)),
                        ),
                    ),
                birthDate = Date(value = FhirDate.fromString("2000-01-01")),
            )

        val csv = CsvExporter.exportPatientsToCsv(listOf(patient))
        val lines = csv.trim().split(nl)
        assertEquals(2, lines.size)
        assertEquals("p-202,," + q + "Bob " + q + ",false,2000-01-01", lines[1])
    }

    /**
     * Verifies patient export when family has null value, active has null value, and birthDate has null value.
     */
    @Test
    fun testExportPatientWithExplicitNullValuesInObjects() {
        val nl = "\n"
        val patient =
            Patient(
                id = "p-303",
                active = FhirBoolean(value = null),
                name =
                    listOf(
                        HumanName(
                            family = FhirString(value = null),
                            given = emptyList(),
                        ),
                    ),
                birthDate = Date(value = null),
            )

        val csv = CsvExporter.exportPatientsToCsv(listOf(patient))
        val lines = csv.trim().split(nl)
        assertEquals(2, lines.size)
        assertEquals(CsvExporter.PATIENT_CSV_HEADER, lines[0])
        assertEquals("p-303,,,false,", lines[1])
    }
}
