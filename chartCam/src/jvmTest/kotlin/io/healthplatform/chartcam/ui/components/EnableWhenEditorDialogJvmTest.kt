/**
 * @file EnableWhenEditorDialogJvmTest.kt
 * Contains declarations for EnableWhenEditorDialogJvmTest.kt.
 *
 * JVM Compose UI tests for the [EnableWhenEditorDialog] component.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.viewmodel.BuilderEnableWhen
import io.healthplatform.chartcam.viewmodel.BuilderItem
import io.healthplatform.chartcam.viewmodel.WidgetType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test suite verifying layout integrity and user interactions in [EnableWhenEditorDialog].
 */
@OptIn(ExperimentalTestApi::class)
class EnableWhenEditorDialogJvmTest {
    /**
     * Verifies that EnableWhenEditorDialog renders conditions, allows adding/updating, and saving.
     */
    @Test
    fun testEnableWhenEditorDialogBasicFlow() =
        runComposeUiTest {
            val candidates =
                listOf(
                    BuilderItem(
                        linkId = "q1",
                        label = "Do you have symptoms?",
                        widgetType = WidgetType.SINGLE_LINE_TEXT,
                    ),
                    BuilderItem(
                        linkId = "q2",
                        label = "Onset date",
                        widgetType = WidgetType.DATE,
                    ),
                )
            val currentItem =
                BuilderItem(
                    linkId = "q3",
                    label = "Symptom details",
                    widgetType = WidgetType.SINGLE_LINE_TEXT,
                    enableWhen =
                        listOf(
                            BuilderEnableWhen(
                                question = "q1",
                                operator = Questionnaire.QuestionnaireItemOperator.EqualTo,
                                answerString = "Yes",
                            ),
                        ),
                    enableBehavior = Questionnaire.EnableWhenBehavior.All,
                )
            var savedConditions: List<BuilderEnableWhen>? = null
            var savedBehavior: Questionnaire.EnableWhenBehavior? = null
            var dismissed = false

            setContent {
                EnableWhenEditorDialog(
                    currentItem = currentItem,
                    candidateQuestions = candidates,
                    onSave = { conds, beh ->
                        savedConditions = conds
                        savedBehavior = beh
                    },
                    onDismiss = { dismissed = true },
                )
            }

            waitForIdle()
            onNodeWithText("Conditional Logic (enableWhen)").assertIsDisplayed()
            onNodeWithText("Add Condition").assertIsDisplayed()

            // Confirm dialog
            onNodeWithText("Confirm").performClick()
            waitForIdle()

            assertEquals(1, savedConditions?.size)
            assertEquals(Questionnaire.EnableWhenBehavior.All, savedBehavior)
        }

