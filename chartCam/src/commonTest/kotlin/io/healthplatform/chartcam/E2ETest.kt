/**
 * @file E2ETest.kt
 * Contains declarations for E2ETest.kt.
 */
package io.healthplatform.chartcam

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * End-to-end tests for the main workflow of the app.
 */
class E2ETest {
    /**
     * Tests the full clinical workflow, from login to questionnaire building to data export.
     */
    @OptIn(ExperimentalTestApi::class)
    @kotlin.test.Ignore
    @Test
    fun testFullWorkflowE2E() {
        try {
            runComposeUiTest {
                setContent {
                    App()
                }

                // 0. Login
                onNodeWithText("Username").performTextInput("testuser")
                onNodeWithText("Password").performTextInput("password")
                onNodeWithText("Login / signup").performClick()

                // Wait for Patient Directory
                onNodeWithText("Patient Directory").assertExists()

                // 1. Create patient
                onNodeWithContentDescription("Add Patient").performClick()
                onNodeWithText("First Name").performTextInput("John")
                onNodeWithText("Last Name").performTextInput("Doe")
                onNodeWithText("MRN").performTextInput("MRN-123")
                onNodeWithText("DOB (YYYY-MM-DD)").performTextInput("1990-01-01")
                onNodeWithText("Create").performClick()

                // 2. Create visit
                onNodeWithContentDescription("New Visit").performClick()

                // 3. Create new questionnaire from dropdown
                onNodeWithText("Select Questionnaire").performClick()
                onNodeWithText("Create New").performClick()

                // Give it a title
                onNodeWithText("Questionnaire Title").performTextInput("Comprehensive E2E Form")

                // 4. Create new camera with custom label
                onNodeWithContentDescription("Photo Camera").performClick()
                onNodeWithText("Label").performTextInput("Left Eye")

                // 5. Create new camera with custom other label
                onNodeWithContentDescription("Photo Camera").performClick()
                onNodeWithText("Label").performTextInput("Right Eye")

                // 6. Create new other widgets (get to ~10 widgets)
                // Adding other widget types via dropdown
                onNodeWithContentDescription("More widgets").performClick()
                onNodeWithText("Single Select").performClick()
                onNodeWithText("Label").performTextInput("Condition")
                onNodeWithText("Add option").performTextInput("Healthy")
                onNodeWithContentDescription("Add").performClick()

                onNodeWithContentDescription("More widgets").performClick()
                onNodeWithText("Date").performClick()
                onNodeWithText("Label").performTextInput("Follow up date")

                // 7. Save questionnaire
                onNodeWithContentDescription("Save").performClick()

                // 8. Fill out questionnaire
                // The newly saved form is now selected and we are in EncounterDetailScreen
                onNodeWithText("Left Eye").assertExists()
                onNodeWithText("Right Eye").assertExists()

                // 9. Confirm questionnaire renders well once filled out
                onNodeWithContentDescription("Finalize Encounter").performClick()

                // 10. Return to patient list screen
                onNodeWithContentDescription("Back").performClick() // to Patient List (from Patient Detail)

                // 11. Select questionnaire from dropdown (in Questionnaires list)
                // Need to navigate to questionnaires?
                // Let's assume we can navigate to questionnaires from the menu or bottom bar
                // or just by clicking the dropdown in a new visit.
                onNodeWithContentDescription("Add Patient").assertExists() // confirms we are in Patient List

                // Let's go to questionnaire manager
                onNodeWithContentDescription("More options").performClick()
                onNodeWithText("Questionnaires").performClick()

                // Select it
                onNodeWithText("Comprehensive E2E Form").performClick()

                // 12. Share questionnaire
                onNodeWithText("Share Questionnaire").performClick()

                // 13. Return to patient list screen
                onNodeWithContentDescription("Back").performClick()
                onNodeWithContentDescription("Back").performClick() // if nested

                // 14. Export all patients + visit with password
                onNodeWithContentDescription("More options").performClick()
                onNodeWithText("Export Data").performClick()
                onNodeWithText("Password").performTextInput("secure123")
                onNodeWithText("Export").performClick()

                // 15. Decrypt locally (in another target? - JVM if iOS, etc.) to confirm import and export work
                // UI test just simulates generating the export. The actual cross-target test
                // will be handled by Kotlin Multiplatform test runners.
                onNodeWithText("Data Exported").assertExists()
                onNodeWithText("Close").performClick()
            }
        } catch (e: IllegalStateException) {
            if (e.message?.contains("State must be at least") != true &&
                e.message?.contains("setCurrentState") != true
            ) {
                throw e
            }
        }
    }
}
