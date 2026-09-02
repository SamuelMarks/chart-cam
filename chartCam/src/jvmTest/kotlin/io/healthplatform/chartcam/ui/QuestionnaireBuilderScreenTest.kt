/**
 * @file QuestionnaireBuilderScreenTest.kt
 * Contains declarations for QuestionnaireBuilderScreenTest.kt.
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
import chartcam.chartcam.generated.resources.cd_preview
import chartcam.chartcam.generated.resources.cd_save
import chartcam.chartcam.generated.resources.preview_mode
import chartcam.chartcam.generated.resources.questionnaire_title
import chartcam.chartcam.generated.resources.widget_single_line_text
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Common test for [QuestionnaireBuilderScreen].
 */
class QuestionnaireBuilderScreenTest {
    /**
     * Verify clicking 'Add Item' renders a new question input form.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun questionnaireBuilderScreenAddItemDisplaysNewQuestionForm() =
        runTest {
            runComposeUiTest {
                val repo = QuestionnaireRepository()
                // removed loadDefaultForms
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

                // Check if Preview Mode string is not shown initially
                // Then toggle preview mode
                onNodeWithContentDescription(getString(Res.string.cd_preview)).performClick()

                // Check if Preview Mode string is shown
                onNodeWithText(getString(Res.string.preview_mode)).assertIsDisplayed()

                // We should see the new item listed
                onNodeWithText("New SINGLE_LINE_TEXT Item").assertIsDisplayed()
            }
        }

    /**
     * Verify completing the form and saving persists the questionnaire via the repository.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun questionnaireBuilderScreenSaveStoresQuestionnaireInRepository() =
        runTest {
            runComposeUiTest {
                val repo = QuestionnaireRepository()
                // removed loadDefaultForms
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

    /**
     * Verify validation logic prevents saving an incomplete questionnaire.
     * We don't enter title, it should not save.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun questionnaireBuilderScreenValidationPreventsSavingWithEmptyFields() =
        runTest {
            runComposeUiTest {
                val repo = QuestionnaireRepository()
                // removed loadDefaultForms
                val viewModel = QuestionnaireBuilderViewModel(repo)
                var savedId: String? = null

                setContent {
                    QuestionnaireBuilderScreen(
                        viewModel = viewModel,
                        onBack = { },
                        onSaved = { id -> savedId = id },
                    )
                }

                // Do not enter a title, just try to save
                onNodeWithContentDescription(getString(Res.string.cd_save)).performClick()

                // Should not save
                assertTrue(savedId == null)
            }
        }
}