    /**
     * Verifies layout integrity across scaled density (200% font scale) settings.
     */
    @Test
    fun testEnableWhenEditorDialogScaledDensity() =
        runComposeUiTest {
            val candidates =
                listOf(
                    BuilderItem(
                        linkId = "q1",
                        label = "Prior question",
                        widgetType = WidgetType.SINGLE_LINE_TEXT,
                    ),
                )
            val currentItem =
                BuilderItem(
                    linkId = "q2",
                    label = "Target question",
                    widgetType = WidgetType.SINGLE_LINE_TEXT,
                )
            var dismissed = false

            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = 2f, fontScale = 2.0f),
                ) {
                    EnableWhenEditorDialog(
                        currentItem = currentItem,
                        candidateQuestions = candidates,
                        onSave = { _, _ -> },
                        onDismiss = { dismissed = true },
                    )
                }
            }

            waitForIdle()
            onNodeWithText("Conditional Logic (enableWhen)").assertIsDisplayed()
            onNodeWithText("Cancel").performClick()
            waitForIdle()
            assertTrue(dismissed)
        }

    /**
     * Tests behavior toggle, adding conditions, dropdown selection, answer input, and deletion.
     */
    @Test
    fun testEnableWhenEditorDialogInteractionsAndBranches() =
        runComposeUiTest {
            val candidates =
                listOf(
                    BuilderItem(
                        linkId = "q_numeric",
                        label = "Patient Age",
                        widgetType = WidgetType.NUMERIC,
                    ),
                    BuilderItem(
                        linkId = "q_text",
                        label = "Symptoms",
                        widgetType = WidgetType.SINGLE_LINE_TEXT,
                    ),
                )
            val currentItem =
                BuilderItem(
                    linkId = "q_target",
                    label = "Followup Question",
                    widgetType = WidgetType.SINGLE_LINE_TEXT,
                    enableWhen =
                        listOf(
                            BuilderEnableWhen(
                                question = "q_numeric",
                                operator = Questionnaire.QuestionnaireItemOperator.EqualTo,
                                answerString = "25",
                            ),
                        ),
                    enableBehavior = Questionnaire.EnableWhenBehavior.All,
                )
            var savedConditions: List<BuilderEnableWhen>? = null
            var savedBehavior: Questionnaire.EnableWhenBehavior? = null

            setContent {
                EnableWhenEditorDialog(
                    currentItem = currentItem,
                    candidateQuestions = candidates,
                    onSave = { conds, beh ->
                        savedConditions = conds
                        savedBehavior = beh
                    },
                    onDismiss = {},
                )
            }

            waitForIdle()

            // 1. Toggle behavior from All to Any, then back to All
            onNodeWithText("Any", substring = true, useUnmergedTree = true).performClick()
            waitForIdle()
            onNodeWithText("All", substring = true, useUnmergedTree = true).performClick()
            waitForIdle()

            // 2. Add Condition
            onNodeWithText("Add Condition", useUnmergedTree = true).performClick()
            waitForIdle()

            // Update expected answer on second condition (index 1)
            onAllNodes(
                androidx.compose.ui.test
                    .hasSetTextAction(),
            )[1].performTextInput("Second")
            waitForIdle()

            // Update expected answer on first condition (index 0)
            onAllNodes(
                androidx.compose.ui.test
                    .hasSetTextAction(),
            )[0].performTextInput("First")
            waitForIdle()

            // 3. Delete the first condition
            onAllNodesWithContentDescription("Delete", substring = true, useUnmergedTree = true)[0].performClick()
            waitForIdle()

            // 4. Update expected answer text on remaining condition
            onNode(
                androidx.compose.ui.test
                    .hasSetTextAction(),
            ).performTextInput("Updated")
            waitForIdle()

            // 5. Expand operator dropdown and select Exists
            onNodeWithText("Equals", substring = true, useUnmergedTree = true).performClick()
            waitForIdle()
            onNodeWithText("Is answered", substring = true, useUnmergedTree = true).performClick()
            waitForIdle()

            // 6. Expand question dropdown and select Symptoms
            onNodeWithText("Patient Age", substring = true, useUnmergedTree = true).performClick()
            waitForIdle()
            onNodeWithText("Symptoms", substring = true, useUnmergedTree = true).performClick()
            waitForIdle()

            // 7. Save
            onNodeWithText("Confirm", useUnmergedTree = true).performClick()
            waitForIdle()

            assertEquals(1, savedConditions?.size)
            assertEquals("q_text", savedConditions?.first()?.question)
            assertEquals(Questionnaire.QuestionnaireItemOperator.Exists, savedConditions?.first()?.operator)
        }

    /**
     * Tests empty candidate questions, empty conditions, null answerString, and unknown question/operator fallbacks.
     */
    @Test
    fun testEnableWhenEditorDialogEmptyAndFallbacks() =
        runComposeUiTest {
            val currentItem =
                BuilderItem(
                    linkId = "q_target",
                    label = "Target",
                    widgetType = WidgetType.SINGLE_LINE_TEXT,
                    enableWhen =
                        listOf(
                            BuilderEnableWhen(
                                question = "unknown_link_id",
                                operator = Questionnaire.QuestionnaireItemOperator.GreaterThan,
                                answerString = null,
                            ),
                        ),
                    enableBehavior = Questionnaire.EnableWhenBehavior.Any,
                )

            setContent {
                EnableWhenEditorDialog(
                    currentItem = currentItem,
                    candidateQuestions = emptyList(),
                    onSave = { _, _ -> },
                    onDismiss = {},
                )
            }

            waitForIdle()
            // Unknown question linkId fallback displayed
            onNodeWithText("unknown_link_id", substring = true, useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Tests recomposition and skipping for [EnableWhenEditorDialog].
     */
    @Test
    fun testEnableWhenEditorDialogRecompositionAndSkipping() =
        runComposeUiTest {
            val candidates1 =
                listOf(
                    BuilderItem(
                        linkId = "q1",
                        label = "Question 1",
                        widgetType = WidgetType.SINGLE_LINE_TEXT,
                    ),
                )
            val candidates2 =
                listOf(
                    BuilderItem(
                        linkId = "q2",
                        label = "Question 2",
                        widgetType = WidgetType.RANGE,
                    ),
                )
            val item1 =
                BuilderItem(
                    linkId = "t1",
                    label = "Target 1",
                    widgetType = WidgetType.SINGLE_LINE_TEXT,
                )
            val item2 =
                BuilderItem(
                    linkId = "t2",
                    label = "Target 2",
                    widgetType = WidgetType.NUMERIC,
                )

            val outerTrigger = androidx.compose.runtime.mutableStateOf(0)
            val itemState = androidx.compose.runtime.mutableStateOf(item1)
            val candidatesState = androidx.compose.runtime.mutableStateOf(candidates1)
            val onDismissState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val onSaveState =
                androidx.compose.runtime.mutableStateOf<
                    (List<BuilderEnableWhen>, Questionnaire.EnableWhenBehavior) -> Unit,
                >({ _, _ -> })

            setContent {
                val dummy = outerTrigger.value
                EnableWhenEditorDialog(
                    currentItem = itemState.value,
                    candidateQuestions = candidatesState.value,
                    onDismiss = onDismissState.value,
                    onSave = onSaveState.value,
                )
            }
            waitForIdle()

            // Skipping
            outerTrigger.value++
            waitForIdle()

            // Mutate parameters
            itemState.value = item2
            waitForIdle()

            candidatesState.value = candidates2
            waitForIdle()

            onDismissState.value = { println("dismissed") }
            waitForIdle()

            onSaveState.value = { _, _ -> println("saved") }
            waitForIdle()
        }

    /**
     * Directly tests [ConditionRow] with keyboard type selection, answer updating, and recomposition.
     */
    @Test
    fun testConditionRowDirect() =
        runComposeUiTest {
            val candidates1 =
                listOf(
                    BuilderItem(
                        linkId = "q_range",
                        label = "Pain Level",
                        widgetType = WidgetType.RANGE,
                    ),
                    BuilderItem(
                        linkId = "q_other",
                        label = "Other Question",
                        widgetType = WidgetType.SINGLE_SELECT,
                    ),
                )
            val candidates2 =
                listOf(
                    BuilderItem(
                        linkId = "q_range",
                        label = "Pain Level (Updated)",
                        widgetType = WidgetType.RANGE,
                    ),
                    BuilderItem(
                        linkId = "q_other",
                        label = "Other Question",
                        widgetType = WidgetType.SINGLE_SELECT,
                    ),
                )
            val candidatesState = androidx.compose.runtime.mutableStateOf(candidates1)
            val condState =
                androidx.compose.runtime.mutableStateOf(
                    BuilderEnableWhen(
                        question = "q_range",
                        operator = Questionnaire.QuestionnaireItemOperator.GreaterThan,
                        answerString = "5",
                    ),
                )
            val outerTrigger = androidx.compose.runtime.mutableStateOf(0)
            val onUpdateState =
                androidx.compose.runtime.mutableStateOf<(BuilderEnableWhen) -> Unit>({ condState.value = it })
            val onAnswerState =
                androidx.compose.runtime.mutableStateOf<(String) -> Unit>({ newAns ->
                    condState.value = updateConditionAnswer(condState.value, newAns)
                })
            var deleted = false

            setContent {
                val dummy = outerTrigger.value
                ConditionRow(
                    condition = condState.value,
                    candidateQuestions = candidatesState.value,
                    onUpdate = onUpdateState.value,
                    onDelete = { deleted = true },
                    onAnswerChange = onAnswerState.value,
                )
            }
            waitForIdle()

            // Test skipping
            outerTrigger.value++
            waitForIdle()

            // Recompose ConditionRow with unchanged condition by mutating candidateQuestions
            candidatesState.value = candidates2
            waitForIdle()

            // Recompose ConditionRow with unchanged condition by mutating onUpdate
            onUpdateState.value = { updated ->
                condState.value = updated
            }
            waitForIdle()

            // Recompose ConditionRow with unchanged condition by mutating onAnswerChange
            onAnswerState.value = { newAns ->
                condState.value = updateConditionAnswer(condState.value, newAns)
            }
            waitForIdle()

            // Mutate answerString to "10"
            condState.value = condState.value.copy(answerString = "10")
            waitForIdle()

            // Trigger onValueChange directly on ConditionRow
            onNode(
                androidx.compose.ui.test
                    .hasSetTextAction(),
            ).performTextInput("99")
            waitForIdle()

            // Update to WidgetType.SINGLE_SELECT and null answerString to test both branches
            condState.value = condState.value.copy(question = "q_other", answerString = null)
            waitForIdle()

            // Switch operator to Exists so the Expected Answer text field is hidden
            condState.value = condState.value.copy(operator = Questionnaire.QuestionnaireItemOperator.Exists)
            waitForIdle()

            // Delete
            onAllNodesWithContentDescription("Delete", substring = true, useUnmergedTree = true)[0].performClick()
            waitForIdle()
            assertTrue(deleted)
        }

    /**
     * Tests [updateConditionAnswer] helper function directly.
     */
    @Test
    fun testUpdateConditionAnswerHelper() {
        val initial = BuilderEnableWhen("q1", Questionnaire.QuestionnaireItemOperator.EqualTo, "old")
        val updated = updateConditionAnswer(initial, "new")
        assertEquals("new", updated.answerString)
        assertEquals("q1", updated.question)
    }
}
