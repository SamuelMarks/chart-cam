package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    
    val focusManager = LocalFocusManager.current
    
    val submitForm = {
        if (firstName.isBlank() || lastName.isBlank() || mrn.isBlank()) {
            error = "All fields are required"
        } else {
            val dob = try {
                LocalDate.parse(dobString.trim())
            } catch (e: Exception) {
                null
            }
            if (dob == null) {
                error = "Invalid Date Format. Use YYYY-MM-DD"
            } else {
                onConfirm(firstName, lastName, mrn, dob, gender)
            }
        }
    }


    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("New Patient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().onKeyEvent { 
                        if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        } else false
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().onKeyEvent { 
                        if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        } else false
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = mrn,
                    onValueChange = { mrn = it },
                    label = { Text("MRN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().onKeyEvent { 
                        if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        } else false
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = dobString,
                    onValueChange = { dobString = it },
                    label = { Text("DOB (YYYY-MM-DD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        submitForm()
                    }),
                    modifier = Modifier.fillMaxWidth().onKeyEvent {
                        if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                            focusManager.clearFocus()
                            submitForm()
                            true
                        } else false
                    },
                    placeholder = { Text("1990-01-01") }
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
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}