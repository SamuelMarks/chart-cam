/**
 * @file FormLabelJvmTest.kt
 * Contains declarations for FormLabelJvmTest.kt.
 *
 * JVM Compose UI tests for the [FormLabel] component.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.ui.setAppLanguage
import org.junit.Test

/**
 * Test suite validating accessibility and visual labeling for [FormLabel].
 */
class FormLabelJvmTest {
    /**
     * Verifies default parameter usage and optional form label display.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDefaultFormLabel() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                FormLabel(text = "Patient MRN")
            }
            waitForIdle()

            onNodeWithText("Patient MRN").assertIsDisplayed()
            onNodeWithContentDescription("Patient MRN").assertIsDisplayed()
        }
    }

    /**
     * Verifies that an optional form label displays base text without required indicators.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testOptionalFormLabel() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                FormLabel(text = "Patient MRN", isRequired = false, modifier = Modifier.testTag("optional_label"))
            }
            waitForIdle()

            onNodeWithTag("optional_label").assertIsDisplayed()
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
                FormLabel(text = "First Name", isRequired = true, modifier = Modifier.testTag("required_label"))
            }
            waitForIdle()

            onNodeWithTag("required_label").assertIsDisplayed()
            onNodeWithText("First Name *").assertIsDisplayed()
            onNodeWithContentDescription("First Name, required").assertIsDisplayed()
        }
    }

    /**
     * Verifies recomposition behavior when properties change and when parent recomposes with unchanged properties.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormLabelRecomposition() {
        setAppLanguage("en")
        runComposeUiTest {
            val labelText = mutableStateOf("Initial Label")
            val isRequiredState = mutableStateOf(false)
            val parentTrigger = mutableStateOf(0)

            setContent {
                // Read parent trigger to force recomposition
                parentTrigger.value
                FormLabel(
                    text = labelText.value,
                    isRequired = isRequiredState.value,
                    modifier = Modifier.testTag("recomposed_label"),
                )
            }
            waitForIdle()

            onNodeWithText("Initial Label").assertIsDisplayed()

            // Update text and requirement
            labelText.value = "Updated Field"
            isRequiredState.value = true
            waitForIdle()

            onNodeWithText("Updated Field *").assertIsDisplayed()

            // Recompose parent with no child state changes (smart recomposition skip)
            parentTrigger.value = 1
            waitForIdle()

            onNodeWithText("Updated Field *").assertIsDisplayed()
        }
    }

    /**
     * Verifies required form label formatting in Japanese (East Asian locale with full-width punctuation).
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRequiredFormLabelJapanese() {
        setAppLanguage("ja")
        runComposeUiTest {
            setContent {
                FormLabel(text = "氏名", isRequired = true)
            }
            waitForIdle()

            onNodeWithText("氏名 *").assertIsDisplayed()
            onNodeWithContentDescription("氏名、必須").assertIsDisplayed()
        }
    }

    /**
     * Verifies required form label formatting in Hebrew (RTL locale).
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRequiredFormLabelHebrew() {
        setAppLanguage("he")
        runComposeUiTest {
            setContent {
                FormLabel(text = "שם פרטי", isRequired = true)
            }
            waitForIdle()

            onNodeWithText("שם פרטי *").assertIsDisplayed()
            onNodeWithContentDescription("שם פרטי, שדה חובה").assertIsDisplayed()
        }
    }

    /**
     * Verifies required form label formatting in Spanish (Romance LTR locale).
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRequiredFormLabelSpanish() {
        setAppLanguage("es")
        runComposeUiTest {
            setContent {
                FormLabel(text = "Nombre", isRequired = true)
            }
            waitForIdle()

            onNodeWithText("Nombre *").assertIsDisplayed()
            onNodeWithContentDescription("Nombre, obligatorio").assertIsDisplayed()
        }
    }
}
