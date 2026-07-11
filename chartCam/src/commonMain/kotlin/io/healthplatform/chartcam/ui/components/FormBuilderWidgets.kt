package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.clear
import chartcam.chartcam.generated.resources.date_format_label
import chartcam.chartcam.generated.resources.datetime_format_label
import chartcam.chartcam.generated.resources.ok
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * A Material 3 single-line text input field.
 *
 * @param value The current value of the input.
 * @param onValueChange Callback invoked when the input value changes.
 * @param label The label text displayed for the input field.
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { FormLabel(label, isRequired) },
        isError = isError,
        supportingText = { if (isError && errorMessage != null) Text(errorMessage) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth().testTag("TextInput $label"),
    )
}

/**
 * A Material 3 multi-line text input field.
 *
 * @param value The current value of the input.
 * @param onValueChange Callback invoked when the input value changes.
 * @param label The label text displayed for the input field.
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { FormLabel(label, isRequired) },
        isError = isError,
        supportingText = { if (isError && errorMessage != null) Text(errorMessage) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.clear))
                }
            }
        },
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
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
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
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
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
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderNumericInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newVal ->
            if (newVal.isEmpty() || newVal.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(newVal)
            }
        },
        label = { FormLabel(label, isRequired) },
        isError = isError,
        supportingText = { if (isError && errorMessage != null) Text(errorMessage) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.clear))
                }
            }
        },
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
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
fun FormBuilderRangeSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
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
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
 * @param modifier The modifier to be applied to the widget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormBuilderDropdown(
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
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
            label = { FormLabel(label, isRequired) },
            isError = isError,
            supportingText = { if (isError && errorMessage != null) Text(errorMessage) },
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
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
 * @param modifier The modifier to be applied to the widget.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormBuilderMultiSelectDropdown(
    selectedOptions: List<String>,
    options: List<String>,
    onSelectionChanged: (List<String>) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("MultiSelectDropdown $label")) {
        FormLabel(label, isRequired, modifier = Modifier.padding(bottom = 4.dp))
        if (isError && errorMessage != null) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(8.dp),
            verticalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val isSelected = selectedOptions.contains(option)
                InputChip(
                    selected = isSelected,
                    onClick = {
                        val newList = if (isSelected) selectedOptions - option else selectedOptions + option
                        onSelectionChanged(newList)
                    },
                    label = { Text(option) },
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
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
 * @param modifier The modifier to be applied to the widget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormBuilderDatePicker(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
                        val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
                        val localDate = instant.toLocalDateTime(tz).date
                        onValueChange(localDate.toString())
                    }
                    showDialog = false
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxWidth().clickable { showDialog = true }) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            enabled = false,
            label = { FormLabel(stringResource(Res.string.date_format_label, label), isRequired) },
            isError = isError,
            supportingText = { if (isError && errorMessage != null) Text(errorMessage) },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.clear))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("DatePicker $label"),
            colors =
                androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
    }
}

/**
 * A Material 3 DateTime Picker widget mock.
 *
 * @param value The current datetime string.
 * @param onValueChange Callback invoked when the datetime changes.
 * @param label The label text displayed for the picker.
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
 * @param modifier The modifier to be applied to the widget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormBuilderDateTimePicker(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    var showDateDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    var selectedDateStr by remember { mutableStateOf<String?>(null) }

    if (showDateDialog) {
        DatePickerDialog(
            onDismissRequest = { showDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
                        val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
                        val localDate = instant.toLocalDateTime(tz).date
                        selectedDateStr = localDate.toString()
                        showDateDialog = false
                        showTimeDialog = true
                    }
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimeDialog) {
        AlertDialog(
            onDismissRequest = { showTimeDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val minute = timePickerState.minute.toString().padStart(2, '0')
                    if (selectedDateStr != null) {
                        onValueChange("${selectedDateStr}T$hour:$minute:00Z")
                    }
                    showTimeDialog = false
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxWidth().clickable { showDateDialog = true }) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            enabled = false,
            label = { FormLabel(stringResource(Res.string.datetime_format_label, label), isRequired) },
            isError = isError,
            supportingText = { if (isError && errorMessage != null) Text(errorMessage) },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.clear))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("DateTimePicker $label"),
            colors =
                androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
    }
}

/**
 * A Material 3 Camera widget for Photos.
 *
 * @param label The instructional label for the user (e.g. "Take photo of left ear").
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
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
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
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
