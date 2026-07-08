package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.date_format_label
import chartcam.chartcam.generated.resources.datetime_format_label
import org.jetbrains.compose.resources.stringResource

/**
 * A Material 3 single-line text input field.
 *
 * @param value The current value of the input.
 * @param onValueChange Callback invoked when the input value changes.
 * @param label The label text displayed for the input field.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth().testTag("TextInput $label"),
    )
}

/**
 * A Material 3 multi-line text input field.
 *
 * @param value The current value of the input.
 * @param onValueChange Callback invoked when the input value changes.
 * @param label The label text displayed for the input field.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = 3,
        modifier = modifier.fillMaxWidth().testTag("TextArea $label"),
    )
}

/**
 * A Material 3 switch (toggle) widget.
 *
 * @param checked The current checked state of the switch.
 * @param onCheckedChange Callback invoked when the state changes.
 * @param label The label text displayed for the switch.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = Role.Switch,
                ).padding(vertical = 8.dp)
                .testTag("Switch $label"),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.testTag("Toggle $label"),
        )
        Text(text = label, modifier = Modifier.padding(start = 16.dp))
    }
}

/**
 * A Material 3 checkbox widget.
 *
 * @param checked The current checked state of the checkbox.
 * @param onCheckedChange Callback invoked when the state changes.
 * @param label The label text displayed for the checkbox.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = Role.Checkbox,
                ).padding(vertical = 8.dp)
                .testTag("CheckboxRow $label"),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.testTag("Checkbox $label"),
        )
        Text(text = label, modifier = Modifier.padding(start = 16.dp))
    }
}

/**
 * A Material 3 numeric input field.
 *
 * @param value The current value of the input.
 * @param onValueChange Callback invoked when the numeric value changes.
 * @param label The label text displayed for the input field.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderNumericInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newVal ->
            if (newVal.isEmpty() || newVal.all { it.isDigit() || it == '.' }) {
                onValueChange(newVal)
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth().testTag("NumericInput $label"),
    )
}

/**
 * A Material 3 range slider widget.
 *
 * @param value The current value of the slider.
 * @param valueRange The valid range of values.
 * @param onValueChange Callback invoked when the value changes.
 * @param label The label text displayed for the slider.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderRangeSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = 8.dp).testTag("RangeSlider $label")) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth().testTag("SliderControl $label"),
        )
    }
}

/**
 * A Material 3 single-select dropdown widget.
 *
 * @param selectedOption The currently selected option.
 * @param options The list of available options.
 * @param onOptionSelected Callback invoked when an option is selected.
 * @param label The label text displayed for the dropdown.
 * @param modifier The modifier to be applied to the widget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormBuilderDropdown(
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("Dropdown $label"),
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    modifier = Modifier.testTag("Option $option"),
                )
            }
        }
    }
}

/**
 * A Material 3 multi-select dropdown widget.
 *
 * @param selectedOptions The list of currently selected options.
 * @param options The list of available options.
 * @param onSelectionChanged Callback invoked when the selection changes.
 * @param label The label text displayed for the dropdown.
 * @param modifier The modifier to be applied to the widget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormBuilderMultiSelectDropdown(
    selectedOptions: List<String>,
    options: List<String>,
    onSelectionChanged: (List<String>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = if (selectedOptions.isEmpty()) "" else selectedOptions.joinToString(", ")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("MultiSelectDropdown $label"),
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                val isSelected = selectedOptions.contains(option)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Text(text = option, modifier = Modifier.padding(start = 8.dp))
                        }
                    },
                    onClick = {
                        val newList = if (isSelected) selectedOptions - option else selectedOptions + option
                        onSelectionChanged(newList)
                    },
                    modifier = Modifier.testTag("MultiSelectOption $option"),
                )
            }
        }
    }
}

/**
 * A Material 3 Date Picker widget mock.
 *
 * @param value The current date string.
 * @param onValueChange Callback invoked when the date changes.
 * @param label The label text displayed for the picker.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderDatePicker(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.date_format_label, label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth().testTag("DatePicker $label"),
    )
}

/**
 * A Material 3 DateTime Picker widget mock.
 *
 * @param value The current datetime string.
 * @param onValueChange Callback invoked when the datetime changes.
 * @param label The label text displayed for the picker.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderDateTimePicker(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.datetime_format_label, label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = modifier.fillMaxWidth().testTag("DateTimePicker $label"),
    )
}

/**
 * A Material 3 Camera widget for Photos.
 *
 * @param label The instructional label for the user (e.g. "Take photo of left ear").
 * @param onClick Callback when the camera button is clicked.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderPhotoCamera(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("PhotoCamera $label"),
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * A Material 3 Camera widget for Videos.
 *
 * @param label The instructional label for the user (e.g. "Take video of face").
 * @param onClick Callback when the camera button is clicked.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderVideoCamera(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("VideoCamera $label"),
    ) {
        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
