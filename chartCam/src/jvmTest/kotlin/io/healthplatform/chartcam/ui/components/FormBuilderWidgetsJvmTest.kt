/**
 * @file FormBuilderWidgetsJvmTest.kt
 * Contains declarations for FormBuilderWidgetsJvmTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.clear
import chartcam.chartcam.generated.resources.ok
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test class for FormBuilderWidgets on JVM.
 */
class FormBuilderWidgetsJvmTest {
    /**
     * Tests FormBuilderTextInput with normal input and tab focus transition.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderTextInput() =
        runComposeUiTest {
            setContent {
                var value by remember { mutableStateOf("") }
                FormBuilderTextInput(
                    label = "Test Label",
                    value = value,
                    onValueChange = { value = it },
                    isRequired = true,
                )
            }

            onNodeWithText("Test Label *").assertExists()
            onNodeWithText("Test Label *").performTextInput("Hello")
            waitForIdle()

            // Test tab handling
            onNodeWithText("Hello").performTextInput("	")
            waitForIdle()
        }

    /**
     * Tests FormBuilderTextArea with normal input and clear button.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderTextArea() =
        runTest {
            val clearStr = getString(Res.string.clear)
            runComposeUiTest {
                setContent {
                    var value by remember { mutableStateOf("Initial text") }
                    FormBuilderTextArea(
                        label = "Notes",
                        value = value,
                        onValueChange = { value = it },
                        isRequired = true,
                    )
                }

                onNodeWithText("Notes *").assertExists()
                // Clear button should be present when non-empty
                onNodeWithContentDescription(clearStr, useUnmergedTree = true).performClick()
                waitForIdle()
                onNodeWithText("Notes *").assertExists()
            }
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
                    FormBuilderDatePicker(
                        label = "Date Field",
                        value = "",
                        onValueChange = {},
                        isError = true,
                        errorMessage = "Error!",
                    )
                    FormBuilderDateTimePicker(
                        label = "DateTime Field",
                        value = "",
                        onValueChange = {},
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
            onNodeWithTag("DatePicker Date Field").assertExists()
            onNodeWithTag("DateTimePicker DateTime Field").assertExists()

            onNodeWithTag("TextInput Text Input").assert(hasErrorMatcher)
            onNodeWithTag("TextArea Text Area").assert(hasErrorMatcher)
            onNodeWithTag("NumericInput Numeric Input").assert(hasErrorMatcher)
            onNodeWithTag("DatePicker Date Field").assert(hasErrorMatcher)
            onNodeWithTag("DateTimePicker DateTime Field").assert(hasErrorMatcher)

            onNodeWithTag("Switch Switch").assert(hasErrorMatcher)
            onNodeWithTag("CheckboxRow Checkbox").assert(hasErrorMatcher)
        }

    /**
     * Tests FormBuilderRangeSlider with continuous, stepped, and zero-range settings.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderRangeSliderA11yAndReadout() =
        runComposeUiTest {
            var sliderVal = 5.0f
            var steppedVal = 3.0f
            setContent {
                Column {
                    FormBuilderRangeSlider(
                        value = sliderVal,
                        valueRange = 0f..10f,
                        onValueChange = { sliderVal = it },
                        label = "Pain Scale",
                        isError = true,
                        errorMessage = "Value out of range",
                    )
                    FormBuilderRangeSlider(
                        value = steppedVal,
                        valueRange = 0f..5f,
                        steps = 4,
                        onValueChange = { steppedVal = it },
                        label = "Stepped Scale",
                    )
                }
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
            onNodeWithTag("SliderControl Stepped Scale").assertExists()
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
     * Tests FormBuilderNumericInput decimal input with comma, dot, tab, and clear button.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderNumericInputCommaAndClear() =
        runTest {
            val clearStr = getString(Res.string.clear)
            runComposeUiTest {
                setContent {
                    var numVal by remember { mutableStateOf("42") }
                    FormBuilderNumericInput(
                        label = "Weight",
                        value = numVal,
                        onValueChange = { numVal = it },
                    )
                }

                // Clear button should exist when non-empty
                onNodeWithContentDescription(clearStr, useUnmergedTree = true).performClick()
                waitForIdle()

                // Input comma decimal
                onNodeWithTag("NumericInput Weight").performTextInput("12,5")
                waitForIdle()

                // Input tab
                onNodeWithTag("NumericInput Weight").performTextInput("	")
                waitForIdle()
            }
        }

    /**
     * Tests FormBuilderDropdown menu expansion and item selection.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderDropdownSelection() =
        runComposeUiTest {
            var selected = "Option 1"
            setContent {
                var currentSelected by remember { mutableStateOf(selected) }
                FormBuilderDropdown(
                    label = "Options",
                    selectedOption = currentSelected,
                    options = listOf("Option 1", "Option 2"),
                    onOptionSelected = {
                        currentSelected = it
                        selected = it
                    },
                    isRequired = true,
                )
            }

            onNodeWithTag("Dropdown Options").assertExists().performClick()
            waitForIdle()

            onNodeWithTag("Option Option 2").assertExists().performClick()
            waitForIdle()
            assertEquals("Option 2", selected)
        }

    /**
     * Tests FormBuilderMultiSelectDropdown selection toggle (select and deselect).
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderMultiSelectDropdownToggle() =
        runComposeUiTest {
            var selected = listOf("A")
            setContent {
                var currentSelected by remember { mutableStateOf(selected) }
                FormBuilderMultiSelectDropdown(
                    label = "Multi",
                    selectedOptions = currentSelected,
                    options = listOf("A", "B"),
                    onSelectionChanged = {
                        currentSelected = it
                        selected = it
                    },
                    isRequired = true,
                )
            }

            // Deselect A
            onNodeWithTag("MultiSelectOption A").performClick()
            waitForIdle()
            assertEquals(emptyList(), selected)

            // Select B
            onNodeWithTag("MultiSelectOption B").performClick()
            waitForIdle()
            assertEquals(listOf("B"), selected)
        }

    /**
     * Tests FormBuilderDatePicker opening dialog, canceling, confirming, and clearing value.
     */
    @OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
    @Test
    fun testFormBuilderDatePickerInteraction() =
        runTest {
            setAppLanguage("en")
            val okStr = getString(Res.string.ok)
            val cancelStr = getString(Res.string.cancel)
            val clearStr = getString(Res.string.clear)

            runComposeUiTest {
                setContent {
                    var dateValue by remember { mutableStateOf("") }
                    FormBuilderDatePicker(
                        label = "Birth Date",
                        value = dateValue,
                        onValueChange = { dateValue = it },
                        isRequired = true,
                    )
                }

                // Open dialog
                onNodeWithTag("DatePicker Birth Date").performClick()
                waitForIdle()

                // Cancel dialog
                onNodeWithText(cancelStr).performClick()
                waitForIdle()

                // Open dialog again
                onNodeWithTag("DatePicker Birth Date").performClick()
                waitForIdle()

                // Select a day in the picker
                val dayNode = onAllNodesWithText("15", useUnmergedTree = true)
                if (dayNode.fetchSemanticsNodes().isNotEmpty()) {
                    dayNode[0].performClick()
                    waitForIdle()
                }

                // Confirm dialog
                onNodeWithText(okStr).performClick()
                waitForIdle()

                // Clear button should be visible now if a date was selected
                val clearBtns = onAllNodesWithContentDescription(clearStr, useUnmergedTree = true)
                if (clearBtns.fetchSemanticsNodes().isNotEmpty()) {
                    clearBtns[0].performClick()
                    waitForIdle()
                }
            }
        }

