/**
 * @file CsvExporter.kt
 * Contains CSV export functionality.
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Patient

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
            val family =
                escapeCsvField(
                    patient.name
                        .firstOrNull()
                        ?.family
                        ?.value ?: "",
                )
            val given =
                escapeCsvField(
                    patient.name
                        .firstOrNull()
                        ?.given
                        ?.joinToString(" ") { it.value ?: "" } ?: "",
                )
            val active = patient.active?.value ?: false

            // Format BirthDate in ISO 8601 (YYYY-MM-DD for FhirDate)
            val birthDate = escapeCsvField(patient.birthDate?.value?.toString() ?: "")

            builder.append("$id,$family,$given,$active,$birthDate\n")
        }
        return builder.toString()
    }
}
