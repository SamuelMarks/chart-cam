/**
 * @file FormBuilderWidgetsJvmTest.kt
 * Contains declarations for FormBuilderWidgetsJvmTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test class for FormBuilderWidgets on JVM.
 */
class FormBuilderWidgetsJvmTest {
    /**
     * Tests FormBuilderTextInput.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderTextInput() =
        runComposeUiTest {
            var value = ""
            setContent {
                FormBuilderTextInput(
                    label = "Test Label",
                    value = value,
                    onValueChange = { value = it },
                    isRequired = true,
                )
            }

            onNodeWithText("Test Label *").assertExists()
            onNodeWithText("Test Label *").performTextInput("Hello")
            assertEquals("Hello", value)
        }

    /**
     * Tests FormBuilderCheckbox.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderCheckbox() =
        runComposeUiTest {
            var isChecked = false
            setContent {
                FormBuilderCheckbox(
                    label = "Agree",
                    checked = isChecked,
                    onCheckedChange = { isChecked = it },
                )
            }

            onNodeWithText("Agree").assertExists()
            onNodeWithText("Agree").performClick()
            assertTrue(isChecked)
        }

    /**
     * Tests FormBuilder widgets error states.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderWidgetsErrorStates() =
        runComposeUiTest {
            setContent {
                Column {
                    FormBuilderTextInput(
                        label = "Text Input",
                        value = "",
                        onValueChange = {},
                        isError = true,
                        errorMessage = "Error!",
                    )
                    FormBuilderTextArea(
                        label = "Text Area",
                        value = "",
                        onValueChange = {},
                        isError = true,
                        errorMessage = "Error!",
                    )
                    FormBuilderSwitch(
                        label = "Switch",
                        checked = false,
                        onCheckedChange = {},
                        isError = true,
                        errorMessage = "Error!",
                    )
                    FormBuilderCheckbox(
                        label = "Checkbox",
                        checked = false,
                        onCheckedChange = {},
                        isError = true,
                        errorMessage = "Error!",
                    )
                    FormBuilderNumericInput(
                        label = "Numeric Input",
                        value = "",
                        onValueChange = {},
                        isError = true,
                        errorMessage = "Error!",
                    )
                    FormBuilderDropdown(
                        label = "Dropdown",
                        selectedOption = "",
                        options = listOf("A"),
                        onOptionSelected = {},
                        isError = true,
                        errorMessage = "Error!",
                    )
                    FormBuilderMultiSelectDropdown(
                        label = "MultiSelect",
                        selectedOptions = emptyList(),
                        options = listOf("A"),
                        onSelectionChanged = {},
                        isError = true,
                        errorMessage = "Error!",
                    )
                }
            }

            val hasErrorMatcher =
                SemanticsMatcher("has error text") { node ->
                    node.config.getOrNull(SemanticsProperties.Error) == "Error!"
                }

            onNodeWithTag("TextInput Text Input").assertExists()
            onNodeWithTag("TextArea Text Area").assertExists()
            onNodeWithTag("NumericInput Numeric Input").assertExists()
            onNodeWithTag("Dropdown Dropdown").assertExists()
            onNodeWithTag("MultiSelectDropdown MultiSelect").assertExists()

            onNodeWithTag("TextInput Text Input").assert(hasErrorMatcher)
            onNodeWithTag("TextArea Text Area").assert(hasErrorMatcher)
            onNodeWithTag("NumericInput Numeric Input").assert(hasErrorMatcher)

            onNodeWithTag("Switch Switch").assert(hasErrorMatcher)
            onNodeWithTag("CheckboxRow Checkbox").assert(hasErrorMatcher)
        }

    /**
     * Tests FormBuilderRangeSlider visual numeric readout and accessibility state description.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderRangeSliderA11yAndReadout() =
        runComposeUiTest {
            var sliderVal = 5.0f
            setContent {
                FormBuilderRangeSlider(
                    value = sliderVal,
                    valueRange = 0f..10f,
                    onValueChange = { sliderVal = it },
                    label = "Pain Scale",
                    isError = true,
                    errorMessage = "Value out of range",
                )
            }

            onNodeWithTag("RangeSlider Pain Scale").assertExists()
            onNodeWithTag("SliderControl Pain Scale").assertExists()
            onNodeWithText("Pain Scale").assertExists()
            onNodeWithText("Value out of range").assertExists()

            val hasStateDescMatcher =
                SemanticsMatcher("has state description") { node ->
                    node.config.getOrNull(SemanticsProperties.StateDescription) != null
                }
            onNodeWithTag("SliderControl Pain Scale").assert(hasStateDescMatcher)
        }

    /**
     * Tests FormBuilderPhotoCamera and FormBuilderVideoCamera buttons.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderCameraButtons() =
        runComposeUiTest {
            var photoClicked = false
            var videoClicked = false
            setContent {
                Column {
                    FormBuilderPhotoCamera(
                        label = "Take Skin Photo",
                        onClick = { photoClicked = true },
                    )
                    FormBuilderVideoCamera(
                        label = "Record Video",
                        onClick = { videoClicked = true },
                    )
                }
            }

            onNodeWithTag("PhotoCamera Take Skin Photo").assertExists().performClick()
            assertTrue(photoClicked)

            onNodeWithTag("VideoCamera Record Video").assertExists().performClick()
            assertTrue(videoClicked)
        }

    /**
     * Tests FormBuilderNumericInput decimal input with both comma and dot.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderNumericInputComma() =
        runComposeUiTest {
            var numVal = ""
            setContent {
                FormBuilderNumericInput(
                    label = "Weight",
                    value = numVal,
                    onValueChange = { numVal = it },
                )
            }

            onNodeWithTag("NumericInput Weight").performTextInput("12,5")
            assertEquals("12,5", numVal)
        }
}
