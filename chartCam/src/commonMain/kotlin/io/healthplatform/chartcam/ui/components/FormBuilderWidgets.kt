/**
 * @file FormBuilderWidgets.kt
 * Contains declarations for form input widgets used in Questionnaires (e.g. TextInput, Switch, Slider).
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.cd_select_date
import chartcam.chartcam.generated.resources.cd_select_datetime
import chartcam.chartcam.generated.resources.clear
import chartcam.chartcam.generated.resources.date_format_label
import chartcam.chartcam.generated.resources.datetime_format_label
import chartcam.chartcam.generated.resources.not_answered
import chartcam.chartcam.generated.resources.ok
import chartcam.chartcam.generated.resources.select_time
import chartcam.chartcam.generated.resources.state_selected
import chartcam.chartcam.generated.resources.state_unselected
import io.healthplatform.chartcam.utils.formatLocalizedDecimal
import io.healthplatform.chartcam.utils.getLocalizedDatePattern
import io.healthplatform.chartcam.utils.getLocalizedDateTimePattern
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * A Material 3 single-line text input field.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param value The current value of the input.
 * @param onValueChange Callback invoked when the input value changes.
 * @param label The label text displayed for the input field.
 * @param isRequired Whether the field is required.
 * @param isError Whether the field is in an error state.
 * @param errorMessage The error message to display when in an error state.
 * @param keyboardOptions Keyboard options.
 * @param keyboardActions Keyboard actions.
 * @param modifier The modifier to be applied to the widget.
 */
@Composable
@Suppress("LongParameterList")
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
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = {
            if (it.contains("\t")) {
                onValueChange(it.replace("\t", ""))
                focusManager.moveFocus(FocusDirection.Next)
            } else {
                onValueChange(it)
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
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier =
            modifier
                .fillMaxWidth()
                .tabFocusNext(focusManager)
                .semantics {
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                    }
                }.testTag("TextInput $label"),
    )
}

