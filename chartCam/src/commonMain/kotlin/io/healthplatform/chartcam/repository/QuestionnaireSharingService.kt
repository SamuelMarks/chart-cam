/**
 * @file QuestionnaireSharingService.kt
 * Contains declarations for QuestionnaireSharingService.kt.
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.FhirR4Json
import com.google.fhir.model.r4.Questionnaire

/**
 * Service to handle the serialization and deserialization of Questionnaires
 * for decentralized sharing across devices, directly using FHIR R4 JSON.
 */
class QuestionnaireSharingService {
    private val fhirJson = FhirR4Json()

    /**
     * Serializes a [Questionnaire] domain model directly to a FHIR JSON string.
     *
     * @param questionnaire The [Questionnaire] resource to serialize.
     * @return A [Result] enclosing the serialized JSON string or an error.
     */
    fun serializeQuestionnaire(questionnaire: Questionnaire): Result<String> = runCatching { fhirJson.encodeToString(questionnaire) }

    /**
     * Deserializes a FHIR JSON string back into a [Questionnaire] domain model.
     *
     * @param jsonString The JSON string representing a FHIR Questionnaire.
     * @return A [Result] enclosing the deserialized [Questionnaire] resource or an error.
     */
    fun deserializeQuestionnaire(jsonString: String): Result<Questionnaire> =
        runCatching { fhirJson.decodeFromString(jsonString) as Questionnaire }
}
