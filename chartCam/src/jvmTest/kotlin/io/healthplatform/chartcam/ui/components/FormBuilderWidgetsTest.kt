package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormBuilderWidgetsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testTextInput() {
        var result = ""
        rule.setContent {
            FormBuilderTextInput(value = "", onValueChange = { result = it }, label = "MyText")
        }
        rule.onNodeWithTag("TextInput MyText").assertIsDisplayed()
        rule.onNodeWithTag("TextInput MyText").performTextInput("Hello")
        assertEquals("Hello", result)
    }

    @Test
    fun testTextArea() {
        var result = ""
        rule.setContent {
            FormBuilderTextArea(value = "", onValueChange = { result = it }, label = "MyArea")
        }
        rule.onNodeWithTag("TextArea MyArea").assertIsDisplayed()
        rule.onNodeWithTag("TextArea MyArea").performTextInput("World")
        assertEquals("World", result)
    }

    @Test
    fun testSwitch() {
        var result = false
        rule.setContent {
            FormBuilderSwitch(checked = false, onCheckedChange = { result = it }, label = "MySwitch")
        }
        rule.onNodeWithTag("Switch MySwitch").assertIsDisplayed()
        rule.onNodeWithTag("Switch MySwitch").performClick()
        assertTrue(result)
    }

    @Test
    fun testCheckbox() {
        var result = false
        rule.setContent {
            FormBuilderCheckbox(checked = false, onCheckedChange = { result = it }, label = "MyCheck")
        }
        rule.onNodeWithTag("CheckboxRow MyCheck").assertIsDisplayed()
        rule.onNodeWithTag("CheckboxRow MyCheck").performClick()
        assertTrue(result)
    }

    @Test
    fun testNumericInput() {
        var result = ""
        rule.setContent {
            FormBuilderNumericInput(value = "", onValueChange = { result = it }, label = "MyNum")
        }
        rule.onNodeWithTag("NumericInput MyNum").assertIsDisplayed()
        rule.onNodeWithTag("NumericInput MyNum").performTextInput("123.45")
        assertEquals("123.45", result)
    }

    @Test
    fun testRangeSlider() {
        var result = 0f
        rule.setContent {
            FormBuilderRangeSlider(value = 0f, valueRange = 0f..100f, onValueChange = { result = it }, label = "MySlider")
        }
        rule.onNodeWithTag("SliderControl MySlider").assertIsDisplayed()
    }

    @Test
    fun testDropdown() {
        var result = ""
        rule.setContent {
            FormBuilderDropdown(selectedOption = "A", options = listOf("A", "B"), onOptionSelected = { result = it }, label = "MyDrop")
        }
        rule.onNodeWithTag("Dropdown MyDrop").assertIsDisplayed()
    }

    @Test
    fun testMultiSelectDropdown() {
        var result = emptyList<String>()
        rule.setContent {
            FormBuilderMultiSelectDropdown(selectedOptions = emptyList(), options = listOf("A", "B"), onSelectionChanged = {
                result = it
            }, label = "MyMultiDrop")
        }
        rule.onNodeWithTag("MultiSelectDropdown MyMultiDrop").assertIsDisplayed()
    }

    @Test
    fun testDatePicker() {
        var result = ""
        rule.setContent {
            FormBuilderDatePicker(value = "", onValueChange = { result = it }, label = "MyDate")
        }
        rule.onNodeWithTag("DatePicker MyDate").assertIsDisplayed()
        rule.onNodeWithTag("DatePicker MyDate").performTextInput("2024-01-01")
        assertEquals("2024-01-01", result)
    }

    @Test
    fun testDateTimePicker() {
        var result = ""
        rule.setContent {
            FormBuilderDateTimePicker(value = "", onValueChange = { result = it }, label = "MyDT")
        }
        rule.onNodeWithTag("DateTimePicker MyDT").assertIsDisplayed()
        rule.onNodeWithTag("DateTimePicker MyDT").performTextInput("2024-01-01 12:00")
        assertEquals("2024-01-01 12:00", result)
    }

    @Test
    fun testCameras() {
        var photoClicked = false
        var videoClicked = false
        rule.setContent {
            Column {
                FormBuilderPhotoCamera(label = "Photo", onClick = { photoClicked = true })
                FormBuilderVideoCamera(label = "Video", onClick = { videoClicked = true })
            }
        }
        rule.onNodeWithTag("PhotoCamera Photo").performClick()
        assertTrue(photoClicked)
        rule.onNodeWithTag("VideoCamera Video").performClick()
        assertTrue(videoClicked)
    }
}
