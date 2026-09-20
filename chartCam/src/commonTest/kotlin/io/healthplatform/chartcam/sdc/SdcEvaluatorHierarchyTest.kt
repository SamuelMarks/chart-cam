/**
 * @file SdcEvaluatorHierarchyTest.kt
 * Unit tests verifying hierarchical cascade enablement where parent group status controls child question visibility.
 */
package io.healthplatform.chartcam.sdc

import dev.ohs.fhir.model.r4.Boolean
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.String
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for SdcEvaluator hierarchical ancestor checking.
 */
class SdcEvaluatorHierarchyTest {
    /**
     * Tests that a child item is automatically disabled if its parent group is disabled.
     */
    @Test
    fun testParentDisablesChild() {
        val ewTrue =
            Questionnaire.Item.EnableWhen.Answer.Boolean(
                Boolean.Builder().apply { value = true }.build(),
            )

        // Parent group enabled only when "has_symptoms" is true
        val parentGroup =
            Questionnaire.Item
                .Builder(
                    linkId = String.Builder().apply { value = "group_symptoms" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).apply {
                    enableWhen.add(
                        Questionnaire.Item.EnableWhen
                            .Builder(
                                answer = ewTrue,
                                operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo),
                                question = String.Builder().apply { value = "has_symptoms" },
                            ),
                    )
                }.build()

        // Child question enabled when "fever_present" is true
        val childQuestion =
            Questionnaire.Item
                .Builder(
                    linkId = String.Builder().apply { value = "temp_reading" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                ).apply {
                    enableWhen.add(
                        Questionnaire.Item.EnableWhen
                            .Builder(
                                answer = ewTrue,
                                operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo),
                                question = String.Builder().apply { value = "fever_present" },
                            ),
                    )
                }.build()

        // Case 1: Parent condition false -> Child must be disabled even though fever_present is true!
        val answersParentFalse = mapOf<kotlin.String, Any>("has_symptoms" to false, "fever_present" to true)
        assertFalse(SdcEvaluator.isItemHierarchyEnabled(childQuestion, listOf(parentGroup), answersParentFalse))

        // Case 2: Parent condition true, but Child condition false -> Child is disabled
        val answersChildFalse = mapOf<kotlin.String, Any>("has_symptoms" to true, "fever_present" to false)
        assertFalse(SdcEvaluator.isItemHierarchyEnabled(childQuestion, listOf(parentGroup), answersChildFalse))

        // Case 3: Both parent and child conditions true -> Child is enabled
        val answersBothTrue = mapOf<kotlin.String, Any>("has_symptoms" to true, "fever_present" to true)
        assertTrue(SdcEvaluator.isItemHierarchyEnabled(childQuestion, listOf(parentGroup), answersBothTrue))
    }

    /**
     * Tests hierarchy check when ancestors list is empty.
     */
    @Test
    fun testEmptyAncestors() {
        val item =
            Questionnaire.Item
                .Builder(
                    linkId = String.Builder().apply { value = "standalone_question" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).build()

        assertTrue(SdcEvaluator.isItemHierarchyEnabled(item, emptyList(), emptyMap()))
    }
}
