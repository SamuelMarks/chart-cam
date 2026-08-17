/**
 * @file AndroidScreenshotGeneratorTest.kt
 * Contains the AndroidScreenshotGeneratorTest class for generating screenshots during tests.
 */
package io.healthplatform.chartcam

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * End-to-end UI tests that traverse the application to generate screenshots.
 */
@RunWith(AndroidJUnit4::class)
class AndroidScreenshotGeneratorTest {
    @get:Rule
    val composeTestRule =
        androidx.compose.ui.test.junit4
            .createAndroidComposeRule<io.healthplatform.chartcam.android.MainActivity>()

    /**
     * Saves a bitmap to the device's external cache directory.
     *
     * @param bitmap The image to save.
     * @param name The filename for the saved screenshot.
     */
    private fun saveScreenshot(
        bitmap: Bitmap,
        name: String,
    ) {
        // Save to external cache dir so adb can pull it easily
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.externalCacheDir, "screenshots")
        android.util.Log.e("SCREENSHOT_TEST", "Saving to: " + dir.absolutePath)

        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "$name.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    /**
     * Traverses the application and generates a set of end-to-end screenshots.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun generateE2EScreenshots() {
        /**
         * Captures the root node as an image and saves it.
         *
         * @param name The name of the screenshot file.
         */
        fun capture(name: String) {
            composeTestRule.waitForIdle()
            Thread.sleep(500) // Ensure animations settle
            composeTestRule.waitForIdle()
            val img = composeTestRule.onAllNodes(isRoot())[0].captureToImage().asAndroidBitmap()
            saveScreenshot(img, name)
        }

        /**
         * Safely attempts to click a UI node, handling potential flakiness.
         *
         * @param text The text or content description to search for.
         * @param isContentDescription Whether to search by content description instead of text.
         */
        fun safeClick(
            text: String,
            isContentDescription: Boolean = false,
        ) {
            var retries = 0
            while (retries < 5) {
                try {
                    if (isContentDescription) {
                        val node = composeTestRule.onAllNodes(hasContentDescription(text), useUnmergedTree = true)[0]
                        try {
                            node.performScrollTo()
                        } catch (e: AssertionError) {
                        }
                        node.performClick()
                    } else {
                        val node = composeTestRule.onAllNodes(hasText(text, substring = true, ignoreCase = true), useUnmergedTree = true)[0]
                        try {
                            node.performScrollTo()
                        } catch (e: AssertionError) {
                        }
                        node.performClick()
                    }
                    composeTestRule.waitForIdle()
                    return
                } catch (e: AssertionError) {
                    retries++
                    Thread.sleep(1000)
                    composeTestRule.waitForIdle()
                } catch (e: IndexOutOfBoundsException) {
                    retries++
                    Thread.sleep(1000)
                    composeTestRule.waitForIdle()
                }
            }
            // final try that will throw if it fails
            if (isContentDescription) {
                val node = composeTestRule.onAllNodes(hasContentDescription(text), useUnmergedTree = true)[0]
                try {
                    node.performScrollTo()
                } catch (e: AssertionError) {
                }
                node.performClick()
            } else {
                val node = composeTestRule.onAllNodes(hasText(text, substring = true, ignoreCase = true), useUnmergedTree = true)[0]
                try {
                    node.performScrollTo()
                } catch (e: AssertionError) {
                }
                node.performClick()
            }
            composeTestRule.waitForIdle()
        }

        // 0. Login/signup
        composeTestRule.waitForIdle()
        capture("iphone-00-login")
        composeTestRule.onNodeWithText("Username").performTextInput("clinician")
        composeTestRule.onNodeWithText("Password").performTextInput("123456")
        safeClick("Login / signup")

        // 2. List patients (Initially empty)
        capture("iphone-02-list-patients-empty")

        // 1. Create patient
        safeClick("Add Patient", isContentDescription = true)
        composeTestRule.onNodeWithText("First Name").performTextInput("Jane")
        composeTestRule.onNodeWithText("Last Name").performTextInput("Smith")
        composeTestRule.onNodeWithText("MRN").performTextInput("MRN-9876")
        composeTestRule.onNodeWithText("DOB (YYYY-MM-DD)").performTextInput("1985-05-15")
        capture("iphone-01-create-patient")
        safeClick("Create")

        // Patient detail view automatically shows after creation
        safeClick("Back", isContentDescription = true)

        // 2. List patients (Populated)
        safeClick("More options", isContentDescription = true)
        safeClick("Show All Patients")

        capture("iphone-02-list-patients")
        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        // 3. Create questionnaire with radio, select, free text, camera + label0, camera + label1
        safeClick("Jane", isContentDescription = false) // Go to patient
        safeClick("New Visit", isContentDescription = true)

        safeClick("Questionnaire Selector", isContentDescription = true)
        safeClick("Create New")

        composeTestRule.onNodeWithText("Questionnaire Title").performTextInput("Advanced Eye Exam")

        // Add Radio (Single Select)

        safeClick("Single Select", isContentDescription = true)
        composeTestRule.onNodeWithText("Label").performTextInput("Pain Level")
        composeTestRule.onNodeWithText("Add option").performTextInput("Low")
        safeClick("Add option", isContentDescription = true)
        composeTestRule.onNodeWithText("Add option").performTextInput("High")
        safeClick("Add option", isContentDescription = true)

        // Add Free Text

        safeClick("Single Line Text", isContentDescription = true)
        val textFields = composeTestRule.onAllNodes(hasText("Label"))
        textFields[1].performTextInput("Symptoms") // First label is Pain Level

        // Add Camera 0
        safeClick("Photo Camera", isContentDescription = true)
        val cameraFields = composeTestRule.onAllNodes(hasText("Label"))
        cameraFields[2].performTextInput("Left Eye")

        // Add Camera 1
        safeClick("Photo Camera", isContentDescription = true)
        val cameraFields2 = composeTestRule.onAllNodes(hasText("Label"))
        cameraFields2[3].performTextInput("Right Eye")

        capture("iphone-03-create-questionnaire")
        safeClick("Save", isContentDescription = true)

        // Back to Encounter Detail
        composeTestRule.waitForIdle()

        // 4. Fill in questionnaire for a given patient
        safeClick("Low")
        composeTestRule.onAllNodes(hasText("Symptoms", substring = true, ignoreCase = true))[0].performTextInput("Blurry vision")

        capture("iphone-04-fill-questionnaire")
        safeClick("Finalize Visit")
        safeClick("Back", isContentDescription = true)

        // 5. View questionnaires for a given patient (Encounters view in Patient Detail)
        capture("iphone-05-view-patient-questionnaires")

        // 6. View specific questionnaire filled out for given patient
        safeClick("Blurry vision")
        capture("iphone-06-view-specific-questionnaire")

        // Back to Patient Detail
        safeClick("Back", isContentDescription = true)
        // Back to List
        safeClick("Back", isContentDescription = true)

        // Go to Questionnaires List for Exporting
        safeClick("More options", isContentDescription = true)
        safeClick("Questionnaires")

        // 7. Export questionnaire
        safeClick("Advanced Eye Exam")
        capture("iphone-07-export-questionnaire-view")
        safeClick("Cancel", isContentDescription = true)
        safeClick("Share Questionnaire", isContentDescription = true)
        safeClick("Share Questionnaire")
        // Back to List
        safeClick("Back", isContentDescription = true)

        // 8. Export dataset (incl. with password)
        safeClick("More options", isContentDescription = true)
        safeClick("Export Data")
        composeTestRule
            .onAllNodes(
                androidx.compose.ui.test
                    .hasSetTextAction(),
            )[0]
            .performTextInput("secure123")
        capture("iphone-08-export-dataset")
        safeClick("Export")
        Thread.sleep(10000)
        composeTestRule.waitForIdle()

        try {
            safeClick("Close")
        } catch (e: Throwable) {
        }

        // 9. Logout
        safeClick("More options", isContentDescription = true)
        safeClick("Logout")
        capture("iphone-09-logout")
    }
}
