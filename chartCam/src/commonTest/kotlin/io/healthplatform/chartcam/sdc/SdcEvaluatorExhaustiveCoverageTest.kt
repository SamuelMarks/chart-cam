/**
 * @file SdcEvaluatorExhaustiveCoverageTest.kt
 * Exhaustive unit tests for SdcEvaluator targeting 100% line, branch, and instruction coverage.
 */

package io.healthplatform.chartcam.sdc

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Date
import dev.ohs.fhir.model.r4.DateTime
import dev.ohs.fhir.model.r4.Decimal
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Extension
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.FhirDecimal
import dev.ohs.fhir.model.r4.Quantity
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Time
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.Integer as FhirInteger
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Exhaustive test suite covering all edge cases, operators, and data types in [SdcEvaluator].
 */
class SdcEvaluatorExhaustiveCoverageTest {
    /**
     * Builds a Questionnaire Item Builder with a calculatedExpression extension.
     */
    private fun createItemWithCalc(
        linkId: String?,
        expr: String,
        isDirectValue: Boolean = false,
    ): Questionnaire.Item.Builder {
        val calcExt =
            Extension.Builder(url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression").apply {
                if (isDirectValue) {
                    value = Extension.Value.String(FhirString.Builder().apply { value = expr }.build())
                } else {
                    extension.add(
                        Extension.Builder(url = "expression").apply {
                            value = Extension.Value.String(FhirString.Builder().apply { value = expr }.build())
                        },
                    )
                }
            }

        return Questionnaire.Item
            .Builder(
                linkId = FhirString.Builder().apply { value = linkId },
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
            ).apply {
                extension.add(calcExt)
            }
    }

    /**
     * Builds a Questionnaire Item Builder with an initialExpression extension.
     */
    private fun createItemWithInit(
        linkId: String?,
        expr: String,
        isDirectValue: Boolean = false,
    ): Questionnaire.Item.Builder {
        val initExt =
            Extension.Builder(url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression").apply {
                if (isDirectValue) {
                    value = Extension.Value.String(FhirString.Builder().apply { value = expr }.build())
                } else {
                    extension.add(
                        Extension.Builder(url = "expression").apply {
                            value = Extension.Value.String(FhirString.Builder().apply { value = expr }.build())
                        },
                    )
                }
            }

        return Questionnaire.Item
            .Builder(
                linkId = FhirString.Builder().apply { value = linkId },
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
            ).apply {
                extension.add(initExt)
            }
    }

    /**
     * Builds an enableWhen rule.
     */
    private fun createCondition(
        targetQuestion: String?,
        operator: Questionnaire.QuestionnaireItemOperator?,
        answer: Questionnaire.Item.EnableWhen.Answer,
    ): Questionnaire.Item.EnableWhen {
        val qBuilder = FhirString.Builder().apply { value = targetQuestion }
        val opEnum = if (operator != null) Enumeration(value = operator) else Enumeration()
        return Questionnaire.Item.EnableWhen
            .Builder(
                answer = answer,
                operator = opEnum,
                question = qBuilder,
            ).build()
    }

    @Test
    fun testCalculatedExpressionsCycleAndMaxIterations() {
        // Circular dependency test
        val qCycle =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    item.add(createItemWithCalc("a", "%b + 1"))
                    item.add(createItemWithCalc("b", "%a + 1"))
                }.build()

        val initial = mapOf<String, Any>("a" to 1f, "b" to 2f)
        val cycleRes = SdcEvaluator.evaluateCalculatedExpressions(qCycle, initial)
        assertEquals(initial, cycleRes)

