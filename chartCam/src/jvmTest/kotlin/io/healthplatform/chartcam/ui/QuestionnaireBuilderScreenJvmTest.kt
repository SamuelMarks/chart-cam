/**
 * @file QuestionnaireBuilderScreenJvmTest.kt
 * Contains declarations for QuestionnaireBuilderScreenJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.build_questionnaire
import chartcam.chartcam.generated.resources.cd_back
import chartcam.chartcam.generated.resources.cd_more_widgets
import chartcam.chartcam.generated.resources.cd_preview
import chartcam.chartcam.generated.resources.cd_save
import chartcam.chartcam.generated.resources.preview_mode
import chartcam.chartcam.generated.resources.questionnaire_title
import chartcam.chartcam.generated.resources.widget_numeric
import chartcam.chartcam.generated.resources.widget_single_line_text
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString
import org.junit.Test
import kotlin.test.assertTrue

class QuestionnaireBuilderScreenJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBuilderScreenRendersAndNavigatesBack() =
        runTest {
            runComposeUiTest {
                val repo = QuestionnaireRepository()
                kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
                val viewModel = QuestionnaireBuilderViewModel(repo)
                var backPressed = false

                setContent {
                    QuestionnaireBuilderScreen(
                        viewModel = viewModel,
                        onBack = { backPressed = true },
                    )
                }

                // Check if build questionnaire string is displayed
                onNodeWithText(getString(Res.string.build_questionnaire)).assertIsDisplayed()
                onNodeWithContentDescription(getString(Res.string.cd_back)).performClick()
                assertTrue(backPressed)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBuilderAddAndPreview() =
        runTest {
            runComposeUiTest {
                val repo = QuestionnaireRepository()
                kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
                val viewModel = QuestionnaireBuilderViewModel(repo)

                setContent {
                    QuestionnaireBuilderScreen(
                        viewModel = viewModel,
                        onBack = {},
                    )
                }

                onNodeWithText(getString(Res.string.questionnaire_title)).performTextInput("My Custom Test")
                // Click Add to add the default SINGLE_LINE_TEXT item
                onNodeWithContentDescription(getString(Res.string.widget_single_line_text)).performClick()

                // Toggle preview mode
                onNodeWithContentDescription(getString(Res.string.cd_preview)).performClick()

                // Check if Preview Mode string is shown
                onNodeWithText(getString(Res.string.preview_mode)).assertIsDisplayed()

                // We should see the new item listed
                onNodeWithText("New SINGLE_LINE_TEXT Item").assertIsDisplayed()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBuilderSave() =
        runTest {
            runComposeUiTest {
                val repo = QuestionnaireRepository()
                kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
                val viewModel = QuestionnaireBuilderViewModel(repo)
                var savedId: String? = null

                setContent {
                    QuestionnaireBuilderScreen(
                        viewModel = viewModel,
                        onBack = { },
                        onSaved = { id -> savedId = id },
                    )
                }

                onNodeWithText(getString(Res.string.questionnaire_title)).performTextInput("Form Save Test")
                onNodeWithContentDescription(getString(Res.string.widget_single_line_text)).performClick()
                onNodeWithContentDescription(getString(Res.string.cd_save)).performClick()

                assertTrue(savedId != null)
                assertTrue(repo.getAvailableQuestionnaires().any { it.title?.value == "Form Save Test" })
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testWidgetSelectionRow() =
        runTest {
            runComposeUiTest {
                var selectedWidget: io.healthplatform.chartcam.viewmodel.WidgetType? = null
                setContent {
                    WidgetSelectionRow(
                        onWidgetSelected = { selectedWidget = it },
                    )
                }

                // Open dropdown
                onNodeWithContentDescription(getString(Res.string.cd_more_widgets)).performClick()

                // Select NUMERIC
                onNodeWithText(getString(Res.string.widget_numeric)).performClick()

                assertTrue(selectedWidget == io.healthplatform.chartcam.viewmodel.WidgetType.NUMERIC)
            }
        }
}
