/**
 * @file SdcEvaluatorOperatorsTest.kt
 * Comprehensive unit tests for FHIR SDC enableWhen relational operators across all answer data types.
 */
package io.healthplatform.chartcam.sdc

import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Coding
import com.google.fhir.model.r4.Date
import com.google.fhir.model.r4.DateTime
import com.google.fhir.model.r4.Decimal
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirDate
import com.google.fhir.model.r4.FhirDateTime
import com.google.fhir.model.r4.Integer
import com.google.fhir.model.r4.Quantity
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.String
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for all SDC enableWhen operators and typed comparisons.
 */
class SdcEvaluatorOperatorsTest {
    /**
     * Helper to construct an enableWhen condition.
     *
     * @param targetQuestion The linkId of the question.
     * @param operator The relational operator.
     * @param answer The expected answer variant.
     * @return A configured EnableWhen item.
     */
    private fun createCondition(
        targetQuestion: kotlin.String,
        operator: Questionnaire.QuestionnaireItemOperator,
        answer: Questionnaire.Item.EnableWhen.Answer,
    ): Questionnaire.Item.EnableWhen =
        Questionnaire.Item.EnableWhen
            .Builder(
                answer = answer,
                operator = Enumeration(value = operator),
                question = String.Builder().apply { value = targetQuestion },
            ).build()

    /**
     * Tests EqualTo and NotEqualTo for String values.
     */
    @Test
    fun testStringEquality() {
        val ewString =
            Questionnaire.Item.EnableWhen.Answer.String(
                String.Builder().apply { value = "hypertension" }.build(),
            )
        val eqCond = createCondition("diagnosis", Questionnaire.QuestionnaireItemOperator.EqualTo, ewString)
        val neCond = createCondition("diagnosis", Questionnaire.QuestionnaireItemOperator.NotEqualTo, ewString)

        assertTrue(SdcEvaluator.evaluateCondition(eqCond, mapOf("diagnosis" to "hypertension")))
        assertFalse(SdcEvaluator.evaluateCondition(eqCond, mapOf("diagnosis" to "asthma")))
        assertFalse(SdcEvaluator.evaluateCondition(eqCond, emptyMap()))

        assertTrue(SdcEvaluator.evaluateCondition(neCond, mapOf("diagnosis" to "asthma")))
        assertFalse(SdcEvaluator.evaluateCondition(neCond, mapOf("diagnosis" to "hypertension")))
    }

    /**
     * Tests EqualTo and NotEqualTo for Boolean values.
     */
    @Test
    fun testBooleanEquality() {
        val ewBool =
            Questionnaire.Item.EnableWhen.Answer.Boolean(
                Boolean.Builder().apply { value = true }.build(),
            )
        val eqCond = createCondition("has_allergy", Questionnaire.QuestionnaireItemOperator.EqualTo, ewBool)
        val neCond = createCondition("has_allergy", Questionnaire.QuestionnaireItemOperator.NotEqualTo, ewBool)

        assertTrue(SdcEvaluator.evaluateCondition(eqCond, mapOf("has_allergy" to true)))
        assertTrue(SdcEvaluator.evaluateCondition(eqCond, mapOf("has_allergy" to "true")))
        assertFalse(SdcEvaluator.evaluateCondition(eqCond, mapOf("has_allergy" to false)))

        assertTrue(SdcEvaluator.evaluateCondition(neCond, mapOf("has_allergy" to false)))
        assertFalse(SdcEvaluator.evaluateCondition(neCond, mapOf("has_allergy" to true)))
    }