/**
 * A Material 3 multi-line text input field.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
@Suppress("LongParameterList")
fun FormBuilderTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
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
        modifier =
            modifier
                .fillMaxWidth()
                .tabFocusNext(focusManager)
                .semantics {
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                    }
                }.testTag("TextArea $label"),
    )
}

/**
 * A Material 3 switch (toggle) widget.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
@Suppress("LongParameterList", "UnusedParameter")
fun FormBuilderSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val selectedText = stringResource(Res.string.state_selected)
    val unselectedText = stringResource(Res.string.state_unselected)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    stateDescription = if (checked) selectedText else unselectedText
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                    }
                }.toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = Role.Switch,
                ).padding(vertical = 8.dp)
                .minimumInteractiveComponentSize()
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
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
@Suppress("LongParameterList", "UnusedParameter")
fun FormBuilderCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val selectedText = stringResource(Res.string.state_selected)
    val unselectedText = stringResource(Res.string.state_unselected)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    stateDescription = if (checked) selectedText else unselectedText
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                    }
                }.toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = Role.Checkbox,
                ).padding(vertical = 8.dp)
                .minimumInteractiveComponentSize()
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
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
@Suppress("LongParameterList")
fun FormBuilderNumericInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = { newVal ->
            if (newVal.contains("\t")) {
                onValueChange(newVal.replace("\t", ""))
                focusManager.moveFocus(FocusDirection.Next)
                return@OutlinedTextField
            }
            if (newVal.isEmpty() || newVal.matches(Regex("^\\d*[.,]?\\d*$"))) {
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
        modifier =
            modifier
                .fillMaxWidth()
                .tabFocusNext(focusManager)
                .semantics {
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                    }
                }.testTag("NumericInput $label"),
    )
}

/**
 * A Material 3 range slider widget.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
@Suppress("LongParameterList")
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
    val currentLang by io.healthplatform.chartcam.ui.currentLanguageState
        .collectAsState()
    val formattedValue = formatLocalizedDecimal(value.toDouble(), currentLang, decimalPlaces = 1)
    Column(
        modifier =
            modifier
                .padding(vertical = 8.dp)
                .minimumInteractiveComponentSize()
                .semantics {
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                    }
                }.testTag("RangeSlider $label"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormLabel(text = label, isRequired = isRequired)
            Text(
                text = formattedValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = label
                        stateDescription = formattedValue
                    }.testTag("SliderControl $label"),
        )
        if (isError && errorMessage != null) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * A Material 3 single-select dropdown widget.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
@Suppress("LongParameterList")
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
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .minimumInteractiveComponentSize()
                .testTag("Dropdown $label"),
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
            modifier =
                Modifier
                    .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
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
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
@Suppress("LongParameterList")
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
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .minimumInteractiveComponentSize()
                .semantics {
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                    }
                }.testTag("MultiSelectDropdown $label"),
    ) {
        FormLabel(label, isRequired, modifier = Modifier.padding(bottom = 4.dp))
        if (isError && errorMessage != null) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier =
                    Modifier
                        .padding(bottom = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
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
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
@Suppress("LongParameterList", "LongMethod")
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
                        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
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

    val currentLang by io.healthplatform.chartcam.ui.currentLanguageState
        .collectAsState()
    val displayValue =
        if (value.isNotEmpty()) {
            io.healthplatform.chartcam.utils
                .formatLocalizedDate(value, currentLang)
        } else {
            ""
        }
    val notAnsweredText = stringResource(Res.string.not_answered)
    val selectDateLabel = stringResource(Res.string.cd_select_date)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = selectDateLabel) {
                        showDialog = true
                    }.semantics {
                        contentDescription =
                            "$label: ${if (displayValue.isNotEmpty()) displayValue else notAnsweredText}"
                        if (isError && errorMessage != null) {
                            error(errorMessage)
                        }
                    }.testTag("DatePicker $label"),
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = { },
                readOnly = true,
                enabled = false,
                label = {
                    val pattern = getLocalizedDatePattern(currentLang)
                    val patternLabel = stringResource(Res.string.date_format_label, label, pattern)
                    FormLabel(patternLabel, isRequired)
                },
                isError = isError,
                supportingText = { if (isError && errorMessage != null) Text(errorMessage) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
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
        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.padding(start = 4.dp).minimumInteractiveComponentSize(),
            ) {
                Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.clear))
            }
        }
    }
}

/**
 * A Material 3 DateTime Picker widget mock.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
@Suppress("LongParameterList", "LongMethod")
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
                        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
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
            title = {
                Text(
                    stringResource(Res.string.select_time),
                    modifier = Modifier.semantics { heading() },
                )
            },
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

    val currentLang by io.healthplatform.chartcam.ui.currentLanguageState
        .collectAsState()
    val displayValue =
        if (value.isNotEmpty()) {
            io.healthplatform.chartcam.utils
                .formatLocalizedDate(value, currentLang)
        } else {
            ""
        }
    val notAnsweredText = stringResource(Res.string.not_answered)
    val selectDateTimeLabel = stringResource(Res.string.cd_select_datetime)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = selectDateTimeLabel) {
                        showDateDialog = true
                    }.semantics {
                        contentDescription =
                            "$label: ${if (displayValue.isNotEmpty()) displayValue else notAnsweredText}"
                        if (isError && errorMessage != null) {
                            error(errorMessage)
                        }
                    }.testTag("DateTimePicker $label"),
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = { },
                readOnly = true,
                enabled = false,
                label = {
                    val pattern = getLocalizedDateTimePattern(currentLang)
                    val patternLabel = stringResource(Res.string.datetime_format_label, label, pattern)
                    FormLabel(patternLabel, isRequired)
                },
                isError = isError,
                supportingText = { if (isError && errorMessage != null) Text(errorMessage) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
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
        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.padding(start = 4.dp).minimumInteractiveComponentSize(),
            ) {
                Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.clear))
            }
        }
    }
}

/**
 * A Material 3 Camera widget for Photos.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .minimumInteractiveComponentSize()
                .testTag("PhotoCamera $label"),
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * A Material 3 Camera widget for Videos.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
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
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .minimumInteractiveComponentSize()
                .testTag("VideoCamera $label"),
    ) {
        Icon(
            Icons.Default.Videocam,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
