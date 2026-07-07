package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.FhirR4Json
import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.models.QuestionnaireExportDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Service to handle the serialization and deserialization of Questionnaires
 * for decentralized sharing across devices.
 */
class QuestionnaireSharingService {
    private val fhirJson = FhirR4Json()
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    /**
     * Serializes a [Questionnaire] domain model into the standard JSON schema for export.
     *
     * @param questionnaire The [Questionnaire] resource to serialize.
     * @return The serialized JSON string.
     */
    fun serializeQuestionnaire(questionnaire: Questionnaire): String {
        val serializedFhir = fhirJson.encodeToString(questionnaire)
        val dto = QuestionnaireExportDto(fhirJson = serializedFhir)
        return json.encodeToString(dto)
    }

    /**
     * Deserializes a JSON string back into a [Questionnaire] domain model.
     * Validates the schema version and handles parsing the embedded FHIR JSON.
     *
     * @param jsonString The JSON string representing a [QuestionnaireExportDto].
     * @return The deserialized [Questionnaire] resource.
     * @throws IllegalArgumentException If the format is invalid or the version is unsupported.
     */
    fun deserializeQuestionnaire(jsonString: String): Questionnaire {
        val dto =
            try {
                json.decodeFromString<QuestionnaireExportDto>(jsonString)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid format: Unable to parse Questionnaire JSON.", e)
            }

        if (dto.app != "ChartCam") {
            throw IllegalArgumentException("Unsupported application: ${dto.app}")
        }

        if (dto.version > 1) {
            throw IllegalArgumentException("Unsupported schema version: ${dto.version}. Please update the app.")
        }

        return try {
            fhirJson.decodeFromString(dto.fhirJson) as Questionnaire
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid format: Embedded FHIR JSON is not a valid Questionnaire.", e)
        }
    }
}