    /**
     * Tests EqualTo, NotEqualTo, and relational operators for Numeric (Integer & Decimal) values.
     */
    @Test
    fun testNumericComparisons() {
        val ewInt =
            Questionnaire.Item.EnableWhen.Answer.Integer(
                Integer.Builder().apply { value = 18 }.build(),
            )
        val gtCond = createCondition("age", Questionnaire.QuestionnaireItemOperator.GreaterThan, ewInt)
        val gteCond = createCondition("age", Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo, ewInt)
        val ltCond = createCondition("age", Questionnaire.QuestionnaireItemOperator.LessThan, ewInt)
        val lteCond = createCondition("age", Questionnaire.QuestionnaireItemOperator.LessThanOrEqualTo, ewInt)
        val eqCond = createCondition("age", Questionnaire.QuestionnaireItemOperator.EqualTo, ewInt)

        assertTrue(SdcEvaluator.evaluateCondition(gtCond, mapOf("age" to 21)))
        assertFalse(SdcEvaluator.evaluateCondition(gtCond, mapOf("age" to 18)))
        assertFalse(SdcEvaluator.evaluateCondition(gtCond, mapOf("age" to 15)))

        assertTrue(SdcEvaluator.evaluateCondition(gteCond, mapOf("age" to 18)))
        assertTrue(SdcEvaluator.evaluateCondition(gteCond, mapOf("age" to 19)))
        assertFalse(SdcEvaluator.evaluateCondition(gteCond, mapOf("age" to 17)))

        assertTrue(SdcEvaluator.evaluateCondition(ltCond, mapOf("age" to 10)))
        assertFalse(SdcEvaluator.evaluateCondition(ltCond, mapOf("age" to 18)))

        assertTrue(SdcEvaluator.evaluateCondition(lteCond, mapOf("age" to 18)))
        assertTrue(SdcEvaluator.evaluateCondition(lteCond, mapOf("age" to 12)))
        assertFalse(SdcEvaluator.evaluateCondition(lteCond, mapOf("age" to 25)))

        assertTrue(SdcEvaluator.evaluateCondition(eqCond, mapOf("age" to 18)))
        assertTrue(SdcEvaluator.evaluateCondition(eqCond, mapOf("age" to "18")))
        assertFalse(SdcEvaluator.evaluateCondition(eqCond, mapOf("age" to 19)))

        // Decimal comparison
        val ewDec =
            Questionnaire.Item.EnableWhen.Answer.Decimal(
                Decimal.Builder().apply { value = BigDecimal.parseString("38.5") }.build(),
            )
        val feverCond = createCondition("temp", Questionnaire.QuestionnaireItemOperator.GreaterThan, ewDec)
        assertTrue(SdcEvaluator.evaluateCondition(feverCond, mapOf("temp" to 39.0)))
        assertFalse(SdcEvaluator.evaluateCondition(feverCond, mapOf("temp" to 37.2)))
    }

    /**
     * Tests date and datetime chronological comparisons.
     */
    @Test
    fun testDateComparisons() {
        val ewDate =
            Questionnaire.Item.EnableWhen.Answer.Date(
                Date.Builder().apply { value = FhirDate.fromString("2026-06-01") }.build(),
            )
        val gtCond = createCondition("onset_date", Questionnaire.QuestionnaireItemOperator.GreaterThan, ewDate)
        val ltCond = createCondition("onset_date", Questionnaire.QuestionnaireItemOperator.LessThan, ewDate)
        val eqCond = createCondition("onset_date", Questionnaire.QuestionnaireItemOperator.EqualTo, ewDate)

        assertTrue(SdcEvaluator.evaluateCondition(gtCond, mapOf("onset_date" to "2026-07-01")))
        assertFalse(SdcEvaluator.evaluateCondition(gtCond, mapOf("onset_date" to "2026-05-01")))

        assertTrue(SdcEvaluator.evaluateCondition(ltCond, mapOf("onset_date" to "2026-05-01")))
        assertFalse(SdcEvaluator.evaluateCondition(ltCond, mapOf("onset_date" to "2026-06-01")))

        assertTrue(SdcEvaluator.evaluateCondition(eqCond, mapOf("onset_date" to "2026-06-01")))
        assertFalse(SdcEvaluator.evaluateCondition(eqCond, mapOf("onset_date" to "2026-06-02")))

        // DateTime comparison
        val ewDateTime =
            Questionnaire.Item.EnableWhen.Answer.DateTime(
                DateTime.Builder().apply { value = FhirDateTime.fromString("2026-06-01T12:00:00Z") }.build(),
            )
        val dtCond = createCondition("checkin_time", Questionnaire.QuestionnaireItemOperator.GreaterThan, ewDateTime)
        assertTrue(SdcEvaluator.evaluateCondition(dtCond, mapOf("checkin_time" to "2026-06-01T15:00:00Z")))
    }

    /**
     * Tests Coding comparison matching either code or display.
     */
    @Test
    fun testCodingComparison() {
        val ewCoding =
            Questionnaire.Item.EnableWhen.Answer.Coding(
                Coding
                    .Builder()
                    .apply {
                        code =
                            com.google.fhir.model.r4.Code
                                .Builder()
                                .apply { value = "opt_cough" }
                        display = String.Builder().apply { value = "Persistent Cough" }
                    }.build(),
            )
        val eqCond = createCondition("symptom", Questionnaire.QuestionnaireItemOperator.EqualTo, ewCoding)

        assertTrue(SdcEvaluator.evaluateCondition(eqCond, mapOf("symptom" to "opt_cough")))
        assertTrue(SdcEvaluator.evaluateCondition(eqCond, mapOf("symptom" to "Persistent Cough")))
        assertFalse(SdcEvaluator.evaluateCondition(eqCond, mapOf("symptom" to "opt_fever")))
    }

