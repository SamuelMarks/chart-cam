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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
