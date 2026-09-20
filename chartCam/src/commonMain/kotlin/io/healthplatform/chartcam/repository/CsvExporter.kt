/**
 * @file CsvExporter.kt
 * Contains CSV export functionality.
 */
package io.healthplatform.chartcam.repository

import dev.ohs.fhir.model.r4.Patient

/**
 * Utility for exporting resources to CSV format.
 */
object CsvExporter {
    /**
     * Standard CSV header for FHIR Patient exports.
     */
    const val PATIENT_CSV_HEADER = "ID,FamilyName,GivenName,Active,BirthDate"

    /**
     * Escapes a single CSV field value according to RFC 4180.
     * If the value contains commas, double quotes, CRLF newlines, or leading/trailing whitespace,
     * it is enclosed in double quotes with internal quotes doubled.
     *
     * @param value The raw string value.
     * @return The properly escaped CSV field string.
     */
    fun escapeCsvField(value: String): String {
        val needsQuotes =
            value.contains(',') ||
                value.contains('"') ||
                value.contains('\n') ||
                value.contains('\r') ||
                value.startsWith(' ') ||
                value.endsWith(' ')

        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Converts a list of FHIR Patients to a CSV string.
     *
     * @param patients The list of patients to export.
     * @return A CSV formatted string.
     */
    fun exportPatientsToCsv(patients: List<Patient>): String {
        val builder = StringBuilder()
        builder.append("$PATIENT_CSV_HEADER\n")
        for (patient in patients) {
            val id = escapeCsvField(patient.id ?: "")
            val firstName = patient.name.firstOrNull()
            val familyVal = firstName?.family?.value ?: ""
            val family = escapeCsvField(familyVal)

            val givenItems = firstName?.given
            val givenVal =
                if (givenItems != null) {
                    givenItems.joinToString(" ") { it.value ?: "" }
                } else {
                    ""
                }
            val given = escapeCsvField(givenVal)

            val active = patient.active?.value ?: false

            // Format BirthDate in ISO 8601 (YYYY-MM-DD for FhirDate)
            val fhirDate = patient.birthDate?.value
            val birthDateVal = if (fhirDate != null) fhirDate.toString() else ""
            val birthDate = escapeCsvField(birthDateVal)

            builder.append("$id,$family,$given,$active,$birthDate\n")
        }
        return builder.toString()
    }
}
