/**
 * @file ScreenshotGeneratorE2E.kt
 * Contains the [ScreenshotGeneratorE2E] test class.
 */
package io.healthplatform.chartcam

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.ui.CaptureScreen
import io.healthplatform.chartcam.ui.QuestionnaireListScreen
import io.healthplatform.chartcam.ui.theme.AppTheme
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

/**
 * End-to-end UI tests that traverse the application on JVM to generate desktop screenshots.
 */
@OptIn(ExperimentalTestApi::class)
class ScreenshotGeneratorE2E {
    /**
     * Captures the root UI node and saves it as an image file.
     *
     * @param composeTestRule The active compose UI test rule.
     * @param filename The path and filename to save the image.
     */
    private fun takeScreenshot(
        composeTestRule: DesktopComposeUiTest,
        filename: String,
    ) {
        val f = File(filename)
        f.parentFile.mkdirs()
        val img =
            composeTestRule
                .onAllNodes(isRoot())
                .onFirst()
                .captureToImage()
                .toAwtImage()
        ImageIO.write(img, "png", f)
        println("WROTE " + f.absolutePath)
    }

    /**
     * Sets up the database and repository dependencies for the test.
     *
     * @return A Pair containing the [FhirRepository] and [QuestionnaireRepository].
     */
    private fun setupDeps(): Pair<FhirRepository, QuestionnaireRepository> {
        val dbFactory = DatabaseDriverFactory()
        val driver = dbFactory.createDriver()
        val fhirRepository = FhirRepository(driver)
        val questionnaireRepository = QuestionnaireRepository(fhirRepository)
        runBlocking {
            io.healthplatform.chartcam.initDatabase(driver)
            questionnaireRepository.loadDefaultForms()
        }
        return Pair(fhirRepository, questionnaireRepository)
    }

    /**
     * Test function that generates screenshots for the questionnaire sharing and capture screens.
     */
    @Test
    fun generateQuestionnaireScreenshots() {
        File("chartcam_desktop.db").delete()
        val (fhirRepo, qRepo) = setupDeps()

        // Create the questionnaire for sharing and capturing
        val q =
            runBlocking {
                qRepo.createQuestionnaire("Burn Assessment", 2, "Left Arm, Right Arm")
            }

        listOf("iphone" to Pair(1284, 2778), "ipad" to Pair(2048, 2732)).forEach { (device, size) ->
            // Screenshot 07: Share Questionnaire
            runDesktopComposeUiTest(width = size.first, height = size.second) {
                setContent {
                    AppTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            QuestionnaireListScreen(questionnaireRepository = qRepo, onBack = {}, onNavigateToBuilder = {})
                        }
                    }
                }
                waitForIdle()
                // Click the share button on Burn Assessment
                onAllNodesWithContentDescription("Share Questionnaire", substring = true, ignoreCase = true).onFirst().performClick()
                waitForIdle()
                takeScreenshot(this, "../fastlane/screenshots/en-US/" + device + "-07-questionnaire-share.png")
            }

            // Screenshot 08: Fill out Questionnaire (CaptureScreen)
            runDesktopComposeUiTest(width = size.first, height = size.second) {
                setContent {
                    AppTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            CaptureScreen(
                                questionnaireId = q.id!!,
                                linkId = null,
                                questionnaireRepository = qRepo,
                                onFinished = {},
                                onCancel = {},
                            )
                        }
                    }
                }
                waitForIdle()
                takeScreenshot(this, "../fastlane/screenshots/en-US/" + device + "-08-fill-questionnaire.png")
            }
        }
    }
}
