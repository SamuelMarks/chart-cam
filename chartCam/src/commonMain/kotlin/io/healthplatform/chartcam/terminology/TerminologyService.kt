/**
 * @file TerminologyService.kt
 * Contains declarations for TerminologyService.kt.
 */
package io.healthplatform.chartcam.terminology

import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.CodeSystem
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Service that provides standard clinical terminology resources (e.g. LOINC, SNOMED)
 * required for standardizing FHIR Observations and Questionnaires.
 */
object TerminologyService {
    /** The canonical URI for the LOINC code system. */
    const val LOINC_URI: String = "http://loinc.org"

    /** The canonical URI for the SNOMED CT code system. */
    const val SNOMED_URI: String = "http://snomed.info/sct"

    /**
     * Retrieves the basic [CodeSystem] resource definition for LOINC.
     *
     * @return A constructed FHIR [CodeSystem] representing LOINC.
     */
    fun getLoincCodeSystem(): CodeSystem =
        CodeSystem(
            status = Enumeration(value = PublicationStatus.Active),
            content = Enumeration(value = CodeSystem.CodeSystemContentMode.Complete),
            url = Uri(value = LOINC_URI),
            name = FhirString(value = "LOINC"),
            title = FhirString(value = "Logical Observation Identifiers Names and Codes"),
        )

    /**
     * Retrieves the basic [CodeSystem] resource definition for SNOMED CT.
     *
     * @return A constructed FHIR [CodeSystem] representing SNOMED CT.
     */
    fun getSnomedCodeSystem(): CodeSystem =
        CodeSystem(
            status = Enumeration(value = PublicationStatus.Active),
            content = Enumeration(value = CodeSystem.CodeSystemContentMode.Complete),
            url = Uri(value = SNOMED_URI),
            name = FhirString(value = "SNOMED CT"),
            title = FhirString(value = "Systematized Nomenclature of Medicine Clinical Terms"),
        )

    /**
     * Constructs a LOINC [Coding] element for use in FHIR resources.
     *
     * @param codeVal The specific LOINC code string (e.g., "85353-1").
     * @param displayVal An optional human-readable display string for the code.
     * @return A FHIR [Coding] object populated with the LOINC system and provided code.
     */
    fun getLoincCoding(
        codeVal: String,
        displayVal: String? = null,
    ): Coding =
        Coding(
            system = Uri(value = LOINC_URI),
            code = Code(value = codeVal),
            display = displayVal?.let { FhirString(value = it) },
        )

    /**
     * Helper factory to construct standard clinical Coding elements.
     *
     * @param system Canonical terminology system URI.
     * @param code The terminology code.
     * @param display Optional human-readable display string.
     * @return A constructed FHIR [Coding] object.
     */
    fun createCoding(
        system: String,
        code: String,
        display: String? = null,
    ): Coding =
        Coding(
            system = Uri(value = system),
            code = Code(value = code),
            display = display?.let { FhirString(value = it) },
        )
}
