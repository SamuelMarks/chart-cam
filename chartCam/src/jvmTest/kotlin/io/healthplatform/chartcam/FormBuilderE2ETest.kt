package io.healthplatform.chartcam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sdc.SdcQuestionnaireForm
import io.healthplatform.chartcam.ui.QuestionnaireBuilderScreen
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import org.junit.Rule
import org.junit.Test

class FormBuilderE2ETest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testFormBuilderJourney() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        var isSaved = false

        rule.setContent {
            if (!isSaved) {
                QuestionnaireBuilderScreen(
                    viewModel = viewModel,
                    onBack = { },
                    onSaved = { isSaved = true },
                )
            } else {
                // Simulate submission phase using the generated form
                val q = repo.getAvailableQuestionnaires().find { it.title?.value == "My E2E Form" }
                if (q != null) {
                    io.healthplatform.chartcam.sdc.SdcQuestionnaireForm(
                        questionnaire = q,
                        answers = emptyMap(),
                        onFormUpdated = { _, _ -> },
                    )
                }
            }
        }

        // 1. Form Creation Journey
        rule.onNodeWithText("Questionnaire Title").performTextInput("My E2E Form")

        // Add a single line text input widget
        rule.onNodeWithContentDescription("Single Line Text").performClick()

        // Let's preview
        rule.onNodeWithContentDescription("Preview").performClick()
        rule.onNodeWithText("Preview Mode").assertIsDisplayed()
        rule.onNodeWithContentDescription("Preview").performClick() // Toggle back

        // Save
        rule.onNodeWithContentDescription("Save").performClick()

        // 2. Form Submission Journey
        // The UI should now render the io.healthplatform.chartcam.sdc.SdcQuestionnaireForm
        // Our added widget is a Single Line Text item with default label "New SINGLE_LINE_TEXT Item"
        rule.onNodeWithText("New SINGLE_LINE_TEXT Item").assertIsDisplayed()
    }
}
