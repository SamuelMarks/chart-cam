/**
 * Provides the CreatePatientDialog component for creating new patients.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * Dialog for creating a new Patient.
 * Collects first name, last name, MRN, Date of Birth, and gender.
 * Provides validation for all fields and date format.
 *
 * @param onDismissRequest Callback invoked when the user dismisses the dialog without saving.
 * @param onConfirm Callback invoked when the user successfully saves the new patient data. It provides the following parameters: first name, last name, MRN, date of birth, and gender.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePatientDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, String, String, LocalDate, String) -> Unit,
) {
    /**
     * Stores the user's input for the patient's first name.
     */
    var firstName by remember { mutableStateOf("") }

    /**
     * Stores the user's input for the patient's last name.
     */
    var lastName by remember { mutableStateOf("") }

    /**
     * Stores the user's input for the patient's Medical Record Number (MRN).
     */
    var mrn by remember { mutableStateOf("") }

    /**
     * Stores the raw string input for the patient's date of birth (expected format: YYYY-MM-DD).
     */
    var dobString by remember { mutableStateOf("") }

    /**
     * Stores the selected gender for the new patient. Defaults to "unknown".
     */
    var gender by remember { mutableStateOf("unknown") }

    /**
     * Holds any validation error message to be displayed to the user. Null if there is no error.
     */
    var error by remember { mutableStateOf<String?>(null) }

    /**
     * Controls the visibility of the DatePicker dialog (currently unused as text input is used).
     */
    var showDatePicker by remember { mutableStateOf(false) }

    /**
     * Manages the state and validation logic for the DatePicker component.
     */
    val datePickerState =
        rememberDatePickerState(
            selectableDates =
                object : SelectableDates {
                    /**
                     * Determines if a specific date can be selected by the user.
                     *
                     * @param utcTimeMillis The UTC time in milliseconds of the date being checked.
                     * @return True if the date is in the past or present, false if it's in the future.
                     */
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        utcTimeMillis <=
                            kotlin.time.Clock.System
                                .now()
                                .toEpochMilliseconds()
                },
        )

    /**
     * The focus manager instance used to navigate between input fields programmatically.
     */
    val focusManager = LocalFocusManager.current

    /**
     * Localized error message to display when required fields are empty.
     */
    val errorAllFields = stringResource(Res.string.all_fields_required)

    /**
     * Localized error message to display when the date of birth is improperly formatted.
     */
    val errorInvalidDate = stringResource(Res.string.invalid_date_format)

    /**
     * Lambda function invoked to validate form input and submit the data if validation passes.
     */
    val submitForm = {
        if (firstName.isBlank() || lastName.isBlank() || mrn.isBlank()) {
            error = errorAllFields
        } else {
            val dob =
                try {
                    LocalDate.parse(dobString.trim())
                } catch (e: Exception) {
                    null
                }
            if (dob == null) {
                error = errorInvalidDate
            } else {
                onConfirm(firstName, lastName, mrn, dob, gender)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.new_patient)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text(stringResource(Res.string.first_name)) },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth().onPreviewKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                true
                            } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            } else {
                                false
                            }
                        },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text(stringResource(Res.string.last_name)) },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth().onPreviewKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                true
                            } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            } else {
                                false
                            }
                        },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                )
                OutlinedTextField(
                    value = mrn,
                    onValueChange = { mrn = it },
                    label = { Text(stringResource(Res.string.mrn)) },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth().onPreviewKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                true
                            } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            } else {
                                false
                            }
                        },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                )
                OutlinedTextField(
                    value = dobString,
                    onValueChange = { dobString = it },
                    label = { Text(stringResource(Res.string.dob_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            submitForm()
                        }),
                    modifier =
                        Modifier.fillMaxWidth().onPreviewKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                true
                            } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                focusManager.clearFocus()
                                submitForm()
                                true
                            } else {
                                false
                            }
                        },
                    placeholder = { Text(stringResource(Res.string.dob_placeholder)) },
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitForm()
                },
            ) {
                Text(stringResource(Res.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
