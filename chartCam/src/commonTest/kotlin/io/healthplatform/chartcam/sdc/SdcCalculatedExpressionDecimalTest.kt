/**
 * @file SdcCalculatedExpressionDecimalTest.kt
 * Unit tests for FhirDecimal calculation and sealed choice type extraction in SDC expressions.
 */

package io.healthplatform.chartcam.sdc

import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Decimal
import dev.ohs.fhir.model.r4.Extension
import dev.ohs.fhir.model.r4.FhirDecimal
import dev.ohs.fhir.model.r4.Integer
import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.fhir.SdcExtensions
import io.healthplatform.chartcam.fhir.safeInitialExpression
import io.healthplatform.chartcam.fhir.safeItemControl
import io.healthplatform.chartcam.fhir.safeMaxValue
import io.healthplatform.chartcam.fhir.safeMinValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Test suite verifying arbitrary-precision calculations and sealed choice extraction in SDC engine.
 */
class SdcCalculatedExpressionDecimalTest {
    /**
     * Verifies BMI calculation produces an accurate [FhirDecimal] wrapped in [Result].
     */
    @Test
    fun testBmiCalculationDecimal() {
        val expression = "%weight / (%height * %height)"
        val answers = mapOf("weight" to 70.0, "height" to 1.75)

        val result = SdcEvaluator.evaluateCalculatedDecimalExpression(expression, answers)
        assertTrue(result.isSuccess)

        val decimal = result.getOrNull()
        assertNotNull(decimal)
        val valueStr = decimal.toString()
        assertTrue(valueStr.startsWith("22.85") || valueStr.startsWith("22.86"))
    }

    /**
     * Verifies dosage calculation with pediatric coefficients.
     */
    @Test
    fun testPediatricDosageCalculation() {
        val expression = "(%weight * 15) / 3"
        val answers = mapOf("weight" to 12.0)

        val result = SdcEvaluator.evaluateCalculatedDecimalExpression(expression, answers)
        assertTrue(result.isSuccess)
        assertEquals("60.0", result.getOrNull()?.toString())
    }

    /**
     * Verifies that division by zero returns a graceful failure rather than crashing.
     */
    @Test
    fun testDivisionByZeroReturnsFailure() {
        val expression = "%dose / %count"
        val answers = mapOf("dose" to 100.0, "count" to 0.0)

        val result = SdcEvaluator.evaluateCalculatedDecimalExpression(expression, answers)
        assertTrue(result.isFailure)
    }

