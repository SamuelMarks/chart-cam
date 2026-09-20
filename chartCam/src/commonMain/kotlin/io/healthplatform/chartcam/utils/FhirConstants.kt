/**
 * @file FhirConstants.kt
 * Contains declarations for FhirConstants.kt.
 *
 * Contains constants related to the Fast Healthcare Interoperability Resources (FHIR) standard.
 */
package io.healthplatform.chartcam.utils

/**
 * An object holding constant values for FHIR operations.
 */
object FhirConstants {
    /**
     * The MIME type representing the standard FHIR JSON format (`application/fhir+json`).
     */
    const val CONTENT_TYPE_FHIR_JSON = "application/fhir+json"

    /**
     * Returns whether the given content type string matches the standard FHIR JSON format.
     *
     * @param contentType The content type string to evaluate.
     * @return True if the content type equals `application/fhir+json`, false otherwise.
     */
    fun isFhirJsonContentType(contentType: String): Boolean = contentType == CONTENT_TYPE_FHIR_JSON
}