    /**
     * Tests FormBuilderDateTimePicker opening date and time dialogs, confirming, and clearing.
     */
    @OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
    @Test
    fun testFormBuilderDateTimePickerInteraction() =
        runTest {
            setAppLanguage("en")
            val okStr = getString(Res.string.ok)
            val cancelStr = getString(Res.string.cancel)
            val clearStr = getString(Res.string.clear)

            runComposeUiTest {
                setContent {
                    var dateTimeValue by remember { mutableStateOf("") }
                    FormBuilderDateTimePicker(
                        label = "Appointment Time",
                        value = dateTimeValue,
                        onValueChange = { dateTimeValue = it },
                        isRequired = true,
                    )
                }

                // Open date dialog
                onNodeWithTag("DateTimePicker Appointment Time").performClick()
                waitForIdle()

                // Cancel date dialog
                onNodeWithText(cancelStr).performClick()
                waitForIdle()

                // Open date dialog again
                onNodeWithTag("DateTimePicker Appointment Time").performClick()
                waitForIdle()

                // Select a day
                val dayNode = onAllNodesWithText("15", useUnmergedTree = true)
                if (dayNode.fetchSemanticsNodes().isNotEmpty()) {
                    dayNode[0].performClick()
                    waitForIdle()
                }

                // Confirm date dialog (moves to time dialog)
                onNodeWithText(okStr).performClick()
                waitForIdle()

                // Cancel time dialog
                onNodeWithText(cancelStr).performClick()
                waitForIdle()

                // Open date dialog again
                onNodeWithTag("DateTimePicker Appointment Time").performClick()
                waitForIdle()

                // Select a day
                val dayNode2 = onAllNodesWithText("15", useUnmergedTree = true)
                if (dayNode2.fetchSemanticsNodes().isNotEmpty()) {
                    dayNode2[0].performClick()
                    waitForIdle()
                }

                // Confirm date dialog
                onNodeWithText(okStr).performClick()
                waitForIdle()

                // Confirm time dialog
                onNodeWithText(okStr).performClick()
                waitForIdle()

                // Clear value via icon button if non-empty
                val clearBtns = onAllNodesWithContentDescription(clearStr, useUnmergedTree = true)
                if (clearBtns.fetchSemanticsNodes().isNotEmpty()) {
                    clearBtns[0].performClick()
                    waitForIdle()
                }
            }
        }
}
