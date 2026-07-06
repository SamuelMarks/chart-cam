/**
 * Contains constants related to the Fast Healthcare Interoperability Resources (FHIR) standard.
 */
package io.healthplatform.chartcam.utils

import io.ktor.http.ContentType

/**
 * An object holding constant values for FHIR operations and content negotiation.
 */
object FhirConstants {
    /**
     * The Ktor `ContentType` representing the standard FHIR JSON format (`application/fhir+json`).
     */
    val ContentTypeFhirJson = ContentType("application", "fhir+json")
}
