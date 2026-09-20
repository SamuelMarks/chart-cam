/**
 * @file FhirMocks.kt
 * Contains declarations for FhirMocks.kt.
 */
package io.healthplatform.chartcam.models

import dev.ohs.fhir.model.r4.Canonical
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import dev.ohs.fhir.model.r4.String as FhirString

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
        idStr: String = "mock-questionnaire-1",
        titleStr: String = "Mock Questionnaire",
    ): Questionnaire =
        Questionnaire(
            id = idStr,
            status = Enumeration(value = PublicationStatus.Active),
            title = FhirString(value = titleStr),
            item =
                listOf(
                    Questionnaire.Item(
                        linkId = FhirString(value = "mock-item-1"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        text = FhirString(value = "Mock Question"),
                    ),
                ),
        )

    /**
     * Creates a mock FHIR QuestionnaireResponse resource for testing.
     *
     * @param idStr The string representation of the QuestionnaireResponse's logical ID.
     * @param questionnaireUrl The canonical URL of the Questionnaire this response is based on.
     * @return A constructed FHIR [QuestionnaireResponse] object.
     */
    fun createMockQuestionnaireResponse(
        idStr: String = "mock-response-1",
        questionnaireUrl: String = "Questionnaire/mock-questionnaire-1",
    ): QuestionnaireResponse =
        QuestionnaireResponse(
            id = idStr,
            status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
            questionnaire = Canonical(value = questionnaireUrl),
            item =
                listOf(
                    QuestionnaireResponse.Item(
                        linkId = FhirString(value = "mock-item-1"),
                        text = FhirString(value = "Mock Question"),
                        answer =
                            listOf(
                                QuestionnaireResponse.Item.Answer(
                                    value =
                                        QuestionnaireResponse.Item.Answer.Value
                                            .String(FhirString(value = "Mock Answer")),
                                ),
                            ),
                    ),
                ),
        )
}
