/**
 * @file CreatePatientDialog.kt
 * Contains declarations for CreatePatientDialog.kt.
 *
 * Provides the CreatePatientDialog component for creating new patients.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.all_fields_required
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.cd_select_date
import chartcam.chartcam.generated.resources.create
import chartcam.chartcam.generated.resources.dob_label
import chartcam.chartcam.generated.resources.first_name
import chartcam.chartcam.generated.resources.invalid_date_format
import chartcam.chartcam.generated.resources.last_name
import chartcam.chartcam.generated.resources.mrn
import chartcam.chartcam.generated.resources.new_patient
import chartcam.chartcam.generated.resources.ok
import io.healthplatform.chartcam.ui.currentLanguageState
import io.healthplatform.chartcam.utils.formatLocalizedDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

private const val EXPECTED_DATE_PARTS = 3
private const val FOUR_DIGIT_YEAR_LENGTH = 4
private const val TWO_DIGIT_YEAR_LENGTH = 2
private const val CENTURY_BASE = 2000

/**
 * Checks if any of the provided strings are blank.
 *
 * @param fields The strings to check.
 * @return True if at least one field is blank.
 */
private fun hasBlankField(vararg fields: String): Boolean = fields.any { it.isBlank() }

/**
 * Determines whether a given language/locale tag primarily uses Day-Month-Year ordering.
 *
 * @param language The IETF BCP-47 language tag (e.g., "en-GB", "en-AU", "es", "he").
 * @return True if the locale uses DD/MM/YYYY by convention.
 */
private fun isDayFirstLocale(language: String): Boolean {
    val lower = language.lowercase()
    val parts = lower.split("-", "_")
    val lang = parts.first()
    val region = if (parts.size > 1) parts[1] else ""
    val isCommonwealthEnglish = lang == "en" && region in setOf("gb", "uk", "au", "nz", "ie", "za", "in", "sg")
    val isDayFirstLanguage = lang in setOf("es", "he", "iw", "fr", "de", "it", "pt", "ru")
    return isCommonwealthEnglish || isDayFirstLanguage
}

/**
 * Resolves a two-digit or four-digit year string to a four-digit integer year.
 *
 * @param part Year string component.
 * @param num Parsed numeric value of the year component.
 * @return Four-digit integer year, or null if length does not represent a valid year.
 */
private fun resolveYear(
    part: String,
    num: Int,
): Int? =
    when (part.length) {
        FOUR_DIGIT_YEAR_LENGTH -> num
        TWO_DIGIT_YEAR_LENGTH -> CENTURY_BASE + num
        else -> null
    }

/**
 * Attempts to construct a [LocalDate] with the given month and day, falling back if invalid.
 *
 * @param year The four-digit year.
 * @param first The first numeric component.
 * @param second The second numeric component.
 * @param tryFirstAsMonth If true, attempts (year, first, second) before (year, second, first).
 * @return The constructed [LocalDate], or null if both attempts fail.
 */