        // Max iterations test with 6 cascading items
        val qMax =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    item.add(createItemWithCalc("i1", "%i2 + 1"))
                    item.add(createItemWithCalc("i2", "%i3 + 1"))
                    item.add(createItemWithCalc("i3", "%i4 + 1"))
                    item.add(createItemWithCalc("i4", "%i5 + 1"))
                    item.add(createItemWithCalc("i5", "%i6 + 1"))
                    item.add(createItemWithCalc("i6", "%seed + 1"))
                }.build()
        val maxRes = SdcEvaluator.evaluateCalculatedExpressions(qMax, mapOf("seed" to 10f))
        assertTrue(maxRes.containsKey("i6"))

        // Item with no linkId, plain item, and unchanged constant calculation
        val plainItem =
            Questionnaire.Item.Builder(
                linkId = FhirString.Builder().apply { value = "plain" },
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
            )
        val qNoLink =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    item.add(createItemWithCalc(null, "5 + 5"))
                    item.add(createItemWithCalc("constant", "10 + 10"))
                    item.add(createItemWithCalc("invalidCalc", "((%a > 5"))
                    item.add(plainItem)
                }.build()
        val noLinkRes = SdcEvaluator.evaluateCalculatedExpressions(qNoLink, mapOf("constant" to 20f))
        assertEquals(20f, noLinkRes["constant"])

        // Empty extension test
        val emptyExt =
            Extension.Builder(url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression").build()
        val itemWithEmptyExt =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "emptyExtItem" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(emptyExt.toBuilder())
                }.build()
        val qEmptyExt =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    item.add(itemWithEmptyExt.toBuilder())
                }.build()
        assertEquals(emptyMap(), SdcEvaluator.evaluateCalculatedExpressions(qEmptyExt, emptyMap()))
    }

    @Test
    fun testEvaluateCalculatedValueBranches() {
        assertEquals(0f, SdcEvaluator.evaluateCalculatedValue("", emptyMap()))
        assertEquals(0f, SdcEvaluator.evaluateCalculatedValue("   ", emptyMap()))
        assertNull(SdcEvaluator.evaluateCalculatedValue("((%a > 5", emptyMap()))

        // String expressions
        val strRes1 = SdcEvaluator.evaluateCalculatedValue("concat('Hello', ' World')", emptyMap())
        assertEquals("Hello World", strRes1)
        val strResConcatUnquoted = SdcEvaluator.evaluateCalculatedValue("concat(%x, %y)", mapOf("x" to "Foo", "y" to "Bar"))
        assertEquals("FooBar", strResConcatUnquoted)
        val strRes2 = SdcEvaluator.evaluateCalculatedValue("'Single' + 'Quote'", emptyMap())
        assertEquals("SingleQuote", strRes2)
        val strRes3 = SdcEvaluator.evaluateCalculatedValue("\"Double\" + \"Quote\"", emptyMap())
        assertEquals("DoubleQuote", strRes3)

        // Logical comparisons
        val answers = mapOf<String, Any>("x" to 10f, "y" to 20f)
        assertEquals(true, SdcEvaluator.evaluateCalculatedValue("%y > %x", answers))
        assertEquals(false, SdcEvaluator.evaluateCalculatedValue("%x > %y", answers))
        assertEquals(true, SdcEvaluator.evaluateCalculatedValue("%x < %y", answers))
        assertEquals(false, SdcEvaluator.evaluateCalculatedValue("%y < %x", answers))
        assertEquals(true, SdcEvaluator.evaluateCalculatedValue("%x == 10", answers))
        assertEquals(false, SdcEvaluator.evaluateCalculatedValue("%x == 20", answers))
        assertEquals(true, SdcEvaluator.evaluateCalculatedValue("%x != 20", answers))
        assertEquals(false, SdcEvaluator.evaluateCalculatedValue("%x != 10", answers))

        // Direct arithmetic
        assertEquals(30f, SdcEvaluator.evaluateCalculatedValue("%x + %y", answers))
    }

    @Test
    fun testEvaluateExpressionAndToNumericFloat() {
        assertNull(SdcEvaluator.evaluateExpression(""))
        assertNull(SdcEvaluator.evaluateExpression("   "))
        assertNull(SdcEvaluator.evaluateExpression("%a ^ 2", mapOf("a" to 3)))
        assertNull(SdcEvaluator.evaluateExpression("1 / 0"))
        assertNull(SdcEvaluator.evaluateExpression("5 +* 3"))
        assertNull(
            SdcEvaluator.evaluateExpression(
                "1000000000000000000000000000000000000000 * 1000000000000000000000000000000000000000",
            ),
        )

        val diverseAnswers =
            mapOf<String, Any?>(
                "nullVal" to null,
                "floatVal" to 1.5f,
                "doubleVal" to 2.5,
                "intVal" to 3,
                "longVal" to 4L,
                "numberVal" to (5.0 as Number),
                "fhirDec" to FhirDecimal.fromString("1.0"),
                "bigDec" to BigDecimal.fromInt(2),
                "boolTrue" to true,
                "boolFalse" to false,
                "blankStr" to "   ",
                "validStr" to "6.5",
                "invalidStr" to "not_a_num",
                "otherVal" to Any(),
            )

        val expr =
            "%nullVal + %floatVal + %doubleVal + %intVal + %longVal + %numberVal + " +
                "%fhirDec + %bigDec + %boolTrue + %boolFalse + %blankStr + %validStr + " +
                "%invalidStr + %otherVal + %missing"
        val sum = SdcEvaluator.evaluateExpression(expr, diverseAnswers)
        // 0 + 1.5 + 2.5 + 3 + 4 + 5.0 + 1.0 + 2.0 + 1 + 0 + 0 + 6.5 + 0 + 0 + 0 = 26.5
        assertEquals(26.5f, sum)
    }

    @Test
    fun testStringAndLogicalExpressions() {
        val strResult = SdcEvaluator.evaluateStringExpression("concat('foo', 'bar')")
        assertEquals("foobar", strResult.getOrNull())

        val logicalValid = SdcEvaluator.evaluateLogicalExpression("true && true")
        assertEquals(true, logicalValid.getOrNull())

        val logicalEmpty = SdcEvaluator.evaluateLogicalExpression("   ")
        assertTrue(logicalEmpty.isFailure)
    }

    @Test
    fun testEvaluateCalculatedDecimalExpression() {
        assertTrue(SdcEvaluator.evaluateCalculatedDecimalExpression("").isFailure)
        assertTrue(SdcEvaluator.evaluateCalculatedDecimalExpression("   ").isFailure)

        val answers =
            mapOf<String, Any?>(
                "dec" to FhirDecimal.fromString("10.5"),
                "num" to 20,
                "other" to "29.5",
            )
        val res = SdcEvaluator.evaluateCalculatedDecimalExpression("%dec + %num + %other", answers)
        assertTrue(res.isSuccess)
        assertEquals("60.0", res.getOrThrow().toString())

        val normalRes = SdcEvaluator.evaluateCalculatedDecimalExpression("%dec + %num", answers)
        assertTrue(normalRes.isSuccess)
        assertEquals("30.5", normalRes.getOrThrow().toString())

        val resWithNull =
            SdcEvaluator.evaluateCalculatedDecimalExpression(
                "%dec + %nullKey + %missingKey",
                mapOf("dec" to FhirDecimal.fromString("10"), "nullKey" to null),
            )
        assertTrue(resWithNull.isSuccess)
        assertEquals("10", resWithNull.getOrThrow().toString())
    }

    @Test
    fun testInitialExpressionsExtractionAndRecursion() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    // Item with expression sub-extension
                    item.add(createItemWithInit("initSub", "%user.name"))
                    // Item with direct valueString
                    item.add(createItemWithInit("initDirect", "user.age", isDirectValue = true))
                    // Item with key matching percent in context
                    item.add(createItemWithInit("initPercentKey", "%patient.weight"))
                    // Item without linkId
                    item.add(createItemWithInit(null, "%user.role"))
                    // Plain item without initial expression
                    item.add(
                        Questionnaire.Item.Builder(
                            linkId = FhirString.Builder().apply { value = "plainInit" },
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                    )
                    // Item with other extension URL
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "otherExtInit" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ).apply {
                                extension.add(Extension.Builder(url = "http://example.com/other-ext"))
                            },
                    )
                    // Nested item
                    val groupItem =
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "group1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                            ).apply {
                                item.add(createItemWithInit("childInit", "%patient.gender"))
                            }
                    item.add(groupItem)
                }.build()

        val context =
            mapOf<String, Any?>(
                "user.name" to "Alice",
                "user.age" to 30,
                "patient.gender" to "female",
                "%patient.weight" to 70,
            )

        val initialAnswers = SdcEvaluator.evaluateInitialExpressions(q, context)
        assertEquals("Alice", initialAnswers["initSub"])
        assertEquals(30, initialAnswers["initDirect"])
        assertEquals(70, initialAnswers["initPercentKey"])
        assertEquals("female", initialAnswers["childInit"])

        val defaultAnswers = SdcEvaluator.evaluateInitialExpressions(q)
        assertTrue(defaultAnswers.isEmpty())
    }

    @Test
    fun testIsItemHierarchyEnabledAndBehaviors() {
        val cond =
            createCondition(
                "q1",
                Questionnaire.QuestionnaireItemOperator.EqualTo,
                Questionnaire.Item.EnableWhen.Answer
                    .String(FhirString.Builder().apply { value = "yes" }.build()),
            )

        val disabledParent =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "parent" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).apply {
                    enableWhen.add(cond.toBuilder())
                }.build()

        val child =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "child" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).build()

        // Disabled parent blocks child
        assertFalse(SdcEvaluator.isItemHierarchyEnabled(child, listOf(disabledParent), emptyMap()))

        // Enabled parent allows enabled child
        val enabledAnswers = mapOf("q1" to "yes")
        assertTrue(SdcEvaluator.isItemHierarchyEnabled(child, listOf(disabledParent), enabledAnswers))
        assertTrue(SdcEvaluator.isItemHierarchyEnabled(child, answers = enabledAnswers))

        // All vs Any enableBehavior
        val cond2 =
            createCondition(
                "q2",
                Questionnaire.QuestionnaireItemOperator.EqualTo,
                Questionnaire.Item.EnableWhen.Answer
                    .String(FhirString.Builder().apply { value = "ok" }.build()),
            )

        val allItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "all" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    enableBehavior = Enumeration(value = Questionnaire.EnableWhenBehavior.All)
                    enableWhen.add(cond.toBuilder())
                    enableWhen.add(cond2.toBuilder())
                }.build()

        assertFalse(SdcEvaluator.isItemEnabled(allItem, mapOf("q1" to "yes")))
        assertTrue(SdcEvaluator.isItemEnabled(allItem, mapOf("q1" to "yes", "q2" to "ok")))

        val anyItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "any" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    enableBehavior = Enumeration(value = Questionnaire.EnableWhenBehavior.Any)
                    enableWhen.add(cond.toBuilder())
                    enableWhen.add(cond2.toBuilder())
                }.build()

        assertTrue(SdcEvaluator.isItemEnabled(anyItem, mapOf("q1" to "yes")))
        assertFalse(SdcEvaluator.isItemEnabled(anyItem, mapOf("q1" to "no", "q2" to "no")))

        // Item with null enableBehavior (defaults to Any)
        val defaultBehaviorItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "defaultBehavior" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    enableWhen.add(cond.toBuilder())
                }.build()
        assertTrue(SdcEvaluator.isItemEnabled(defaultBehaviorItem, mapOf("q1" to "yes")))
        assertFalse(SdcEvaluator.isItemEnabled(defaultBehaviorItem, mapOf("q1" to "no")))

        // Item with invalid condition returning failure
        val condInvalid =
            createCondition(
                null,
                Questionnaire.QuestionnaireItemOperator.Exists,
                Questionnaire.Item.EnableWhen.Answer.Boolean(
                    FhirBoolean
                        .Builder()
                        .apply {
                            value =
                                true
                        }.build(),
                ),
            )
        val invalidCondItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "invalidCond" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    enableWhen.add(condInvalid.toBuilder())
                }.build()
        assertFalse(SdcEvaluator.isItemEnabled(invalidCondItem, emptyMap()))

        // Direct isItemEnabled on item without enableWhen
        assertTrue(SdcEvaluator.isItemEnabled(child, emptyMap()))
        // isItemHierarchyEnabled with default empty ancestors on disabled item
        assertFalse(SdcEvaluator.isItemHierarchyEnabled(allItem, answers = emptyMap()))
    }

    @Test
    fun testEvaluateConditionMalformedAndOperators() {
        val answer =
            Questionnaire.Item.EnableWhen.Answer
                .Boolean(FhirBoolean.Builder().apply { value = true }.build())

        // Target question null
        val condNoQuestion = createCondition(null, Questionnaire.QuestionnaireItemOperator.Exists, answer)
        assertTrue(SdcEvaluator.evaluateCondition(condNoQuestion, emptyMap()).isFailure)

        // Operator null
        val condNoOp = createCondition("q1", null, answer)
        assertTrue(SdcEvaluator.evaluateCondition(condNoOp, emptyMap()).isFailure)

        // NotEqualTo
        val condNotEqual = createCondition("q1", Questionnaire.QuestionnaireItemOperator.NotEqualTo, answer)
        assertTrue(SdcEvaluator.evaluateCondition(condNotEqual, mapOf("q1" to false)).getOrDefault(false))
        assertFalse(SdcEvaluator.evaluateCondition(condNotEqual, mapOf("q1" to true)).getOrDefault(true))

        // Relational operators in evaluateCondition
        val ewInt =
            Questionnaire.Item.EnableWhen.Answer
                .Integer(FhirInteger.Builder().apply { value = 10 }.build())
        val condEqual = createCondition("q1", Questionnaire.QuestionnaireItemOperator.EqualTo, ewInt)
        val condGt = createCondition("q1", Questionnaire.QuestionnaireItemOperator.GreaterThan, ewInt)
        val condLt = createCondition("q1", Questionnaire.QuestionnaireItemOperator.LessThan, ewInt)
        val condGte = createCondition("q1", Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo, ewInt)
        val condLte = createCondition("q1", Questionnaire.QuestionnaireItemOperator.LessThanOrEqualTo, ewInt)
        assertTrue(SdcEvaluator.evaluateCondition(condEqual, mapOf("q1" to 10)).getOrDefault(false))
        assertTrue(SdcEvaluator.evaluateCondition(condGt, mapOf("q1" to 15)).getOrDefault(false))
        assertTrue(SdcEvaluator.evaluateCondition(condLt, mapOf("q1" to 5)).getOrDefault(false))
        assertTrue(SdcEvaluator.evaluateCondition(condGte, mapOf("q1" to 10)).getOrDefault(false))
        assertTrue(SdcEvaluator.evaluateCondition(condLte, mapOf("q1" to 10)).getOrDefault(false))
    }

    @Test
    fun testEvaluateExistsAndEqualityVariants() {
        val ewTrue =
            Questionnaire.Item.EnableWhen.Answer
                .Boolean(FhirBoolean.Builder().apply { value = true }.build())
        val ewFalse =
            Questionnaire.Item.EnableWhen.Answer
                .Boolean(FhirBoolean.Builder().apply { value = false }.build())

        assertFalse(SdcEvaluator.evaluateExists(ewTrue, null))
        assertFalse(SdcEvaluator.evaluateExists(ewTrue, "   "))
        assertFalse(SdcEvaluator.evaluateExists(ewTrue, emptyList<String>()))
        assertTrue(SdcEvaluator.evaluateExists(ewTrue, "value"))
        assertTrue(SdcEvaluator.evaluateExists(ewTrue, listOf("value")))
        assertTrue(SdcEvaluator.evaluateExists(ewTrue, 123))

        assertTrue(SdcEvaluator.evaluateExists(ewFalse, null))
        assertFalse(SdcEvaluator.evaluateExists(ewFalse, "value"))

        // Null boolean value in evaluateExists
        val ewBoolNull =
            Questionnaire.Item.EnableWhen.Answer
                .Boolean(FhirBoolean.Builder().apply { value = null }.build())
        assertTrue(SdcEvaluator.evaluateExists(ewBoolNull, "value"))

        // Non-boolean answer fallback in evaluateExists
        val ewStr =
            Questionnaire.Item.EnableWhen.Answer
                .String(FhirString.Builder().apply { value = "target" }.build())
        assertTrue(SdcEvaluator.evaluateExists(ewStr, "value"))
        assertFalse(SdcEvaluator.evaluateNotEqualTo(ewStr, null).getOrDefault(true))
        assertFalse(SdcEvaluator.evaluateNotEqualTo(ewStr, "   ").getOrDefault(true))
        assertFalse(SdcEvaluator.evaluateNotEqualTo(ewStr, emptyList<String>()).getOrDefault(true))
        assertFalse(SdcEvaluator.evaluateNotEqualTo(ewStr, "target").getOrDefault(true))
        assertTrue(SdcEvaluator.evaluateNotEqualTo(ewStr, "other").getOrDefault(false))

        // EqualTo with collection target
        assertTrue(SdcEvaluator.evaluateEqualTo(ewStr, listOf("other", "target")))
        assertFalse(SdcEvaluator.evaluateEqualTo(ewStr, listOf("other1", "other2")))
        assertFalse(SdcEvaluator.evaluateEqualTo(ewStr, null))
    }

    @Test
    fun testTypedMatchesAndComparisons() {
        // Integer
        val ewInt =
            Questionnaire.Item.EnableWhen.Answer
                .Integer(FhirInteger.Builder().apply { value = 10 }.build())
        assertTrue(SdcEvaluator.matchesSingleValue(ewInt, 10))
        assertFalse(SdcEvaluator.matchesSingleValue(ewInt, 20))

        // Decimal
        val ewDec =
            Questionnaire.Item.EnableWhen.Answer
                .Decimal(Decimal.Builder().apply { value = FhirDecimal.fromString("10.5") }.build())
        assertTrue(SdcEvaluator.matchesSingleValue(ewDec, 10.5))
        assertFalse(SdcEvaluator.matchesSingleValue(ewDec, 10.6))

        // Quantity
        val ewQuant =
            Questionnaire.Item.EnableWhen.Answer.Quantity(
                Quantity
                    .Builder()
                    .apply {
                        value = Decimal.Builder().apply { value = FhirDecimal.fromString("50.0") }
                    }.build(),
            )
        assertTrue(SdcEvaluator.matchesSingleValue(ewQuant, 50.0))

        // Date, DateTime, Time
        val ewDate =
            Questionnaire.Item.EnableWhen.Answer
                .Date(Date.Builder().apply { value = FhirDate.fromString("2026-01-01") }.build())
        assertTrue(SdcEvaluator.matchesSingleValue(ewDate, "2026-01-01"))
        assertFalse(SdcEvaluator.matchesSingleValue(ewDate, "2026-01-02"))

        val ewDateTime =
            Questionnaire.Item.EnableWhen.Answer.DateTime(
                DateTime
                    .Builder()
                    .apply {
                        value =
                            FhirDateTime.fromString("2026-01-01T10:00:00Z")
                    }.build(),
            )
        assertTrue(SdcEvaluator.matchesSingleValue(ewDateTime, "2026-01-01T10:00:00Z"))

        val ewTime =
            Questionnaire.Item.EnableWhen.Answer
                .Time(Time.Builder().apply { value = LocalTime(10, 0, 0) }.build())
        assertTrue(SdcEvaluator.matchesSingleValue(ewTime, "10:00"))

        // Coding code vs display
        val ewCoding =
            Questionnaire.Item.EnableWhen.Answer.Coding(
                Coding
                    .Builder()
                    .apply {
                        code = Code.Builder().apply { value = "C1" }
                        display = FhirString.Builder().apply { value = "Display1" }
                    }.build(),
            )
        assertTrue(SdcEvaluator.matchesSingleValue(ewCoding, "C1"))
        assertTrue(SdcEvaluator.matchesSingleValue(ewCoding, "Display1"))
        assertFalse(SdcEvaluator.matchesSingleValue(ewCoding, "Other"))

        // Coding with only display or only code
        val ewCodingNoCode =
            Questionnaire.Item.EnableWhen.Answer.Coding(
                Coding
                    .Builder()
                    .apply {
                        display = FhirString.Builder().apply { value = "DisplayOnly" }
                    }.build(),
            )
        assertTrue(SdcEvaluator.matchesSingleValue(ewCodingNoCode, "DisplayOnly"))

        val ewCodingNoDisplay =
            Questionnaire.Item.EnableWhen.Answer.Coding(
                Coding
                    .Builder()
                    .apply {
                        code = Code.Builder().apply { value = "CodeOnly" }
                    }.build(),
            )
        assertTrue(SdcEvaluator.matchesSingleValue(ewCodingNoDisplay, "CodeOnly"))

        // Coding with null code and display values
        val ewCodingWithNullValues =
            Questionnaire.Item.EnableWhen.Answer.Coding(
                Coding
                    .Builder()
                    .apply {
                        code = Code.Builder().apply { value = null }
                        display = FhirString.Builder().apply { value = null }
                    }.build(),
            )
        assertFalse(SdcEvaluator.matchesSingleValue(ewCodingWithNullValues, "NonEmpty"))

        // Reference answer fallback
        val ewRef =
            Questionnaire.Item.EnableWhen.Answer.Reference(
                Reference
                    .Builder()
                    .apply {
                        reference = FhirString.Builder().apply { value = "Patient/123" }
                    }.build(),
            )
        assertFalse(SdcEvaluator.matchesSingleValue(ewRef, "Patient/123"))

        // Null value target
        assertFalse(SdcEvaluator.matchesSingleValue(ewInt, null))

        // Null value wrappers
        val ewIntNull =
            Questionnaire.Item.EnableWhen.Answer
                .Integer(FhirInteger.Builder().apply { value = null }.build())
        assertFalse(SdcEvaluator.matchesSingleValue(ewIntNull, 10))

        val ewDecNull =
            Questionnaire.Item.EnableWhen.Answer
                .Decimal(Decimal.Builder().apply { value = null }.build())
        assertFalse(SdcEvaluator.matchesSingleValue(ewDecNull, 10.5))

        val ewQuantNull =
            Questionnaire.Item.EnableWhen.Answer
                .Quantity(Quantity.Builder().apply { value = null }.build())
        assertFalse(SdcEvaluator.matchesSingleValue(ewQuantNull, 50.0))

        val ewDateNull =
            Questionnaire.Item.EnableWhen.Answer
                .Date(Date.Builder().apply { value = null }.build())
        assertFalse(SdcEvaluator.matchesSingleValue(ewDateNull, "2026-01-01"))

        val ewDateTimeNull =
            Questionnaire.Item.EnableWhen.Answer
                .DateTime(DateTime.Builder().apply { value = null }.build())
        assertFalse(SdcEvaluator.matchesSingleValue(ewDateTimeNull, "2026-01-01T10:00:00Z"))

        val ewTimeNull =
            Questionnaire.Item.EnableWhen.Answer
                .Time(Time.Builder().apply { value = null }.build())
        assertFalse(SdcEvaluator.matchesSingleValue(ewTimeNull, "10:00"))

        val ewBoolNull =
            Questionnaire.Item.EnableWhen.Answer
                .Boolean(FhirBoolean.Builder().apply { value = null }.build())
        assertFalse(SdcEvaluator.matchesSingleValue(ewBoolNull, true))

        // Relational comparisons
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.GreaterThan, ewInt, null))
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.GreaterThan, ewInt, "not_a_num"))
        assertTrue(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.GreaterThan, ewInt, 15))
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.GreaterThan, ewInt, 5))

        assertTrue(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.LessThan, ewInt, 5))
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.LessThan, ewInt, 15))

        assertTrue(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo, ewInt, 10))
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo, ewInt, 9))

        assertTrue(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.LessThanOrEqualTo, ewInt, 10))
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.LessThanOrEqualTo, ewInt, 11))

        // Date comparisons
        assertTrue(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.GreaterThan, ewDate, "2026-01-02"))
        assertTrue(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.LessThan, ewDate, "2025-12-31"))
        assertTrue(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo, ewDate, "2026-01-01"))
        assertTrue(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.LessThanOrEqualTo, ewDate, "2026-01-01"))

        // Boolean enableWhen answer match variants
        val ewBool =
            Questionnaire.Item.EnableWhen.Answer
                .Boolean(FhirBoolean.Builder().apply { value = true }.build())
        assertTrue(SdcEvaluator.matchesSingleValue(ewBool, true))
        assertTrue(SdcEvaluator.matchesSingleValue(ewBool, "true"))
        assertFalse(SdcEvaluator.matchesSingleValue(ewBool, false))
        assertFalse(SdcEvaluator.matchesSingleValue(ewBool, "false"))
        assertFalse(SdcEvaluator.matchesSingleValue(ewBool, "notabool"))
        assertFalse(SdcEvaluator.matchesSingleValue(ewBool, 123))

        // Unsupported operator fallback
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.EqualTo, ewInt, 10))
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.EqualTo, ewDate, "2026-01-01"))
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.GreaterThan, ewBool, true))

        // Date comparison false branches
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.GreaterThan, ewDate, "2026-01-01"))
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.LessThan, ewDate, "2026-01-02"))
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo, ewDate, "2025-12-31"))
        assertFalse(SdcEvaluator.evaluateComparison(Questionnaire.QuestionnaireItemOperator.LessThanOrEqualTo, ewDate, "2026-01-02"))

        // extractNumericValue
        assertEquals(42.0, SdcEvaluator.extractNumericValue(BigDecimal.fromInt(42)))
        assertEquals(12.34, SdcEvaluator.extractNumericValue(FhirDecimal.fromString("12.34")))
        assertEquals(99.0, SdcEvaluator.extractNumericValue("99"))
        assertNull(SdcEvaluator.extractNumericValue("invalid"))
        assertNull(SdcEvaluator.extractNumericValue(Any()))

        // evaluateDateOffset
        assertEquals("2026-05-15", SdcEvaluator.evaluateDateOffset("2026-05-10", 5))
        assertEquals("2026-05-08", SdcEvaluator.evaluateDateOffset("2026-05-10T12:00:00Z", -2))
    }
}
