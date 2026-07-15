package io.healthplatform.chartcam.fhir

import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.String
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionnaireResponseGeneratorJvmTest {
    @Test
    fun `test unanswered required questions and deeply nested groups`() {
        val nestedItem =
            Questionnaire.Item
                .Builder(
                    linkId = String.Builder().apply { value = "nested_1" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    required = Boolean.Builder().apply { value = true }
                }.build()

        val groupItem =
            Questionnaire.Item
                .Builder(
                    linkId = String.Builder().apply { value = "group_1" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).apply {
                    item.add(nestedItem.toBuilder())
                }.build()

        val questionnaire =
            Questionnaire
                .Builder(
                    status = Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active),
                ).apply {
                    id = "test-q"
                    item.add(groupItem.toBuilder())
                }.build()

        // 1. Unanswered required question (currently omitted by the generator)
        val emptyAnswers = emptyMap<kotlin.String, Any>()
        val response1 = QuestionnaireResponseGenerator.generate(questionnaire, emptyAnswers)
        assertTrue(response1.item.isEmpty(), "Expected group to be omitted when deeply nested required question is unanswered")

        // 2. Answered nested question
        val answers = mapOf("nested_1" to "Some answer")
        val response2 = QuestionnaireResponseGenerator.generate(questionnaire, answers)
        assertEquals(1, response2.item.size)
        assertEquals("group_1", response2.item[0].linkId?.value)
        assertEquals(1, response2.item[0].item.size)
        assertEquals(
            "nested_1",
            response2.item[0]
                .item[0]
                .linkId
                ?.value,
        )
        val stringValueWrapper =
            response2.item[0]
                .item[0]
                .answer[0]
                .value as? com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String
        assertEquals(
            "Some answer",
            stringValueWrapper?.value?.value,
        )
    }
}
