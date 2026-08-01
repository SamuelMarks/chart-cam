/**
 * @file TerminologyService.kt
 * Contains declarations for TerminologyService.kt.
 */
package io.healthplatform.chartcam.terminology

import com.google.fhir.model.r4.Code
import com.google.fhir.model.r4.CodeSystem
import com.google.fhir.model.r4.Coding
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.Uri
import com.google.fhir.model.r4.terminologies.PublicationStatus

/**
 * Service that provides standard clinical terminology resources (e.g. LOINC, SNOMED)
 * required for standardizing FHIR Observations and Questionnaires.
 */
object TerminologyService {
    /** The canonical URI for the LOINC code system. */
    const val LOINC_URI: kotlin.String = "http://loinc.org"

    /** The canonical URI for the SNOMED CT code system. */
    const val SNOMED_URI: kotlin.String = "http://snomed.info/sct"

    /**
     * Retrieves the basic [CodeSystem] resource definition for LOINC.
     *
     * @return A constructed FHIR [CodeSystem] representing LOINC.
     */
    fun getLoincCodeSystem(): CodeSystem =
        CodeSystem
            .Builder(
                status = Enumeration(value = PublicationStatus.Active),
                content = Enumeration(value = CodeSystem.CodeSystemContentMode.Complete),
            ).apply {
                url = Uri.Builder().apply { value = LOINC_URI }
                name = String.Builder().apply { value = "LOINC" }
                title = String.Builder().apply { value = "Logical Observation Identifiers Names and Codes" }
            }.build()

    /**
     * Retrieves the basic [CodeSystem] resource definition for SNOMED CT.
     *
     * @return A constructed FHIR [CodeSystem] representing SNOMED CT.
     */
    fun getSnomedCodeSystem(): CodeSystem =
        CodeSystem
            .Builder(
                status = Enumeration(value = PublicationStatus.Active),
                content = Enumeration(value = CodeSystem.CodeSystemContentMode.Complete),
            ).apply {
                url = Uri.Builder().apply { value = SNOMED_URI }
                name = String.Builder().apply { value = "SNOMED CT" }
                title = String.Builder().apply { value = "Systematized Nomenclature of Medicine Clinical Terms" }
            }.build()

    /**
     * Constructs a LOINC [Coding] element for use in FHIR resources.
     *
     * @param codeVal The specific LOINC code string (e.g., "85353-1").
     * @param displayVal An optional human-readable display string for the code.
     * @return A FHIR [Coding] object populated with the LOINC system and provided code.
     */
    fun getLoincCoding(
        codeVal: kotlin.String,
        displayVal: kotlin.String? = null,
    ): Coding =
        Coding
            .Builder()
            .apply {
                system = Uri.Builder().apply { value = LOINC_URI }
                code = Code.Builder().apply { value = codeVal }
                if (displayVal != null) {
                    display = String.Builder().apply { value = displayVal }
                }
            }.build()
}
