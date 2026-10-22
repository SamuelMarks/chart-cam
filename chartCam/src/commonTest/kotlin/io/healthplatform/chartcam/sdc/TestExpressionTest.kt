/**
 * @file TestExpressionTest.kt
 * Contains unit tests for SDC calculated expression evaluation and answer preservation.
 */
package io.healthplatform.chartcam.sdc

import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Extension
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import dev.ohs.fhir.model.r4.terminologies.QuestionnaireItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Common tests for SDC calculated expressions and non-calculated answer preservation.
 */
class TestExpressionTest {
    /**
     * Verifies that non-calculated answers are preserved intact while calculated expressions
     * dynamically compute derived item answers.
     */
    @Test
    fun testExp() {
        val calcItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "sum" },
                    type = Enumeration(value = QuestionnaireItemType.Integer),
                ).apply {
                    extension.add(
                        Extension
                            .Builder(
                                url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression",
                            ).apply {
                                extension.add(
                                    Extension.Builder(url = "expression").apply {
                                        value =
                                            Extension.Value.String(
                                                FhirString
                                                    .Builder()
                                                    .apply { value = "%val1 + %val2" }
                                                    .build(),
                                            )
                                    },
                                )
                            },
                    )
                }

        val q =
            Questionnaire
                .Builder(Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = "test-q"
                    item.add(calcItem)
                }.build()
        val answers = mapOf("val1" to 10, "val2" to 20)
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(10, result["val1"])
        assertEquals(20, result["val2"])
        assertEquals(30f, result["sum"])
    }
}
