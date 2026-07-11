package io.healthplatform.chartcam.sdc

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.String
import io.healthplatform.chartcam.fhir.isHidden
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SdcQuestionnaireFormTest {
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
                            String.Builder().apply { value = "item7" },
                            Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ).apply {
                            text = String.Builder().apply { value = "String Item" }
                        },
                    Questionnaire.Item
                        .Builder(
                            String.Builder().apply { value = "item8" },
                            Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                        ).apply {
                            text = String.Builder().apply { value = "Boolean Item" }
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

            var answers: Map<kotlin.String, Any> = mapOf()

            setContent {
                SdcQuestionnaireForm(
                    questionnaire = questionnaire,
                    answers = answers,
                    onFormUpdated = { updated, _ -> answers = updated },
                )
            }

            onNodeWithTag("TextArea Multi-line Text Item").assertIsDisplayed()
            onNodeWithTag("TextInput String Item").assertIsDisplayed()
            onNodeWithTag("CheckboxRow Boolean Item").assertIsDisplayed()

            // Trigger state changes
            onNodeWithTag("TextArea Multi-line Text Item").performTextInput("test text")
            assertEquals("test text", answers["item1"])

            onAllNodes(
                androidx.compose.ui.test
                    .hasText("Boolean Item", substring = true),
            ).onFirst().performClick()
            assertEquals(true, answers["item8"])
        }

    @Test
    fun testRendersNestedGroupItems() =
        runComposeUiTest {
            val nestedItemBuilder =
                Questionnaire.Item
                    .Builder(
                        String.Builder().apply { value = "nestedItem1" },
                        Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                    ).apply {
                        text = String.Builder().apply { value = "Nested Text Item" }
                    }

            val groupBuilder =
                Questionnaire.Item
                    .Builder(
                        String.Builder().apply { value = "group1" },
                        Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                    ).apply {
                        text = String.Builder().apply { value = "Group Item Header" }
                        item.add(nestedItemBuilder)
                    }

            val questionnaire =
                Questionnaire
                    .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = "test-q-nested"
                        title = String.Builder().apply { value = "Test Q Nested" }
                        item.add(groupBuilder)
                    }.build()

            var answers: Map<kotlin.String, Any> = mapOf()

            setContent {
                SdcQuestionnaireForm(
                    questionnaire = questionnaire,
                    answers = answers,
                    onFormUpdated = { updated, _ -> answers = updated },
                )
            }

            onNodeWithText("Group Item Header").assertIsDisplayed()
            onNodeWithTag("TextInput Nested Text Item").assertIsDisplayed()

            onNodeWithTag("TextInput Nested Text Item").performTextInput("nested value")
            assertEquals("nested value", answers["nestedItem1"])
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

        assertTrue(item.isHidden())
    }
}
