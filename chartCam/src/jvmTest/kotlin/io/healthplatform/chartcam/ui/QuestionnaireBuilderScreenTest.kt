package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class QuestionnaireBuilderScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testBuilderScreenRendersAndNavigatesBack() {
        val repo = QuestionnaireRepository()
        val viewModel = QuestionnaireBuilderViewModel(repo)
        var backPressed = false

        rule.setContent {
            QuestionnaireBuilderScreen(
                viewModel = viewModel,
                onBack = { backPressed = true },
            )
        }

        // Check if build questionnaire string is displayed
        rule.onNodeWithText("Build Questionnaire").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backPressed)
    }

    @Test
    fun testBuilderAddAndPreview() {
        val repo = QuestionnaireRepository()
        val viewModel = QuestionnaireBuilderViewModel(repo)

        rule.setContent {
            QuestionnaireBuilderScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        rule.onNodeWithText("Questionnaire Title").performTextInput("My Custom Test")
        // Click Add to add the default SINGLE_LINE_TEXT item
        rule.onNodeWithContentDescription("Single Line Text").performClick()

        // Toggle preview mode
        rule.onNodeWithContentDescription("Preview").performClick()

        // Check if Preview Mode string is shown
        rule.onNodeWithText("Preview Mode").assertIsDisplayed()

        // We should see the new item listed
        rule.onNodeWithText("New SINGLE_LINE_TEXT Item (SINGLE_LINE_TEXT)").assertIsDisplayed()
    }

    @Test
    fun testBuilderSave() {
        val repo = QuestionnaireRepository()
        val viewModel = QuestionnaireBuilderViewModel(repo)
        var savedId: String? = null

        rule.setContent {
            QuestionnaireBuilderScreen(
                viewModel = viewModel,
                onBack = { },
                onSaved = { id -> savedId = id },
            )
        }

        rule.onNodeWithText("Questionnaire Title").performTextInput("Form Save Test")
        rule.onNodeWithContentDescription("Save").performClick()

        assertTrue(savedId != null)
        assertTrue(repo.getAvailableQuestionnaires().any { it.title?.value == "Form Save Test" })
    }

    @Test
    fun testWidgetSelectionRow() {
        var selectedWidget: io.healthplatform.chartcam.viewmodel.WidgetType? = null
        rule.setContent {
            WidgetSelectionRow(
                onWidgetSelected = { selectedWidget = it },
            )
        }

        // Open dropdown
        rule.onNodeWithContentDescription("More widgets").performClick()

        // Select NUMERIC
        rule.onNodeWithText("Numeric").performClick()

        assertTrue(selectedWidget == io.healthplatform.chartcam.viewmodel.WidgetType.NUMERIC)
    }
}
