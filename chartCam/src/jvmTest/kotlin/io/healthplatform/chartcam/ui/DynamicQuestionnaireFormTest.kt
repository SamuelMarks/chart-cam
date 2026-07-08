package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.String
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DynamicQuestionnaireFormTest {
    @Test
    fun testRendersDifferentWidgets() =
        runComposeUiTest {
            val fhirItemBuilders =
                listOf(
                    Questionnaire.Item
                        .Builder(
                            String.Builder().apply { value = "item1" },
                            Enumeration(value = Questionnaire.QuestionnaireItemType.Text),
                        ).apply {
                            text = String.Builder().apply { value = "Multi-line Text Item" }
                            required = Boolean.Builder().apply { value = false }
                        },
                    Questionnaire.Item
                        .Builder(
                            String.Builder().apply { value = "item2" },
                            Enumeration(value = Questionnaire.QuestionnaireItemType.Date),
                        ).apply {
                            text = String.Builder().apply { value = "Date Item" }
                            required = Boolean.Builder().apply { value = false }
                        },
                    Questionnaire.Item
                        .Builder(
                            String.Builder().apply { value = "item3" },
                            Enumeration(value = Questionnaire.QuestionnaireItemType.DateTime),
                        ).apply {
                            text = String.Builder().apply { value = "Date Time Item" }
                            required = Boolean.Builder().apply { value = false }
                        },
                    Questionnaire.Item
                        .Builder(
                            String.Builder().apply { value = "item4" },
                            Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                        ).apply {
                            text = String.Builder().apply { value = "Decimal Item" }
                            required = Boolean.Builder().apply { value = false }
                        },
                    Questionnaire.Item
                        .Builder(
                            String.Builder().apply { value = "item5" },
                            Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                        ).apply {
                            text = String.Builder().apply { value = "Integer Item" }
                            required = Boolean.Builder().apply { value = false }
                        },
                    Questionnaire.Item
                        .Builder(
                            String.Builder().apply { value = "item6" },
                            Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                        ).apply {
                            text = String.Builder().apply { value = "Multi-select Item" }
                            required = Boolean.Builder().apply { value = false }
                            repeats = Boolean.Builder().apply { value = true }
                            answerOption.add(
                                Questionnaire.Item.AnswerOption.Builder(
                                    Questionnaire.Item.AnswerOption.Value.String(
                                        String.Builder().apply { value = "Opt1" }.build(),
                                    ),
                                ),
                            )
                        },
                )

            val questionnaire =
                Questionnaire
                    .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = "test-q"
                        title = String.Builder().apply { value = "Test Q" }
                        item.addAll(fhirItemBuilders)
                    }.build()

            val answers = mutableMapOf<kotlin.String, Any>()

            setContent {
                DynamicQuestionnaireForm(
                    questionnaire = questionnaire,
                    answers = answers,
                    onAnswerChanged = { linkId, value ->
                        if (value != null) {
                            answers[linkId] = value
                        } else {
                            answers.remove(linkId)
                        }
                    },
                )
            }

            onNodeWithText("Multi-line Text Item").assertIsDisplayed()
            onNodeWithText("Decimal Item").assertIsDisplayed()
            onNodeWithText("Integer Item").assertIsDisplayed()
            onNodeWithText("Multi-select Item").assertIsDisplayed()
        }

    @Test
    fun testIsItemHidden() {
        val item =
            Questionnaire.Item
                .Builder(
                    String.Builder().apply { value = "hidden_item" },
                    Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(
                        com.google.fhir.model.r4.Extension
                            .Builder(
                                url = "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden",
                            ).apply {
                                value =
                                    com.google.fhir.model.r4.Extension.Value.Boolean(
                                        Boolean.Builder().apply { value = true }.build(),
                                    )
                            },
                    )
                }.build()

        kotlin.test.assertTrue(isItemHidden(item))
    }

    @Test
    fun testIsItemEnabled_Exists() {
        val item =
            Questionnaire.Item
                .Builder(
                    String.Builder().apply { value = "dependent_item" },
                    Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    enableWhen.add(
                        Questionnaire.Item.EnableWhen.Builder(
                            String.Builder().apply { value = "trigger_item" },
                            Enumeration(value = Questionnaire.QuestionnaireItemOperator.Exists),
                            Questionnaire.Item.EnableWhen.Answer.Boolean(
                                Boolean.Builder().apply { value = true }.build(),
                            ),
                        ),
                    )
                }.build()

        kotlin.test.assertFalse(isItemEnabled(item, mapOf()))
        kotlin.test.assertTrue(isItemEnabled(item, mapOf("trigger_item" to "some_value")))
    }

    @Test
    fun testIsItemEnabled_EqualTo() {
        val item =
            Questionnaire.Item
                .Builder(
                    String.Builder().apply { value = "dependent_item" },
                    Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    enableBehavior = Enumeration(value = Questionnaire.EnableWhenBehavior.All)
                    enableWhen.add(
                        Questionnaire.Item.EnableWhen.Builder(
                            String.Builder().apply { value = "trigger_item" },
                            Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo),
                            Questionnaire.Item.EnableWhen.Answer.String(
                                String.Builder().apply { value = "Yes" }.build(),
                            ),
                        ),
                    )
                }.build()

        kotlin.test.assertFalse(isItemEnabled(item, mapOf("trigger_item" to "No")))
        kotlin.test.assertTrue(isItemEnabled(item, mapOf("trigger_item" to "Yes")))
    }

    @Test
    fun testIsItemEnabled_NotEqualTo() {
        val item =
            Questionnaire.Item
                .Builder(
                    String.Builder().apply { value = "dependent_item" },
                    Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    enableBehavior = Enumeration(value = Questionnaire.EnableWhenBehavior.Any)
                    enableWhen.add(
                        Questionnaire.Item.EnableWhen.Builder(
                            String.Builder().apply { value = "trigger_item" },
                            Enumeration(value = Questionnaire.QuestionnaireItemOperator.NotEqualTo),
                            Questionnaire.Item.EnableWhen.Answer.String(
                                String.Builder().apply { value = "No" }.build(),
                            ),
                        ),
                    )
                }.build()

        kotlin.test.assertFalse(isItemEnabled(item, mapOf("trigger_item" to "No")))
        kotlin.test.assertTrue(isItemEnabled(item, mapOf("trigger_item" to "Yes")))
    }
}
