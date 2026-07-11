package io.healthplatform.chartcam.terminology

import com.google.fhir.model.r4.Code
import com.google.fhir.model.r4.CodeSystem
import com.google.fhir.model.r4.Coding
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.Uri
import com.google.fhir.model.r4.terminologies.PublicationStatus

object TerminologyService {
    val loincUri: kotlin.String = "http://loinc.org"
    val snomedUri: kotlin.String = "http://snomed.info/sct"

    fun getLoincCodeSystem(): CodeSystem =
        CodeSystem
            .Builder(
                status = Enumeration(value = PublicationStatus.Active),
                content = Enumeration(value = CodeSystem.CodeSystemContentMode.Complete),
            ).apply {
                url = Uri.Builder().apply { value = loincUri }
                name = String.Builder().apply { value = "LOINC" }
                title = String.Builder().apply { value = "Logical Observation Identifiers Names and Codes" }
            }.build()

    fun getSnomedCodeSystem(): CodeSystem =
        CodeSystem
            .Builder(
                status = Enumeration(value = PublicationStatus.Active),
                content = Enumeration(value = CodeSystem.CodeSystemContentMode.Complete),
            ).apply {
                url = Uri.Builder().apply { value = snomedUri }
                name = String.Builder().apply { value = "SNOMED CT" }
                title = String.Builder().apply { value = "Systematized Nomenclature of Medicine Clinical Terms" }
            }.build()

    fun getLoincCoding(
        codeVal: kotlin.String,
        displayVal: kotlin.String? = null,
    ): Coding =
        Coding
            .Builder()
            .apply {
                system = Uri.Builder().apply { value = loincUri }
                code = Code.Builder().apply { value = codeVal }
                if (displayVal != null) {
                    display = String.Builder().apply { value = displayVal }
                }
            }.build()
}
