package io.healthplatform.chartcam.ui.components

import org.jetbrains.compose.resources.stringResource
import chartcam.chartcam.generated.resources.*

import org.jetbrains.compose.resources.stringResource
import chartcam.chartcam.generated.resources.*


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

/**
 * Dialog for creating a new Patient.
 *
 * @param onDismissRequest Callback when dialog is dismissed.
 * @param onConfirm Callback with the new data(firstName, lastName, mrn, dob, gender).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePatientDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, String, String, LocalDate, String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var mrn by remember { mutableStateOf("") }
    
    // For simplicity in KMP without specific DatePicker library versions, using String input YYYY-MM-DD

    var dobString by remember { mutableStateOf("") } 
    var gender by remember { mutableStateOf("unknown") }
    var error by remember { mutableStateOf<String?>(null) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= kotlin.time.Clock.System.now().toEpochMilliseconds()
            }
        }
    )

    
    val focusManager = LocalFocusManager.current
    val errorAllFields = stringResource(Res.string.all_fields_required)
    val errorInvalidDate = stringResource(Res.string.invalid_date_format)
    
    val submitForm = {
        if (firstName.isBlank() || lastName.isBlank() || mrn.isBlank()) {
            error = errorAllFields
        } else {
            val dob = try {
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
                    modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { 
                        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                            true
                        } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                            focusManager.moveFocus(FocusDirection.Next)
                            true
                        } else false
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text(stringResource(Res.string.last_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { 
                        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                            true
                        } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                            focusManager.moveFocus(FocusDirection.Next)
                            true
                        } else false
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
                OutlinedTextField(
                    value = mrn,
                    onValueChange = { mrn = it },
                    label = { Text(stringResource(Res.string.mrn)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { 
                        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                            true
                        } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                            focusManager.moveFocus(FocusDirection.Next)
                            true
                        } else false
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
                OutlinedTextField(
                    value = dobString,
                    onValueChange = { dobString = it },
                    label = { Text(stringResource(Res.string.dob_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        submitForm()
                    }),
                    modifier = Modifier.fillMaxWidth().onPreviewKeyEvent {
                        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                            true
                        } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                            focusManager.clearFocus()
                            submitForm()
                            true
                        } else false
                    },
                    placeholder = { Text(stringResource(Res.string.dob_placeholder)) }
                )
                
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitForm()
                }
            ) {
                Text(stringResource(Res.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}