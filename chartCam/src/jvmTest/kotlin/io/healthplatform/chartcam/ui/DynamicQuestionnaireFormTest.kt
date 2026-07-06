package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Extension
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.String
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DynamicQuestionnaireFormTest {
    @Test
    fun testIsItemHidden() {
        val itemHidden =
            Questionnaire.Item
                .Builder()
                .apply {
                    linkId = String.Builder().setValue("1").build()
                    extension.add(
                        Extension
                            .Builder()
                            .apply {
                                url =
                                    com.google.fhir.model.r4.Uri
                                        .Builder()
                                        .setValue("http://hl7.org/fhir/StructureDefinition/questionnaire-hidden")
                                        .build()
                                value =
                                    Extension.ValueX
                                        .Builder()
                                        .setBoolean(Boolean.Builder().setValue(true).build())
                                        .build()
                            }.build(),
                    )
                }.build()

        assertTrue(isItemHidden(itemHidden))

        val itemNotHidden =
            Questionnaire.Item
                .Builder()
                .apply {
                    linkId = String.Builder().setValue("2").build()
                }.build()

        assertFalse(isItemHidden(itemNotHidden))
    }

    @Test
    fun testIsItemEnabled() {
        val item =
            Questionnaire.Item
                .Builder()
                .apply {
                    linkId = String.Builder().setValue("target").build()
                    enableWhen.add(
                        Questionnaire.Item.EnableWhen
                            .Builder()
                            .apply {
                                question = String.Builder().setValue("q1").build()
                                operator =
                                    Questionnaire.QuestionnaireItemOperator
                                        .Builder()
                                        .setValue(
                                            Questionnaire.QuestionnaireItemOperator.Value.EQUAL_TO,
                                        ).build()
                                answer =
                                    Questionnaire.Item.EnableWhen.AnswerX
                                        .Builder()
                                        .setString(String.Builder().setValue("yes").build())
                                        .build()
                            }.build(),
                    )
                }.build()

        // Missing answer
        assertFalse(isItemEnabled(item, emptyMap()))

        // Wrong answer
        assertFalse(isItemEnabled(item, mapOf("q1" to "no")))

        // Correct answer
        assertTrue(isItemEnabled(item, mapOf("q1" to "yes")))
    }

    @Test
    fun testDynamicQuestionnaireForm() =
        runComposeUiTest {
            val q =
                Questionnaire
                    .Builder()
                    .apply {
                        item.add(
                            Questionnaire.Item
                                .Builder()
                                .apply {
                                    linkId = String.Builder().setValue("q1").build()
                                    type =
                                        Questionnaire.QuestionnaireItemType
                                            .Builder()
                                            .setValue(Questionnaire.QuestionnaireItemType.Value.STRING)
                                            .build()
                                    text = String.Builder().setValue("Question 1").build()
                                }.build(),
                        )
                        item.add(
                            Questionnaire.Item
                                .Builder()
                                .apply {
                                    linkId = String.Builder().setValue("q2").build()
                                    type =
                                        Questionnaire.QuestionnaireItemType
                                            .Builder()
                                            .setValue(Questionnaire.QuestionnaireItemType.Value.BOOLEAN)
                                            .build()
                                    text = String.Builder().setValue("Question 2").build()
                                }.build(),
                        )
                    }.build()

            setContent {
                DynamicQuestionnaireForm(
                    questionnaire = q,
                    answers = mapOf("q1" to "test"),
                    onAnswerChanged = { _, _ -> },
                )
            }

            onRoot().assertExists()
        }
}
