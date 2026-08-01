/**
 * @file DocumentReferenceModels.kt
 * Contains declarations for DocumentReferenceModels.kt.
 */
package io.healthplatform.chartcam.models

import com.google.fhir.model.r4.Attachment
import com.google.fhir.model.r4.Code
import com.google.fhir.model.r4.Coding
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Identifier
import com.google.fhir.model.r4.Reference
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.Uri
import com.google.fhir.model.r4.Url
import io.healthplatform.chartcam.terminology.TerminologyService

/**
 * Helper function for document reference construction.
 */
internal fun buildDocumentReferenceContent(
    mime: kotlin.String,
    urlPath: kotlin.String,
): MutableList<DocumentReference.Content.Builder> =
    mutableListOf(
        DocumentReference.Content.Builder(
            attachment =
                Attachment.Builder().apply {
                    contentType = Code.Builder().apply { value = mime }
                    url = Url.Builder().apply { value = urlPath }
                },
        ),
    )

/**
 * Helper function for document reference construction.
 */
internal fun buildDocumentReferenceContext(
    encounterId: kotlin.String,
    answerCode: kotlin.String?,
): DocumentReference.Context.Builder =
    DocumentReference.Context.Builder().apply {
        encounter.add(
            Reference.Builder().apply {
                reference = String.Builder().apply { value = encounterId }
            },
        )
        if (answerCode != null) {
            related.add(
                Reference.Builder().apply {
                    identifier =
                        Identifier.Builder().apply {
                            value = String.Builder().apply { value = answerCode }
                        }
                },
            )
        }
    }

/**
 * Helper function for document reference construction.
 */
internal fun buildClinicalNoteContent(notesText: kotlin.String): MutableList<DocumentReference.Content.Builder> =
    mutableListOf(
        DocumentReference.Content.Builder(
            attachment =
                Attachment.Builder().apply {
                    contentType = Code.Builder().apply { value = "text/plain" }
                    url =
                        Url.Builder().apply {
                            value = "data:text/plain;charset=utf-8,$notesText"
                        }
                },
        ),
    )

/**
 * Helper function for document reference construction.
 */
internal fun buildClinicalNoteType(): com.google.fhir.model.r4.CodeableConcept.Builder =
    com.google.fhir.model.r4.CodeableConcept.Builder().apply {
        coding.add(
            Coding.Builder().apply {
                system = Uri.Builder().apply { value = TerminologyService.LOINC_URI }
                code = Code.Builder().apply { value = "11488-4" }
                display = String.Builder().apply { value = "Consultation note" }
            },
        )
    }
