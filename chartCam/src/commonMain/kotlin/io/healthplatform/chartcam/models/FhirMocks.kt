/**
 * @file FhirMocks.kt
 * Contains clinical demo and preview fixture builders for FHIR R4 Questionnaire and QuestionnaireResponse resources.
 */
package io.healthplatform.chartcam.models

import dev.ohs.fhir.model.r4.Canonical
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import dev.ohs.fhir.model.r4.terminologies.QuestionnaireItemType
import dev.ohs.fhir.model.r4.terminologies.QuestionnaireResponseStatus
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Strongly-typed factory methods for constructing baseline FHIR [Questionnaire] and
 * [QuestionnaireResponse] fixtures for Compose Previews and clinical workflow verification.
 */
object FhirMocks {
    /**
     * Creates a baseline FHIR [Questionnaire] resource with configurable item metadata.
     *
     * @param idStr The logical ID of the Questionnaire.
     * @param titleStr The display title of the Questionnaire.
     * @param itemLinkId The linkId of the primary question item.
     * @param itemText The prompt text of the primary question item.
     * @param itemType The FHIR item type for the primary question item.
     * @return A constructed FHIR [Questionnaire] resource.
     */
    fun createMockQuestionnaire(
        idStr: String = "mock-questionnaire-1",
        titleStr: String = "Mock Questionnaire",
        itemLinkId: String = "mock-item-1",
        itemText: String = "Mock Question",
        itemType: QuestionnaireItemType = QuestionnaireItemType.String,
    ): Questionnaire =
        Questionnaire(
            id = idStr,
            status = Enumeration(value = PublicationStatus.Active),
            title = FhirString(value = titleStr),
            item =
                listOf(
                    Questionnaire.Item(
                        linkId = FhirString(value = itemLinkId),
                        type = Enumeration(value = itemType),
                        text = FhirString(value = itemText),
                    ),
                ),
        )

    /**
     * Creates a baseline FHIR [QuestionnaireResponse] resource with configurable item and answer values.
     *
     * @param idStr The logical ID of the QuestionnaireResponse.
     * @param questionnaireUrl The canonical URL of the source Questionnaire.
     * @param itemLinkId The linkId of the answered question item.
     * @param itemText The prompt text of the answered question item.
     * @param answerText The string answer value recorded for the question item.
     * @return A constructed FHIR [QuestionnaireResponse] resource.
     */
    fun createMockQuestionnaireResponse(
        idStr: String = "mock-response-1",
        questionnaireUrl: String = "Questionnaire/mock-questionnaire-1",
        itemLinkId: String = "mock-item-1",
        itemText: String = "Mock Question",
        answerText: String = "Mock Answer",
    ): QuestionnaireResponse =
        QuestionnaireResponse(
            id = idStr,
            status = Enumeration(value = QuestionnaireResponseStatus.Completed),
            questionnaire = Canonical(value = questionnaireUrl),
            item =
                listOf(
                    QuestionnaireResponse.Item(
                        linkId = FhirString(value = itemLinkId),
                        text = FhirString(value = itemText),
                        answer =
                            listOf(
                                QuestionnaireResponse.Item.Answer(
                                    value =
                                        QuestionnaireResponse.Item.Answer.Value
                                            .String(FhirString(value = answerText)),
                                ),
                            ),
                    ),
                ),
        )
}
