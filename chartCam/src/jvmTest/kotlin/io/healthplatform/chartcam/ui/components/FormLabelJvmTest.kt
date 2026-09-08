/**
 * @file FormLabelJvmTest.kt
 * Contains declarations for FormLabelJvmTest.kt.
 *
 * JVM Compose UI tests for the [FormLabel] component.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.ui.setAppLanguage
import org.junit.Test

/**
 * Test suite validating accessibility and visual labeling for [FormLabel].
 */
class FormLabelJvmTest {
    /**
     * Verifies that an optional form label displays base text without required indicators.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testOptionalFormLabel() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                FormLabel(text = "Patient MRN", isRequired = false)
            }
            waitForIdle()

            onNodeWithText("Patient MRN").assertIsDisplayed()
            onNodeWithContentDescription("Patient MRN").assertIsDisplayed()
        }
    }

    /**
     * Verifies that a required form label appends an asterisk visually and provides accessible semantic required status.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRequiredFormLabel() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                FormLabel(text = "First Name", isRequired = true)
            }
            waitForIdle()

            onNodeWithText("First Name *").assertIsDisplayed()
            onNodeWithContentDescription("First Name, required").assertIsDisplayed()
        }
    }
}
