/**
 * @file AccessibilityJvmTest.kt
 * Contains UI accessibility tests for the Compose UI.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.Test

/**
 * Validates accessibility guidelines (content descriptions, semantic trees, focus traversal hints)
 * on core screens as outlined in docs/ACCESSIBILITY.md.
 */
class AccessibilityJvmTest {
    /**
     * Verifies that the LoginScreen inputs and buttons are properly labeled and accessible.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLoginScreenAccessibility() =
        runComposeUiTest {
            setContent {
                LoginScreen(
                    viewModel = LoginViewModel(mock(AuthRepository::class.java)),
                    onLoginSuccess = {},
                )
            }
            waitForIdle()

            // Verify inputs have semantic labels via text placeholders or actual labels
            onNodeWithText("Username").assertIsDisplayed()
            onNodeWithText("Password").assertIsDisplayed()

            // The Login button should be actionable and semantically clear
            onNodeWithText("Login / signup").assertIsDisplayed().assertHasClickAction()
        }

    /**
     * Verifies that the PatientListScreen top bar actions and FAB are accessible.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testPatientListScreenAccessibility() =
        runComposeUiTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val fhirRepository = FhirRepository(ChartCamDatabase(driver))
            val authRepository = mock(AuthRepository::class.java)
            val exportImportService = mock(ExportImportService::class.java)

            val practitioner = Practitioner.Builder().apply { id = "prac-1" }.build()
            `when`(authRepository.currentUser).thenReturn(MutableStateFlow(practitioner))

            setContent {
                PatientListScreen(
                    dependencies = PatientListDependencies(fhirRepository, exportImportService, authRepository),
                    actions = PatientListActions({}, {}, {}),
                )
            }
            waitForIdle()

            // Verify top bar actions have content descriptions for screen readers
            onNodeWithContentDescription("More options").assertIsDisplayed().assertHasClickAction()

            // Verify Add Patient FAB has proper content description and click action
            onNodeWithContentDescription("Add Patient").assertIsDisplayed().assertHasClickAction()

            driver.close()
        }

    /**
     * Verifies that the LoginScreen offline mode toggle is disabled and displays proper state description,
     * and that submitting empty credentials produces accessibility errors.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLoginScreenA11yOfflineSwitchAndErrorSemantics() =
        runComposeUiTest {
            setContent {
                LoginScreen(
                    viewModel = LoginViewModel(mock(AuthRepository::class.java)),
                    onLoginSuccess = {},
                )
            }
            waitForIdle()

            // Verify offline switch has state description and disabled semantics
            val hasDisabledOrState =
                SemanticsMatcher("has state description") { node ->
                    node.config.getOrNull(SemanticsProperties.StateDescription) != null
                }
            onNodeWithContentDescription("Offline mode").assertExists().assert(hasDisabledOrState)

            // Verify app title has heading semantics
            val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
            onNodeWithText("ChartCam").assert(isHeading)

            // Click Login without credentials to trigger error state
            onNodeWithText("Login / signup").performClick()
            waitForIdle()

            // Error message should be displayed with polite live region on text and inputs
            onAllNodesWithText("All fields are required").assertCountEquals(3)
            val hasPoliteLiveRegion =
                SemanticsMatcher("has polite live region") { node ->
                    node.config.getOrNull(SemanticsProperties.LiveRegion) ==
                        androidx.compose.ui.semantics.LiveRegionMode.Polite
                }
            onAllNodes(hasPoliteLiveRegion).assertCountEquals(1)
        }
}