    /**
     * Tests Quantity comparison.
     */
    @Test
    fun testQuantityComparison() {
        val ewQuantity =
            Questionnaire.Item.EnableWhen.Answer.Quantity(
                Quantity
                    .Builder()
                    .apply {
                        value = Decimal.Builder().apply { value = BigDecimal.fromInt(100) }
                        unit = String.Builder().apply { value = "mg" }
                    }.build(),
            )
        val eqCond = createCondition("dosage", Questionnaire.QuestionnaireItemOperator.EqualTo, ewQuantity)
        val gtCond = createCondition("dosage", Questionnaire.QuestionnaireItemOperator.GreaterThan, ewQuantity)

        assertTrue(SdcEvaluator.evaluateCondition(eqCond, mapOf("dosage" to 100)))
        assertTrue(SdcEvaluator.evaluateCondition(gtCond, mapOf("dosage" to 150)))
        assertFalse(SdcEvaluator.evaluateCondition(gtCond, mapOf("dosage" to 50)))
    }

    /**
     * Tests the Exists operator for detecting presence or absence of an answer.
     */
    @Test
    fun testExistsOperator() {
        val ewExistsTrue =
            Questionnaire.Item.EnableWhen.Answer.Boolean(
                Boolean.Builder().apply { value = true }.build(),
            )
        val ewExistsFalse =
            Questionnaire.Item.EnableWhen.Answer.Boolean(
                Boolean.Builder().apply { value = false }.build(),
            )

        val condExists = createCondition("notes", Questionnaire.QuestionnaireItemOperator.Exists, ewExistsTrue)
        val condNotExists = createCondition("notes", Questionnaire.QuestionnaireItemOperator.Exists, ewExistsFalse)

        assertTrue(SdcEvaluator.evaluateCondition(condExists, mapOf("notes" to "Patient reports fatigue")))
        assertTrue(SdcEvaluator.evaluateCondition(condExists, mapOf("notes" to 42)))
        assertFalse(SdcEvaluator.evaluateCondition(condExists, mapOf("notes" to "")))
        assertFalse(SdcEvaluator.evaluateCondition(condExists, mapOf("notes" to emptyList<Any>())))
        assertFalse(SdcEvaluator.evaluateCondition(condExists, emptyMap()))

        assertTrue(SdcEvaluator.evaluateCondition(condNotExists, emptyMap()))
        assertTrue(SdcEvaluator.evaluateCondition(condNotExists, mapOf("notes" to "")))
        assertFalse(SdcEvaluator.evaluateCondition(condNotExists, mapOf("notes" to "something")))
    }

    /**
     * Tests multi-condition combining behaviors: All vs Any.
     */
    @Test
    fun testEnableBehaviorAllAndAny() {
        val ew1 =
            Questionnaire.Item.EnableWhen.Answer.Boolean(
                Boolean.Builder().apply { value = true }.build(),
            )
        val ew2 =
            Questionnaire.Item.EnableWhen.Answer.Integer(
                Integer.Builder().apply { value = 65 }.build(),
            )

        val itemAll =
            Questionnaire.Item
                .Builder(
                    linkId = String.Builder().apply { value = "high_risk_protocol" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    enableBehavior = Enumeration(value = Questionnaire.EnableWhenBehavior.All)
                    enableWhen.add(createCondition("is_smoker", Questionnaire.QuestionnaireItemOperator.EqualTo, ew1).toBuilder())
                    enableWhen.add(createCondition("age", Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo, ew2).toBuilder())
                }.build()

        val itemAny =
            itemAll
                .toBuilder()
                .apply {
                    enableBehavior = Enumeration(value = Questionnaire.EnableWhenBehavior.Any)
                }.build()

        // Both true
        val bothTrue = mapOf<kotlin.String, Any>("is_smoker" to true, "age" to 70)
        assertTrue(SdcEvaluator.isItemEnabled(itemAll, bothTrue))
        assertTrue(SdcEvaluator.isItemEnabled(itemAny, bothTrue))

        // One true, one false
        val oneTrue = mapOf<kotlin.String, Any>("is_smoker" to false, "age" to 70)
        assertFalse(SdcEvaluator.isItemEnabled(itemAll, oneTrue))
        assertTrue(SdcEvaluator.isItemEnabled(itemAny, oneTrue))

        // Both false
        val bothFalse = mapOf<kotlin.String, Any>("is_smoker" to false, "age" to 30)
        assertFalse(SdcEvaluator.isItemEnabled(itemAll, bothFalse))
        assertFalse(SdcEvaluator.isItemEnabled(itemAny, bothFalse))
    }
}
