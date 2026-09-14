/**
 * @file SdcEvaluatorMultiAnswerTest.kt
 * Unit tests verifying enableWhen condition evaluations against multi-select choice responses and answer lists.
 */
package io.healthplatform.chartcam.sdc

import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.String
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for multi-answer target question evaluations.
 */
class SdcEvaluatorMultiAnswerTest {
    /**
     * Tests that EqualTo condition matches if the target question's list of selected options includes the expected answer.
     */
    @Test
    fun testMultiAnswerContainment() {
        val ewAnswer =
            Questionnaire.Item.EnableWhen.Answer.String(
                String.Builder().apply { value = "opt_cough" }.build(),
            )
        val condition =
            Questionnaire.Item.EnableWhen
                .Builder(
                    answer = ewAnswer,
                    operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo),
                    question = String.Builder().apply { value = "symptoms" },
                ).build()

        val selectedSymptoms = listOf("opt_fever", "opt_cough", "opt_fatigue")
        assertTrue(SdcEvaluator.evaluateCondition(condition, mapOf("symptoms" to selectedSymptoms)))

        val otherSymptoms = listOf("opt_headache", "opt_nausea")
        assertFalse(SdcEvaluator.evaluateCondition(condition, mapOf("symptoms" to otherSymptoms)))
    }

    /**
     * Tests that NotEqualTo condition matches if the expected answer is NOT in the target question's list of answers.
     */
    @Test
    fun testMultiAnswerExclusion() {
        val ewAnswer =
            Questionnaire.Item.EnableWhen.Answer.String(
                String.Builder().apply { value = "opt_rash" }.build(),
            )
        val condition =
            Questionnaire.Item.EnableWhen
                .Builder(
                    answer = ewAnswer,
                    operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.NotEqualTo),
                    question = String.Builder().apply { value = "symptoms" },
                ).build()

        val symptomsWithoutRash = listOf("opt_fever", "opt_cough")
        assertTrue(SdcEvaluator.evaluateCondition(condition, mapOf("symptoms" to symptomsWithoutRash)))

        val symptomsWithRash = listOf("opt_fever", "opt_rash")
        assertFalse(SdcEvaluator.evaluateCondition(condition, mapOf("symptoms" to symptomsWithRash)))
    }

    /**
     * Tests Exists condition with multi-answer lists.
     */
    @Test
    fun testMultiAnswerExists() {
        val ewExists =
            Questionnaire.Item.EnableWhen.Answer.Boolean(
                Boolean.Builder().apply { value = true }.build(),
            )
        val condition =
            Questionnaire.Item.EnableWhen
                .Builder(
                    answer = ewExists,
                    operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.Exists),
                    question = String.Builder().apply { value = "symptoms" },
                ).build()

        assertTrue(SdcEvaluator.evaluateCondition(condition, mapOf("symptoms" to listOf("opt_fever"))))
        assertFalse(SdcEvaluator.evaluateCondition(condition, mapOf("symptoms" to emptyList<kotlin.String>())))
    }
}
