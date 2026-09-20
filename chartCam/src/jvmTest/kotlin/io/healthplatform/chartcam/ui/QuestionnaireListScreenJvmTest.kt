/**
 * @file QuestionnaireListScreenJvmTest.kt
 * Contains declarations for QuestionnaireListScreenJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Test class for QuestionnaireListScreen on JVM.
 */
class QuestionnaireListScreenJvmTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repo: QuestionnaireRepository

    /**
     * Sets up the test environment.
     */
    @Before
    fun setup() {
        setAppLanguage("en")
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        repo = QuestionnaireRepository()
    }

    /**
     * Tears down the test environment.
     */
    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Tests QuestionnaireListScreen list rendering and Scan QR dialog interactions on JVM.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testQuestionnaireListScreen() {
        val mockQ =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = "q-123"
                    title = FhirString(value = "Custom Test Form").toBuilder()
                }.build()
        repo.saveQuestionnaire(mockQ)

        runComposeUiTest {
            setAppLanguage("en")
            setContent {
                QuestionnaireListScreen(
                    questionnaireRepository = repo,
                    onBack = {},
                    onNavigateToBuilder = {},
                )
            }

            waitForIdle()
            onNodeWithText("Custom Test Form", useUnmergedTree = true).assertIsDisplayed()

            // Open Import options sheet
            onNodeWithContentDescription("Import Questionnaire", useUnmergedTree = true).performClick()
            waitForIdle()

            // Click Scan QR Code action
            onNodeWithText("Scan QR Code", useUnmergedTree = true).performClick()
            waitForIdle()

            // Verify dialog heading and localized chunk label
            onNodeWithText("QR Payload or Chunk", useUnmergedTree = true).assertIsDisplayed()

            // Dismiss dialog
            onNodeWithText("Cancel", useUnmergedTree = true).performClick()
            waitForIdle()
        }
    }
}
