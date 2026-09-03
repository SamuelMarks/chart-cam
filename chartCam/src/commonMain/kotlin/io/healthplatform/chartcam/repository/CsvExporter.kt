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
     * Converts a list of FHIR Patients to a CSV string.
     *
     * @param patients The list of patients to export.
     * @return A CSV formatted string.
     */
    fun exportPatientsToCsv(patients: List<Patient>): String {
        val builder = StringBuilder()
        builder.append("ID,FamilyName,GivenName,Active\n")
        for (patient in patients) {
            val id = patient.id
            val family =
                patient.name
                    .firstOrNull()
                    ?.family
                    ?.value ?: ""
            val given =
                patient.name
                    .firstOrNull()
                    ?.given
                    ?.joinToString(" ") { it.value ?: "" } ?: ""
            val active = patient.active?.value ?: false
            builder.append("$id,$family,$given,$active\n")
        }
        return builder.toString()
    }
}
