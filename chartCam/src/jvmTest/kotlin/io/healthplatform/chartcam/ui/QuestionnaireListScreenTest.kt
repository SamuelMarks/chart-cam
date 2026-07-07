package io.healthplatform.chartcam.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class QuestionnaireListScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testShareBottomSheetShowsUp() {
        val repo = QuestionnaireRepository() // comes with "std-form" and "basic-followup"

        rule.setContent {
            QuestionnaireListScreen(
                questionnaireRepository = repo,
                onBack = {},
                onNavigateToBuilder = {},
            )
        }

        // Click on the first share button
        rule
            .onAllNodes(
                androidx.compose.ui.test
                    .hasContentDescription("Share"),
            )[0]
            .performClick()

        // Assert bottom sheet is displayed by checking for text
        rule.onNodeWithText("Share Questionnaire").assertExists()
        rule.onNodeWithText("Copy to Clipboard").assertExists()
        rule.onNodeWithText("Share text/JSON").assertExists()
    }

    @Test
    fun testImportBottomSheetShowsUp() {
        val repo = QuestionnaireRepository()

        rule.setContent {
            QuestionnaireListScreen(
                questionnaireRepository = repo,
                onBack = {},
                onNavigateToBuilder = {},
            )
        }

        // Click the top app bar Import button
        rule.onNodeWithContentDescription("Import Questionnaire").performClick()

        // Assert import bottom sheet is displayed
        rule.onNodeWithText("Import Questionnaire").assertExists()
        rule.onNodeWithText("Paste from Clipboard").assertExists()
    }

    @Test
    fun testImportFromClipboard() {
        val repo = QuestionnaireRepository()
        val validJson =
            """{"version":1,"app":"ChartCam","fhirJson":"{\"resourceType\":\"Questionnaire\",""" +
                """\"id\":\"imported-form\",\"title\":\"Imported Questionnaire\",\"status\":\"active\",""" +
                """\"item\":[{\"linkId\":\"notes\",\"text\":\"Notes\",\"type\":\"string\"}]}"}"""

        val fakeClipboard =
            object : androidx.compose.ui.platform.Clipboard {
                val myClipboard =
                    java.awt.datatransfer.Clipboard("fake").apply {
                        setContents(java.awt.datatransfer.StringSelection(validJson), null)
                    }

                override suspend fun getClipEntry(): androidx.compose.ui.platform.ClipEntry? = null

                override suspend fun setClipEntry(clipEntry: androidx.compose.ui.platform.ClipEntry?) {}

                override val nativeClipboard: androidx.compose.ui.platform.NativeClipboard
                    get() = myClipboard as androidx.compose.ui.platform.NativeClipboard
            }

        rule.setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalClipboard provides fakeClipboard) {
                QuestionnaireListScreen(
                    questionnaireRepository = repo,
                    onBack = {},
                    onNavigateToBuilder = {},
                )
            }
        }

        // Click Import
        rule.onNodeWithContentDescription("Import Questionnaire").performClick()

        // Click Paste
        rule.onNodeWithText("Paste from Clipboard").performClick()

        // Wait for preview dialog
        rule.waitForIdle()

        // Validate preview dialog text
        rule.onNodeWithText("Import Questionnaire").assertExists()
        rule.onNodeWithText("Title: Imported Questionnaire", substring = true).assertExists()

        // Click Import in the dialog
        rule.onNodeWithText("Import").performClick()

        // Wait for UI to update and dialog to close
        rule.waitForIdle()

        // Validate the imported questionnaire is now in the list
        rule.onNodeWithText("Imported Questionnaire").assertExists()
        rule.onNodeWithText("imported-form").assertExists()
    }

    @Test
    fun testImportFromClipboardInvalidJson() {
        val repo = QuestionnaireRepository()
        val invalidJson = """{"version":2,"app":"ChartCam","fhirJson":"{}"}"""

        val fakeClipboard =
            object : androidx.compose.ui.platform.Clipboard {
                val myClipboard =
                    java.awt.datatransfer.Clipboard("fake").apply {
                        setContents(java.awt.datatransfer.StringSelection(invalidJson), null)
                    }

                override suspend fun getClipEntry(): androidx.compose.ui.platform.ClipEntry? = null

                override suspend fun setClipEntry(clipEntry: androidx.compose.ui.platform.ClipEntry?) {}

                override val nativeClipboard: androidx.compose.ui.platform.NativeClipboard
                    get() = myClipboard as androidx.compose.ui.platform.NativeClipboard
            }

        rule.setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalClipboard provides fakeClipboard) {
                QuestionnaireListScreen(
                    questionnaireRepository = repo,
                    onBack = {},
                    onNavigateToBuilder = {},
                )
            }
        }

        // Click Import
        rule.onNodeWithContentDescription("Import Questionnaire").performClick()

        // Click Paste
        rule.onNodeWithText("Paste from Clipboard").performClick()

        // Wait for UI to update
        rule.waitForIdle()

        // Validate error message is shown
        rule.onNodeWithText("Import Error: Unsupported schema version: 2. Please update the app.", substring = true).assertExists()
    }
}
