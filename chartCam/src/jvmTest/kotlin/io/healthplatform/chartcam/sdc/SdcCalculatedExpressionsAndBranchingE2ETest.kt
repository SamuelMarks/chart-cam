/**
 * @file SdcCalculatedExpressionsAndBranchingE2ETest.kt
 * Contains declarations for SdcCalculatedExpressionsAndBranchingE2ETest.kt.
 *
 * Validates SDC dynamic calculated expressions (e.g. BMI calculations)
 * and complex enableWhen branching rules end-to-end.
 */
package io.healthplatform.chartcam.sdc

import com.google.fhir.model.r4.Decimal
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Extension
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.PublicationStatus
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.healthplatform.chartcam.validation.FhirValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.google.fhir.model.r4.String as FhirString

/**
 * End-to-End workflow tests validating mathematical SDC expressions,
 * cascading calculated fields, and conditional branching based on computed metrics.
 */
class SdcCalculatedExpressionsAndBranchingE2ETest {
    /**
     * Builds a clinical questionnaire with weight, height, calculated BMI,
     * and conditional follow-up groups guarded by enableWhen predicates.
     *
     * @return Fully configured FHIR [Questionnaire].
     */
    private fun buildBmiClinicalQuestionnaire(): Questionnaire {
        val calcExt =
            Extension.Builder(url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression").apply {
                extension.add(
                    Extension.Builder(url = "expression").apply {
                        value =
                            Extension.Value.String(
                                FhirString.Builder().apply { value = "%weight / (%height * %height)" }.build(),
                            )
                    },
                )
            }

        val weightItemBuilder =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "weight" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                ).apply {
                    text = FhirString.Builder().apply { value = "Weight (kg)" }
                }

        val heightItemBuilder =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "height" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                ).apply {
                    text = FhirString.Builder().apply { value = "Height (m)" }
                }

        val bmiItemBuilder =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "bmi" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                ).apply {
                    text = FhirString.Builder().apply { value = "Body Mass Index" }
                    extension.add(calcExt)
                }

        val followUpEnableWhenBuilder =
            Questionnaire.Item.EnableWhen
                .Builder(
                    question = FhirString.Builder().apply { value = "bmi" },
                    operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo),
                    answer =
                        Questionnaire.Item.EnableWhen.Answer.Decimal(
                            Decimal
                                .Builder()
                                .apply {
                                    value = BigDecimal.parseString("25.0")
                                }.build(),
                        ),
                )

        val followUpGroupBuilder =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "high_bmi_group" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).apply {
                    text = FhirString.Builder().apply { value = "High BMI Clinical Follow-Up" }
                    enableWhen.add(followUpEnableWhenBuilder)
                }

        return Questionnaire
            .Builder(status = Enumeration(value = PublicationStatus.Active))
            .apply {
                title = FhirString.Builder().apply { value = "Metabolic & BMI Assessment Form" }
                item.add(weightItemBuilder)
                item.add(heightItemBuilder)
                item.add(bmiItemBuilder)
                item.add(followUpGroupBuilder)
            }.build()
    }

    /**
     * Validates that entering weight = 80kg and height = 1.6m calculates BMI = 31.25,
     * which in turn activates the High BMI Follow-Up group.
     */
    @Test
    fun testBmiCalculationEnablesFollowUpGroup() {
        val questionnaire = buildBmiClinicalQuestionnaire()
        assertTrue(FhirValidator.validate(questionnaire).isSuccess, "Questionnaire must pass structural validation")

        val answers =
            mutableMapOf<String, Any>(
                "weight" to 80.0f,
                "height" to 1.6f,
            )

        val computedAnswers = SdcEvaluator.evaluateCalculatedExpressions(questionnaire, answers)
        val computedBmi = computedAnswers["bmi"] as? Float
        assertTrue(computedBmi != null, "BMI should be computed from height and weight")
        assertEquals(31.25f, computedBmi, 0.01f, "BMI must accurately equal 31.25")

        val followUpGroup = questionnaire.item.first { it.linkId.value == "high_bmi_group" }
        val isGroupEnabled = SdcEvaluator.isItemEnabled(followUpGroup, computedAnswers)
        assertTrue(isGroupEnabled, "High BMI group must be enabled when BMI >= 25.0")
    }

    /**
     * Validates that updating weight to 55kg (BMI = 21.48) causes the follow-up group
     * to become hidden.
     */
    @Test
    fun testBmiRecalculationHidesFollowUpGroup() {
        val questionnaire = buildBmiClinicalQuestionnaire()

        val answers =
            mutableMapOf<String, Any>(
                "weight" to 55.0f,
                "height" to 1.6f,
            )

        val computedAnswers = SdcEvaluator.evaluateCalculatedExpressions(questionnaire, answers)
        val computedBmi = computedAnswers["bmi"] as? Float
        assertTrue(computedBmi != null, "BMI should be computed")
        assertEquals(21.48f, computedBmi, 0.05f, "BMI must accurately equal ~21.48")

        val followUpGroup = questionnaire.item.first { it.linkId.value == "high_bmi_group" }
        val isGroupEnabled = SdcEvaluator.isItemEnabled(followUpGroup, computedAnswers)
        assertFalse(isGroupEnabled, "High BMI group must be disabled/hidden when BMI < 25.0")
    }
}
