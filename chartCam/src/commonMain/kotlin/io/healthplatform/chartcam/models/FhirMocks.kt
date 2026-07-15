/**
 * @file FhirMocks.kt
 * Contains declarations for FhirMocks.kt.
 */
package io.healthplatform.chartcam.models

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.terminologies.PublicationStatus

/**
 * Robust mock factories for FHIR resources to simplify Compose Previews and UI testing.
 */
object FhirMocks {
    /**
     * Creates a mock FHIR Questionnaire resource for testing.
     *
     * @param idStr The string representation of the Questionnaire's logical ID.
     * @param titleStr The title of the Questionnaire.
     * @return A constructed FHIR [Questionnaire] object.
     */
    fun createMockQuestionnaire(
        idStr: kotlin.String = "mock-questionnaire-1",
        titleStr: kotlin.String = "Mock Questionnaire",
    ): Questionnaire =
        Questionnaire
            .Builder(status = Enumeration(value = PublicationStatus.Active))
            .apply {
                id = idStr
                title = String.Builder().apply { value = titleStr }
                item.add(
                    Questionnaire.Item
                        .Builder(
                            linkId = String.Builder().apply { value = "mock-item-1" },
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ).apply {
                            text = String.Builder().apply { value = "Mock Question" }
                        },
                )
            }.build()

    /**
     * Creates a mock FHIR QuestionnaireResponse resource for testing.
     *
     * @param idStr The string representation of the QuestionnaireResponse's logical ID.
     * @param questionnaireUrl The canonical URL of the Questionnaire this response is based on.
     * @return A constructed FHIR [QuestionnaireResponse] object.
     */
    fun createMockQuestionnaireResponse(
        idStr: kotlin.String = "mock-response-1",
        questionnaireUrl: kotlin.String = "Questionnaire/mock-questionnaire-1",
    ): QuestionnaireResponse =
        QuestionnaireResponse
            .Builder(status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
            .apply {
                id = idStr
                questionnaire =
                    com.google.fhir.model.r4.Canonical
                        .Builder()
                        .apply { value = questionnaireUrl }
                item.add(
                    QuestionnaireResponse.Item
                        .Builder(
                            linkId = String.Builder().apply { value = "mock-item-1" },
                        ).apply {
                            text = String.Builder().apply { value = "Mock Question" }
                            answer.add(
                                QuestionnaireResponse.Item.Answer.Builder().apply {
                                    value =
                                        QuestionnaireResponse.Item.Answer.Value
                                            .String(String.Builder().apply { value = "Mock Answer" }.build())
                                },
                            )
                        },
                )
            }.build()
}