private fun tryConstructDate(
    year: Int,
    first: Int,
    second: Int,
    tryFirstAsMonth: Boolean,
): LocalDate? =
    try {
        if (tryFirstAsMonth) LocalDate(year, first, second) else LocalDate(year, second, first)
    } catch (_: IllegalArgumentException) {
        try {
            if (tryFirstAsMonth) LocalDate(year, second, first) else LocalDate(year, first, second)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

/**
 * Attempts to parse date parts into a [LocalDate].
 *
 * @param parts String components of the date.
 * @param nums Numeric components of the date.
 * @param language Active language tag.
 * @return Parsed [LocalDate] or null.
 */
private fun parsePartsToDate(
    parts: List<String>,
    nums: List<Int>,
    language: String,
): LocalDate? =
    try {
        if (parts[0].length == FOUR_DIGIT_YEAR_LENGTH) {
            LocalDate(nums[0], nums[1], nums[2])
        } else {
            val year = resolveYear(parts[2], nums[2])
            if (year != null) {
                val isDayFirst = isDayFirstLocale(language)
                tryConstructDate(year, nums[0], nums[1], tryFirstAsMonth = !isDayFirst)
            } else {
                null
            }
        }
    } catch (_: IllegalArgumentException) {
        null
    }

/**
 * Attempts to parse a user-entered date string in either ISO-8601 (YYYY-MM-DD)
 * or common localized numeric formats (MM/DD/YYYY, DD/MM/YYYY, YYYY/MM/DD).
 *
 * @param input The raw user input date string.
 * @param language The active application language to disambiguate month/day order.
 * @return The parsed [LocalDate], or null if parsing fails.
 */
fun parseFlexibleDate(
    input: String,
    language: String = currentLanguageState.value,
): LocalDate? {
    val trimmed = input.trim()
    val isoDate =
        try {
            LocalDate.parse(trimmed)
        } catch (_: IllegalArgumentException) {
            null
        }
    if (isoDate != null) return isoDate

    val parts = trimmed.split(Regex("[/.-]")).filter { it.isNotBlank() }
    val nums = parts.mapNotNull { it.toIntOrNull() }

    return if (parts.size == EXPECTED_DATE_PARTS && nums.size == EXPECTED_DATE_PARTS) {
        parsePartsToDate(parts, nums, language)
    } else {
        null
    }
}

/**
 * Dialog for creating a new Patient.
 * Collects first name, last name, MRN, Date of Birth, and gender.
 * Provides validation for all fields and date format.
 *
 * **State & Side Effects:**
 * Maintains internal form state (firstName, lastName, mrn, dob, gender, and errors).
 * Validates date parsing directly in the dialog logic.
 * No explicit Modifier is exposed as this represents a top-level Dialog.
 *
 * @param onDismissRequest Callback invoked when the user dismisses the dialog without saving.
 * @param onConfirm Callback invoked when the user successfully saves the new patient data.
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
     * Stores the raw string input for the patient's date of birth.
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
     * Controls the visibility of the DatePicker dialog.
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

    val currentLang by currentLanguageState.collectAsState()
    val localizedDatePattern =
        when (currentLang.lowercase().split("-", "_").first()) {
            "zh", "ja" -> "YYYY/MM/DD"
            "es", "he" -> "DD/MM/YYYY"
            else -> "YYYY-MM-DD"
        }
    val sampleDate = formatLocalizedDate("1990-01-01", currentLang)
    val baseDobLabel = stringResource(Res.string.dob_label)
    val dobLabelText =
        if (localizedDatePattern == "YYYY-MM-DD") {
            baseDobLabel
        } else {
            baseDobLabel
                .replace("YYYY-MM-DD", localizedDatePattern)
                .replace("AAAA-MM-DD", localizedDatePattern)
        }

    /**
     * Lambda function invoked to validate form input and submit the data if validation passes.
     */
    val submitForm = {
        if (hasBlankField(firstName, lastName, mrn, dobString)) {
            error = errorAllFields
        } else {
            val dob = parseFlexibleDate(dobString, currentLang)
            if (dob == null) {
                error = errorInvalidDate
            } else {
                onConfirm(firstName, lastName, mrn, dob, gender)
            }
        }
    }

    val isFirstNameError = error != null && firstName.isBlank()
    val isLastNameError = error != null && lastName.isBlank()
    val isMrnError = error != null && mrn.isBlank()
    val isDobError = error != null && (dobString.isBlank() || error == errorInvalidDate)
    val dobValidationMessage = if (dobString.isBlank()) errorAllFields else errorInvalidDate

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                stringResource(Res.string.new_patient),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        if (isFirstNameError) error = null
                    },
                    label = { Text(stringResource(Res.string.first_name)) },
                    isError = isFirstNameError,
                    supportingText = {
                        if (isFirstNameError) {
                            Text(errorAllFields)
                        }
                    },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics {
                                if (isFirstNameError) {
                                    error(errorAllFields)
                                }
                            }.tabFocusNext(focusManager)
                            .onPreviewKeyEvent {
                                if (it.key == Key.Enter &&
                                    it.type == KeyEventType.KeyUp
                                ) {
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
                    onValueChange = {
                        lastName = it
                        if (isLastNameError) error = null
                    },
                    label = { Text(stringResource(Res.string.last_name)) },
                    isError = isLastNameError,
                    supportingText = {
                        if (isLastNameError) {
                            Text(errorAllFields)
                        }
                    },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics {
                                if (isLastNameError) {
                                    error(errorAllFields)
                                }
                            }.tabFocusNext(focusManager)
                            .onPreviewKeyEvent {
                                if (it.key == Key.Enter &&
                                    it.type == KeyEventType.KeyUp
                                ) {
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
                    onValueChange = {
                        mrn = it
                        if (isMrnError) error = null
                    },
                    label = { Text(stringResource(Res.string.mrn)) },
                    isError = isMrnError,
                    supportingText = {
                        if (isMrnError) {
                            Text(errorAllFields)
                        }
                    },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics {
                                if (isMrnError) {
                                    error(errorAllFields)
                                }
                            }.tabFocusNext(focusManager)
                            .onPreviewKeyEvent {
                                if (it.key == Key.Enter &&
                                    it.type == KeyEventType.KeyUp
                                ) {
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
                    onValueChange = {
                        dobString = it
                        if (isDobError) error = null
                    },
                    label = { Text(dobLabelText) },
                    isError = isDobError,
                    supportingText = {
                        if (isDobError) {
                            Text(dobValidationMessage)
                        } else {
                            Text(sampleDate)
                        }
                    },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics {
                                if (isDobError) {
                                    error(dobValidationMessage)
                                }
                            }.tabFocusNext(focusManager)
                            .onPreviewKeyEvent {
                                if (it.key == Key.Enter &&
                                    it.type == KeyEventType.KeyUp
                                ) {
                                    showDatePicker = true
                                    true
                                } else {
                                    false
                                }
                            },
                    placeholder = { Text(sampleDate) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = stringResource(Res.string.cd_select_date),
                            )
                        }
                    },
                )

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                showDatePicker = false
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
                                    val date = instant.toLocalDateTime(TimeZone.UTC).date
                                    val iso = date.toString()
                                    val parts = iso.split("-")
                                    dobString =
                                        when (localizedDatePattern) {
                                            "DD/MM/YYYY" -> "${parts[2]}/${parts[1]}/${parts[0]}"
                                            "YYYY/MM/DD" -> "${parts[0]}/${parts[1]}/${parts[2]}"
                                            else -> iso
                                        }
                                }
                            }) {
                                Text(stringResource(Res.string.ok))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text(stringResource(Res.string.cancel))
                            }
                        },
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier =
                            Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                                this.error(error!!)
                            },
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
