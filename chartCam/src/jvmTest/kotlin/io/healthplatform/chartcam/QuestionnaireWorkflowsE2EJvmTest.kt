package io.healthplatform.chartcam

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class QuestionnaireWorkflowsE2EJvmTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun duplicateQuestionnaireWorkflow() {
        val repo =
            io.healthplatform.chartcam.repository
                .QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }

        var navigateToBuilderId: String? = null

        rule.setContent {
            io.healthplatform.chartcam.ui.QuestionnaireListScreen(
                questionnaireRepository = repo,
                onBack = {},
                onNavigateToBuilder = { id -> navigateToBuilderId = id },
            )
        }

        // Wait for idle
        rule.waitForIdle()

        // The default list should have "Standard Clinical Photo" and maybe others.
        // Let's click on "Standard Clinical Photo" (std-form) to view it.
        rule.onNodeWithText("Standard Clinical Photo").assertExists().performClick()
        rule.waitForIdle()

        // Assert we are in the view mode (read only, has Duplicate and Delete buttons)
        rule.onNodeWithContentDescription("Duplicate", substring = true).assertIsDisplayed()
        rule.onNodeWithContentDescription("Delete", substring = true).assertIsDisplayed()

        // 3. Duplicate it
        rule.onNodeWithContentDescription("Duplicate", substring = true).performClick()
        rule.waitForIdle()

        // 4. Assert that we would navigate to the builder
        kotlin.test.assertNotNull(navigateToBuilderId)

        // At this point, rather than switching full screens, we've proven the duplication triggered a navigation correctly
        // Testing the builder functionality is done in QuestionnaireBuilderScreenTest.
    }
}
