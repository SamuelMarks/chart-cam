/**
 * @file DocumentReferenceModels.kt
 * Pure multiplatform data models and builders for FHIR DocumentReference resources.
 */
package io.healthplatform.chartcam.models

import dev.ohs.fhir.model.r4.Attachment
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.ExtensibleEnumeration
import dev.ohs.fhir.model.r4.Identifier
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.fhir.model.r4.Url
import dev.ohs.fhir.model.r4.terminologies.CommonLanguages
import io.healthplatform.chartcam.terminology.TerminologyService
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Safely parses a BCP-47 language tag or custom dialect into an [ExtensibleEnumeration].
 *
 * @param tag The language code tag (e.g., "en", "es", "he", "zh", or a custom dialect).
 * @return A [Result] enclosing the parsed [ExtensibleEnumeration].
 */
fun parseExtensibleLanguage(tag: String): Result<ExtensibleEnumeration<CommonLanguages>> =
    runCatching {
        val trimmed = tag.trim()
        require(trimmed.isNotEmpty()) { "Language tag cannot be empty" }
        val predefined = runCatching { CommonLanguages.fromCode(trimmed.lowercase()) }.getOrNull()
        if (predefined != null) {
            ExtensibleEnumeration.of(predefined)
        } else {
            ExtensibleEnumeration.of(trimmed)
        }
    }

/**
 * Helper function for document reference content construction using immutable data classes.
 *
 * @param mime The MIME type string of the media artifact.
 * @param urlPath The relative or local storage URL of the media artifact.
 * @param languageTag Optional BCP-47 language tag or custom dialect string.
 * @return A [Result] enclosing the list of [DocumentReference.Content] elements.
 */
fun buildDocumentReferenceContent(
    mime: String,
    urlPath: String,
    languageTag: String? = null,
): Result<List<DocumentReference.Content>> =
    runCatching {
        val lang = languageTag?.let { parseExtensibleLanguage(it).getOrNull() }
        listOf(
            DocumentReference.Content(
                attachment =
                    Attachment(
                        contentType = Code(value = mime),
                        url = Url(value = urlPath),
                        language = lang,
                    ),
            ),
        )
    }

/**
 * Helper function for document reference context construction using immutable data classes.
 *
 * @param encounterId The associated encounter identifier.
 * @param answerCode Optional linkId or clinical answer code relating the document.
 * @return A [Result] enclosing the [DocumentReference.Context] element.
 */
fun buildDocumentReferenceContext(
    encounterId: String,
    answerCode: String? = null,
): Result<DocumentReference.Context> =
    runCatching {
        DocumentReference.Context(
            encounter = listOf(Reference(reference = FhirString(value = encounterId))),
            related =
                if (answerCode != null) {
                    listOf(Reference(identifier = Identifier(value = FhirString(value = answerCode))))
                } else {
                    emptyList()
                },
        )
    }

/**
 * Helper function for constructing textual clinical note contents using immutable data classes.
 *
 * @param notesText The raw clinical text note.
 * @param languageTag Optional language code.
 * @return A [Result] enclosing the list of [DocumentReference.Content] elements.
 */
fun buildClinicalNoteContent(
    notesText: String,
    languageTag: String? = null,
): Result<List<DocumentReference.Content>> =
    runCatching {
        val lang = languageTag?.let { parseExtensibleLanguage(it).getOrNull() }
        listOf(
            DocumentReference.Content(
                attachment =
                    Attachment(
                        contentType = Code(value = "text/plain"),
                        url = Url(value = "data:text/plain;charset=utf-8,$notesText"),
                        language = lang,
                    ),
            ),
        )
    }

/**
 * Helper function for creating the LOINC Consultation Note codeable concept.
 *
 * @return A [Result] enclosing the LOINC consultation note [CodeableConcept].
 */
fun buildClinicalNoteType(): Result<CodeableConcept> =
    runCatching {
        CodeableConcept(
            coding =
                listOf(
                    Coding(
                        system = Uri(value = TerminologyService.LOINC_URI),
                        code = Code(value = "11488-4"),
                        display = FhirString(value = "Consultation note"),
                    ),
                ),
        )
    }
