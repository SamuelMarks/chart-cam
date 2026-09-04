/**
 * @file QuestionnaireListScreenJvmTest.kt
 * Contains declarations for QuestionnaireListScreenJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import com.google.fhir.model.r4.String as FhirString

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
     * Tests QuestionnaireListScreen on JVM.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testQuestionnaireListScreen() =
        runComposeUiTest {
            val mockQ =
                Questionnaire
                    .Builder(status = Enumeration(value = PublicationStatus.Active))
                    .apply {
                        id = "q-123"
                        title = FhirString.Builder().apply { value = "My Form" }
                    }.build()

            // This is async, so we'd better run it in runTest? But runComposeUiTest allows coroutines.
            // For simplicity, we can block or run runTest wrapper. Actually, we can just save it inside runComposeUiTest.
            runTest {
                repo.saveQuestionnaire(mockQ)
            }

            setContent {
                QuestionnaireListScreen(
                    questionnaireRepository = repo,
                    onBack = {},
                    onNavigateToBuilder = {},
                )
            }

            onNodeWithText("My Form").assertExists()
            // onNodeWithText("ID: q-123").assertExists()
        }
}
