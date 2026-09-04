/**
 * @file ComplexSdcEvaluatorExpansionTest.kt
 * Contains declarations for ComplexSdcEvaluatorExpansionTest.kt.
 */
package io.healthplatform.chartcam.sdc

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Extension
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.fhir.SdcExtensions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.google.fhir.model.r4.Boolean as FhirBoolean
import com.google.fhir.model.r4.String as FhirString

/**
 * Unit tests covering Section 3: Complex SDC Expressions & Evaluator Engine.
 */
class ComplexSdcEvaluatorExpansionTest {
    /**
     * Helper to create a calculatedExpression Extension.
     * @param expr The expression string.
     * @return The Extension.Builder.
     */
    private fun createCalcExtension(expr: String): Extension.Builder =
        Extension.Builder(url = SdcExtensions.CALCULATED_EXPRESSION).apply {
            extension.add(
                Extension.Builder(url = "expression").apply {
                    value = Extension.Value.String(FhirString.Builder().apply { value = expr }.build())
                },
            )
        }

    /**
     * Helper to create an initialExpression Extension.
     * @param expr The expression string.
     * @return The Extension.Builder.
     */
    private fun createInitialExtension(expr: String): Extension.Builder =
        Extension.Builder(url = SdcExtensions.INITIAL_EXPRESSION).apply {
            extension.add(
                Extension.Builder(url = "expression").apply {
                    value = Extension.Value.String(FhirString.Builder().apply { value = expr }.build())
                },
            )
        }

    /**
     * Test deeply nested fhirpath calculations across multi-level groups.
     */
    @Test
    fun testDeeplyNestedCalculations() {
        // Grandparent -> Parent Group -> Child Item (Calculated)
        val childItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "child_score" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                ).apply {
                    extension.add(createCalcExtension("(%a * 2) + (%b / 2)"))
                }

        val childConcat =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "child_full_name" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(createCalcExtension("concat(%first_name, ' ', %last_name)"))
                }

        val parentGroup =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "parent_group" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).apply {
                    item.add(childItem)
                    item.add(childConcat)
                }

        val rootGroup =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "root_group" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).apply {
                    item.add(parentGroup)
                }

        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    item.add(rootGroup)
                }.build()

        val answers =
            mutableMapOf<String, Any>(
                "a" to 10f,
                "b" to 20f,
                "first_name" to "Jane",
                "last_name" to "Doe",
            )

        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(30f, result["child_score"])
        assertEquals("Jane Doe", result["child_full_name"])
    }

    /**
     * Test recursive enableWhen hierarchies where child items depend on multi-level parent conditions.
     */
    @Test
    fun testRecursiveEnableWhenHierarchies() {
        // Parent item enabled when "is_hospitalized" == true
        val parentItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "hospital_stay_details" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).apply {
                    enableWhen.add(
                        Questionnaire.Item.EnableWhen
                            .Builder(
                                answer =
                                    Questionnaire.Item.EnableWhen.Answer
                                        .Boolean(FhirBoolean.Builder().apply { value = true }.build()),
                                operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo),
                                question = FhirString.Builder().apply { value = "is_hospitalized" },
                            ),
                    )
                }.build()

        // Child item enabled when "icu_stay" == true
        val childItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "icu_bed_number" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    enableWhen.add(
                        Questionnaire.Item.EnableWhen
                            .Builder(
                                answer =
                                    Questionnaire.Item.EnableWhen.Answer
                                        .Boolean(FhirBoolean.Builder().apply { value = true }.build()),
                                operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo),
                                question = FhirString.Builder().apply { value = "icu_stay" },
                            ),
                    )
                }.build()

        // Case 1: Parent disabled (is_hospitalized = false), child condition true (icu_stay = true)
        val answers1 = mapOf<String, Any>("is_hospitalized" to false, "icu_stay" to true)
        assertFalse(SdcEvaluator.isItemHierarchyEnabled(childItem, listOf(parentItem), answers1))

        // Case 2: Parent enabled (is_hospitalized = true), child condition false (icu_stay = false)
        val answers2 = mapOf<String, Any>("is_hospitalized" to true, "icu_stay" to false)
        assertFalse(SdcEvaluator.isItemHierarchyEnabled(childItem, listOf(parentItem), answers2))

        // Case 3: Both parent and child conditions met -> enabled
        val answers3 = mapOf<String, Any>("is_hospitalized" to true, "icu_stay" to true)
        assertTrue(SdcEvaluator.isItemHierarchyEnabled(childItem, listOf(parentItem), answers3))
    }

    /**
     * Verify type coercion and boundary values: division by zero, null propagation, date arithmetic.
     */
    @Test
    fun testTypeCoercionAndBoundaryValues() {
        // Division by zero should safely evaluate to null
        val divZeroResult = SdcEvaluator.evaluateExpression("100 / 0", emptyMap())
        assertNull(divZeroResult, "Division by zero must return null")

        // Null operand propagation: missing variable replaced with 0
        val nullOperandResult = SdcEvaluator.evaluateExpression("%missing_var + 42", emptyMap())
        assertEquals(42f, nullOperandResult)

        // Type coercion from string representations
        val coercedResult = SdcEvaluator.evaluateExpression("%str_val * 3", mapOf("str_val" to "15"))
        assertEquals(45f, coercedResult)

        // Date arithmetic leap year and month boundary
        val dateNormal = SdcEvaluator.evaluateDateOffset("2026-01-30", 2)
        assertEquals("2026-02-01", dateNormal)

        // Leap day
        val leapDate = SdcEvaluator.evaluateDateOffset("2024-02-28", 1)
        assertEquals("2024-02-29", leapDate)

        // Logical comparisons in SdcEvaluator
        val compGt = SdcEvaluator.evaluateLogicalExpression("%score > 50", mapOf("score" to 75f))
        assertEquals(true, compGt)

        val compLte = SdcEvaluator.evaluateLogicalExpression("%score <= 50", mapOf("score" to 75f))
        assertEquals(false, compLte)

        // Compound logical expressions
        val compAnd = SdcEvaluator.evaluateLogicalExpression("%a > 10 && %b < 20", mapOf("a" to 15f, "b" to 12f))
        assertEquals(true, compAnd)

        val compOr = SdcEvaluator.evaluateLogicalExpression("%a > 50 || %b < 20", mapOf("a" to 15f, "b" to 12f))
        assertEquals(true, compOr)
    }

    /**
     * Test evaluation of dynamic defaults derived from patient demographics or previous encounter responses.
     */
    @Test
    fun testInitialExpressionDefaults() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "pat_name" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ).apply {
                                extension.add(createInitialExtension("%patient.name"))
                            },
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "pat_gender" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ).apply {
                                extension.add(createInitialExtension("%patient.gender"))
                            },
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "encounter_ref" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ).apply {
                                extension.add(createInitialExtension("%encounter.id"))
                            },
                    )
                }.build()

        val context =
            mapOf<String, Any?>(
                "patient.name" to "Alice Smith",
                "patient.gender" to "female",
                "encounter.id" to "enc-uuid-12345",
            )

        val initialAnswers = SdcEvaluator.evaluateInitialExpressions(q, context)
        assertEquals("Alice Smith", initialAnswers["pat_name"])
        assertEquals("female", initialAnswers["pat_gender"])
        assertEquals("enc-uuid-12345", initialAnswers["encounter_ref"])
    }
}