    /**
     * Verifies sealed choice type extraction on [Questionnaire.Item] extensions.
     */
    @Test
    fun testSafeExtensionAccessors() {
        val item =
            Questionnaire.Item(
                linkId = FhirString(value = "item-1"),
                type =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                extension =
                    listOf(
                        Extension(
                            url = SdcExtensions.ITEM_CONTROL,
                            value =
                                Extension.Value.CodeableConcept(
                                    CodeableConcept(
                                        coding =
                                            listOf(
                                                Coding(
                                                    code =
                                                        dev.ohs.fhir.model.r4
                                                            .Code(value = SdcExtensions.ITEM_CONTROL_PAIN_VAS),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                        Extension(
                            url = SdcExtensions.MIN_VALUE,
                            value = Extension.Value.Integer(Integer(value = 5)),
                        ),
                        Extension(
                            url = SdcExtensions.MAX_VALUE,
                            value = Extension.Value.Decimal(Decimal(value = FhirDecimal.fromString("100.5"))),
                        ),
                        Extension(
                            url = SdcExtensions.INITIAL_EXPRESSION,
                            value = Extension.Value.String(FhirString(value = "%patient.age * 2")),
                        ),
                    ),
            )

        assertEquals(SdcExtensions.ITEM_CONTROL_PAIN_VAS, item.safeItemControl.getOrNull())
        assertEquals("5", item.safeMinValue.getOrNull()?.toString())
        assertEquals("100.5", item.safeMaxValue.getOrNull()?.toString())
        assertEquals("%patient.age * 2", item.safeInitialExpression.getOrNull())
    }

    /**
     * Verifies SdcMathEvaluator arbitrary-precision decimal operations and Result handling.
     */
    @Test
    fun testSdcMathEvaluatorArbitraryPrecision() {
        val addResult = SdcMathEvaluator.evalSimpleMath("0.1 + 0.2")
        assertTrue(addResult.isSuccess)
        assertEquals("0.3", addResult.getOrNull()?.toString())

        val divZero = SdcMathEvaluator.evalSimpleMath("10 / 0")
        assertTrue(divZero.isFailure)

        val complex = SdcMathEvaluator.evalSimpleMath("(10.5 * 2) + (5.25 / 2.5)")
        assertTrue(complex.isSuccess)
        assertEquals("23.1", complex.getOrNull()?.toString())
    }

    /**
     * Verifies parsing edge cases, quote parsing, missing variables, and float evaluation in SdcMathEvaluator.
     */
    @Test
    fun testExhaustiveSdcMathEvaluatorBranches() {
        // Empty string returns 0
        val emptyRes = SdcMathEvaluator.evalSimpleMath("")
        assertTrue(emptyRes.isSuccess)
        assertEquals("0", emptyRes.getOrNull()?.toString())

        // Mismatched parentheses
        val parenMismatch = SdcMathEvaluator.evalSimpleMath(")(")
        assertTrue(parenMismatch.isFailure)

        val unclosedParen = SdcMathEvaluator.evalSimpleMath("((5 + 2)")
        assertTrue(unclosedParen.isFailure)

        // Float evaluation
        val floatVal = SdcMathEvaluator.evalSimpleMathFloat("12.5 + 7.5").getOrNull()
        assertEquals(20.0f, floatVal)

        // Multiple addition/subtraction terms triggering opMatch
        val multiAdd = SdcMathEvaluator.evalSimpleMath("1 + 2 + 3 - 4")
        assertTrue(multiAdd.isSuccess)
        assertEquals("2", multiAdd.getOrNull()?.toString())

        val singleAdd = SdcMathEvaluator.evalSimpleMath("10 + 5")
        assertTrue(singleAdd.isSuccess)
        assertEquals("15", singleAdd.getOrNull()?.toString())

        // splitArgumentsRespectingQuotes with parentheses
        val argsWithParens = SdcMathEvaluator.splitArgumentsRespectingQuotes("concat('a', fn(1, 2)), 'b'")
        assertEquals(2, argsWithParens.size)

        // splitByPlusRespectingQuotes with double quotes and plus inside quotes
        val plusWithDoubleQuotes = SdcMathEvaluator.splitByPlusRespectingQuotes("\"first\" + \"second\" + 'third'")
        assertEquals(3, plusWithDoubleQuotes.size)

        val plusInsideQuotes = SdcMathEvaluator.splitByPlusRespectingQuotes("'a + b' + 'c'")
        assertEquals(2, plusInsideQuotes.size)

        // evaluateStringExpression with default answers parameter
        val defaultAnswersEval = SdcMathEvaluator.evaluateStringExpression("'hello ' + 'world'").getOrThrow()
        assertEquals("hello world", defaultAnswersEval)

        // evaluateStringExpression with missing variables (%var) in concat and plus
        val concatMissing =
            SdcMathEvaluator
                .evaluateStringExpression(
                    "concat(%missing_var, 'test')",
                    emptyMap<kotlin.String, Any?>(),
                ).getOrThrow()
        assertEquals("test", concatMissing)

        val plusMissing =
            SdcMathEvaluator
                .evaluateStringExpression(
                    "%missing_var + 'test ' + %also_missing",
                    emptyMap<kotlin.String, Any?>(),
                ).getOrThrow()
        assertEquals("test ", plusMissing)

        // Negative numbers in addition/subtraction
        val negSub = SdcMathEvaluator.evalSimpleMath("-5 + 10")
        assertTrue(negSub.isSuccess)
        assertEquals("5", negSub.getOrNull()?.toString())

        val negSub2 = SdcMathEvaluator.evalSimpleMath("-5 - 10")
        assertTrue(negSub2.isSuccess)
        assertEquals("-15", negSub2.getOrNull()?.toString())

        val subNeg = SdcMathEvaluator.evalSimpleMath("5 - -10")
        assertTrue(subNeg.isSuccess)
        assertEquals("15", subNeg.getOrNull()?.toString())

        // splitArgumentsRespectingQuotes unquoted and mismatched quotes
        val simpleArgs = SdcMathEvaluator.splitArgumentsRespectingQuotes("a, b, c")
        assertEquals(3, simpleArgs.size)

        val unclosedQuoteArgs = SdcMathEvaluator.splitArgumentsRespectingQuotes("'unclosed, b")
        assertEquals(1, unclosedQuoteArgs.size)

        // splitByPlusRespectingQuotes simple and unquoted
        val simplePlus = SdcMathEvaluator.splitByPlusRespectingQuotes("a + b + c")
        assertEquals(3, simplePlus.size)

        // evaluateStringExpression with unquoted variable that exists in answers
        val varAnswers = mapOf("firstName" to "Alice", "lastName" to "Smith")
        val concatVars = SdcMathEvaluator.evaluateStringExpression("concat(%firstName, ' ', %lastName)", varAnswers).getOrThrow()
        assertEquals("Alice Smith", concatVars)

        val plusVars = SdcMathEvaluator.evaluateStringExpression("%firstName + ' ' + %lastName", varAnswers).getOrThrow()
        assertEquals("Alice Smith", plusVars)

        // evaluateStringExpression with plain string without concat or plus
        val plainStr = SdcMathEvaluator.evaluateStringExpression("'plain string'").getOrThrow()
        assertEquals("plain string", plainStr)

        // Nested single quotes inside double quotes and vice versa in splitArgumentsRespectingQuotes
        val nestedQuotesArgs = SdcMathEvaluator.splitArgumentsRespectingQuotes("\"a 'b' c\", 'd \"e\" f', a), b")
        assertEquals(4, nestedQuotesArgs.size)
        assertEquals(emptyList<String>(), SdcMathEvaluator.splitArgumentsRespectingQuotes(""))

        // Nested quotes and plus signs in splitByPlusRespectingQuotes
        val nestedPlus = SdcMathEvaluator.splitByPlusRespectingQuotes("\"a 'b' + c\" + 'd \"e\" + f' + g")
        assertEquals(3, nestedPlus.size)
        assertEquals(emptyList<String>(), SdcMathEvaluator.splitByPlusRespectingQuotes(""))

        // Malformed consecutive operators
        val malformedOps = SdcMathEvaluator.evalSimpleMath("1 ++ 2")
        assertTrue(malformedOps.isFailure)

        // Null variable values in evaluateStringExpression
        val answersWithNull = mapOf<String, Any?>("nullVar" to null)
        val concatNull = SdcMathEvaluator.evaluateStringExpression("concat(%nullVar, 'value')", answersWithNull).getOrThrow()
        assertEquals("value", concatNull)
        val plusNull = SdcMathEvaluator.evaluateStringExpression("%nullVar + 'value'", answersWithNull).getOrThrow()
        assertEquals("value", plusNull)

        // Unquoted raw text without percent
        val concatRaw = SdcMathEvaluator.evaluateStringExpression("concat(raw, 'text')").getOrThrow()
        assertEquals("rawtext", concatRaw)
        val plusRaw = SdcMathEvaluator.evaluateStringExpression("raw + text").getOrThrow()
        assertEquals("rawtext", plusRaw)

        // Single number expression with no addition or subtraction
        val singleNum = SdcMathEvaluator.evalSimpleMath("42")
        assertTrue(singleNum.isSuccess)
        assertEquals("42", singleNum.getOrNull()?.toString())

        val mulOnly = SdcMathEvaluator.evalSimpleMath("5 * 2")
        assertTrue(mulOnly.isSuccess)
        assertEquals("10", mulOnly.getOrNull()?.toString())

        // Expression starting with concat( but not ending with )
        val unclosedConcat = SdcMathEvaluator.evaluateStringExpression("concat('unclosed', 'b'")
        assertTrue(unclosedConcat.isSuccess)
        assertEquals("concat('unclosed', 'b", unclosedConcat.getOrNull())
    }
}
