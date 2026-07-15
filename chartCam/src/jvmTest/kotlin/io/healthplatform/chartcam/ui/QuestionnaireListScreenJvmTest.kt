package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.copy_to_clipboard
import chartcam.chartcam.generated.resources.import_questionnaire
import chartcam.chartcam.generated.resources.paste_from_clipboard
import chartcam.chartcam.generated.resources.share_questionnaire
import chartcam.chartcam.generated.resources.share_text_json
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class QuestionnaireListScreenJvmTest {
    @Test
    fun testShareBottomSheetShowsUp() =
        runTest {
            runComposeUiTest {
                val repo = QuestionnaireRepository()
                kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }

                setContent {
                    QuestionnaireListScreen(
                        questionnaireRepository = repo,
                        onBack = {},
                        onNavigateToBuilder = {},
                    )
                }

                onAllNodes(
                    androidx.compose.ui.test
                        .hasContentDescription(getString(Res.string.share_questionnaire)),
                )[0].performClick()

                onNodeWithText(getString(Res.string.share_questionnaire)).assertExists()
                onNodeWithText(getString(Res.string.copy_to_clipboard)).assertExists()
                onNodeWithText(getString(Res.string.share_text_json)).assertExists()
            }
        }

    @Test
    fun testImportBottomSheetShowsUp() =
        runTest {
            runComposeUiTest {
                val repo = QuestionnaireRepository()
                kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }

                setContent {
                    QuestionnaireListScreen(
                        questionnaireRepository = repo,
                        onBack = {},
                        onNavigateToBuilder = {},
                    )
                }

                onNodeWithContentDescription(getString(Res.string.import_questionnaire)).performClick()

                onNodeWithText(getString(Res.string.import_questionnaire)).assertExists()
                onNodeWithText(getString(Res.string.paste_from_clipboard)).assertExists()
            }
        }
}
