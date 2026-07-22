/**
 * @file SdcEvaluatorTest.kt
 * Contains declarations for SdcEvaluatorTest.kt.
 */
package io.healthplatform.chartcam.sdc

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Extension
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.google.fhir.model.r4.String as FhirString

class SdcEvaluatorTest {
    private fun createQuestionnaireWithCalcExt(
        linkIdStr: String,
        expression: String,
    ): Questionnaire {
        val calcExt =
            Extension.Builder(url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression").apply {
                extension.add(
                    Extension.Builder(url = "expression").apply {
                        value = Extension.Value.String(FhirString.Builder().apply { value = expression }.build())
                    },
                )
            }

        val itemB =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = linkIdStr },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(calcExt)
                }

        return Questionnaire.Builder(status = Enumeration(value = PublicationStatus.Active)).apply { item.add(itemB) }.build()
    }

    private fun createQuestionnaireWithAltCalcExt(
        linkIdStr: String,
        expression: String,
    ): Questionnaire {
        val calcExt =
            Extension.Builder(url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression").apply {
                value = Extension.Value.String(FhirString.Builder().apply { value = expression }.build())
            }

        val itemB =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = linkIdStr },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(calcExt)
                }

        return Questionnaire.Builder(status = Enumeration(value = PublicationStatus.Active)).apply { item.add(itemB) }.build()
    }

    @Test
    fun testEvaluateMathExpressionBasic() {
        val q = createQuestionnaireWithCalcExt("target", "%a + %b")
        val answers = mutableMapOf<String, Any>("a" to 5f, "b" to 10f)
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(15f, result["target"])
    }

    @Test
    fun testEvaluateMathExpressionAdvanced() {
        val q = createQuestionnaireWithCalcExt("target", "(%a + %b) * %c / 2")
        val answers = mutableMapOf<String, Any>("a" to 5f, "b" to 10f, "c" to "4")
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(30f, result["target"])
    }

    @Test
    fun testEvaluateMathExpressionWithAltExt() {
        val q = createQuestionnaireWithAltCalcExt("target", "%a - %b")
        val answers = mutableMapOf<String, Any>("a" to 15f, "b" to 5f)
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(10f, result["target"])
    }

    @Test
    fun testMissingVarsDefaultToZero() {
        val q = createQuestionnaireWithCalcExt("target", "%a + %missing")
        val answers = mutableMapOf<String, Any>("a" to 5f)
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(5f, result["target"])
    }

    @Test
    fun testCascadingUpdates() {
        val itemB =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "B" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(
                        Extension
                            .Builder(
                                url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression",
                            ).apply {
                                extension.add(
                                    Extension.Builder(url = "expression").apply {
                                        value = Extension.Value.String(FhirString.Builder().apply { value = "%A + 5" }.build())
                                    },
                                )
                            },
                    )
                }

        val itemC =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "C" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(
                        Extension
                            .Builder(
                                url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression",
                            ).apply {
                                extension.add(
                                    Extension.Builder(url = "expression").apply {
                                        value = Extension.Value.String(FhirString.Builder().apply { value = "%B * 2" }.build())
                                    },
                                )
                            },
                    )
                }

        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    item.add(itemB)
                    item.add(itemC)
                }.build()
        val answers = mutableMapOf<String, Any>("A" to 10f)

        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(15f, result["B"])
        assertEquals(30f, result["C"])
    }

    @Test
    fun testMalformedMath() {
        val q = createQuestionnaireWithCalcExt("target", "1 + * 2")
        val answers = mutableMapOf<String, Any>()
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertTrue(!result.containsKey("target"))
    }

    @Test
    fun testEmptyExpression() {
        val q = createQuestionnaireWithCalcExt("target", "   ")
        val answers = mutableMapOf<String, Any>()
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(0f, result["target"])
    }

    @Test
    fun testNestedItem() {
        val calcExt =
            Extension.Builder(url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression").apply {
                extension.add(
                    Extension.Builder(url = "expression").apply {
                        value = Extension.Value.String(FhirString.Builder().apply { value = "%parent_val + 5" }.build())
                    },
                )
            }

        val nestedItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "nested" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(calcExt)
                }

        val parentItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "parent" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).apply {
                    item.add(nestedItem)
                }

        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    item.add(parentItem)
                }.build()

        val answers = mutableMapOf<String, Any>("parent_val" to 10f)
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(15f, result["nested"])
    }

    @Test
    fun testInvalidVariableType() {
        val q = createQuestionnaireWithCalcExt("target", "%a + 2")
        val answers = mutableMapOf<String, Any>("a" to "abc")
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(2f, result["target"])
    }

    @Test
    fun testSubtractionAndDivision() {
        val q = createQuestionnaireWithCalcExt("target", "10 - 4 / 2")
        val answers = mutableMapOf<String, Any>()
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(8f, result["target"])
    }

    @Test
    fun testMissingLinkId() {
        val calcExt =
            Extension.Builder(url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression").apply {
                extension.add(
                    Extension.Builder(url = "expression").apply {
                        value = Extension.Value.String(FhirString.Builder().apply { value = "5 + 5" }.build())
                    },
                )
            }

        val itemB =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder(), // missing value
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(calcExt)
                }

        val q = Questionnaire.Builder(status = Enumeration(value = PublicationStatus.Active)).apply { item.add(itemB) }.build()
        val answers = mutableMapOf<String, Any>()
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertTrue(result.isEmpty())
    }
}
