/**
 * @file SdcQuestionnaireForm.kt
 * Contains declarations for SdcQuestionnaireForm.kt.
 *
 * Provides components for rendering dynamic FHIR Questionnaires as UI forms.
 */
package io.healthplatform.chartcam.sdc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_unnamed_group
import chartcam.chartcam.generated.resources.cd_unnamed_item
import chartcam.chartcam.generated.resources.error_required_field
import chartcam.chartcam.generated.resources.no
import chartcam.chartcam.generated.resources.not_answered
import chartcam.chartcam.generated.resources.select_an_option
import chartcam.chartcam.generated.resources.yes
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.fhir.getItemControl
import io.healthplatform.chartcam.fhir.getMaxValue
import io.healthplatform.chartcam.fhir.getMinValue
import io.healthplatform.chartcam.fhir.isHidden
import io.healthplatform.chartcam.ui.components.FormBuilderDatePicker
import io.healthplatform.chartcam.ui.components.FormBuilderDateTimePicker
import io.healthplatform.chartcam.ui.components.FormBuilderMultiSelectDropdown
import io.healthplatform.chartcam.ui.components.FormBuilderNumericInput
import io.healthplatform.chartcam.ui.components.FormBuilderRangeSlider
import io.healthplatform.chartcam.ui.components.FormBuilderTextArea
import io.healthplatform.chartcam.ui.components.tabFocusNext
import org.jetbrains.compose.resources.stringResource

/**
 * Dynamically renders a Questionnaire based on the resource items.
 * Acts as a KMP equivalent SDC engine supporting enableWhen, calculatedExpression,
 * and automatic QuestionnaireResponse generation.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param questionnaire The FHIR Questionnaire resource to render.
 * @param answers A map containing the current answers, keyed by linkId.
 * @param readOnly Whether the form should be rendered in read-only mode.
 * @param showValidationErrors Whether to display validation errors immediately.
 * @param hideDisabledItems Whether to completely hide disabled items.
 * @param attachments Optional list of attachments to display inline (e.g. photos).
 * @param onFormUpdated Callback invoked when the user interacts with the input, returning the updated answers map and the generated QuestionnaireResponse.
 * @param onTakePhotoRequested Callback invoked when the user taps to take a photo for a specific attachment item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdcQuestionnaireForm(
    questionnaire: Questionnaire,
    answers: Map<String, Any>,
    readOnly: Boolean = false,
    showValidationErrors: Boolean = false,
    hideDisabledItems: Boolean = false,
    attachments: List<DocumentReference> = emptyList(),
    onFormUpdated: (Map<String, Any>, com.google.fhir.model.r4.QuestionnaireResponse) -> Unit,
    onTakePhotoRequested: (String) -> Unit = {},
) {
    var touchedFields by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(setOf<String>()) }
    val handleAnswerChange: (String, Any?) -> Unit = { linkId, value ->
        touchedFields = touchedFields + linkId
        val updatedAnswers = answers.toMutableMap()
        if (value == null) {
            updatedAnswers.remove(linkId)
        } else {
            updatedAnswers[linkId] = value
        }

        // SDC Extension: Evaluate calculated expressions
        val evaluatedAnswers = SdcEvaluator.evaluateCalculatedExpressions(questionnaire, updatedAnswers)

        // Auto-generate QuestionnaireResponse directly from UI
        val response =
            io.healthplatform.chartcam.fhir.QuestionnaireResponseGenerator
                .generate(questionnaire, evaluatedAnswers)

        onFormUpdated(evaluatedAnswers, response)
    }

    /** Focus manager used to navigate form inputs. */
    val focusManager = LocalFocusManager.current
    Column {
        questionnaire.item.forEach { item ->
            RenderQuestionnaireItem(
                item,
                answers,
                readOnly,
                showValidationErrors,
                hideDisabledItems,
                touchedFields,
                handleAnswerChange,
                focusManager,
                attachments,
                onTakePhotoRequested,
            )
        }
    }
}

/**
 * Recursively renders an individual Questionnaire Item (and its nested items).
 * Manages visibility based on FHIR SDC enableWhen logic, read-only formatting,
 * and widget delegation based on item type and extensions.
 *
 * Sibling items are stacked natively in a Column structure. Intentional spacing
 * handles separation instead of rigid explicit dividers between each item, keeping
 * the form visually uncluttered.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param item The specific Questionnaire Item to render.
 * @param answers The current map of answers.
 * @param readOnly Whether the field should be forced into a read-only state.
 * @param showValidationErrors Whether validation errors should be immediately visible.
 * @param hideDisabledItems Whether to completely hide disabled items or just grey them out.
 * @param touchedFields A set of linkIds that have been interacted with, to trigger validation.
 * @param onAnswerChanged Callback invoked when the user interacts with the input.
 * @param focusManager Compose focus manager to handle 'Next' actions on keyboards.
 * @param attachments A list of attached documents for rendering inline images in read-only mode.
 * @param onTakePhotoRequested Callback invoked when the user taps to take a photo for a specific attachment item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderQuestionnaireItem(
    item: Questionnaire.Item,
    answers: Map<String, Any>,
    readOnly: Boolean,
    showValidationErrors: Boolean,
    hideDisabledItems: Boolean,
    touchedFields: Set<String>,
    onAnswerChanged: (String, Any?) -> Unit,
    focusManager: FocusManager,
    attachments: List<DocumentReference> = emptyList(),
    onTakePhotoRequested: (String) -> Unit = {},
) {
    val linkId = item.linkId.value ?: return
    val type = item.type.value ?: return

    val fallbackLabel =
        if (type == Questionnaire.QuestionnaireItemType.Group) {
            stringResource(
                Res.string.cd_unnamed_group,
            )
        } else {
            stringResource(Res.string.cd_unnamed_item)
        }
    val displayLabel = item.text?.value ?: fallbackLabel

    // Instead of using just linkId for semantics, use the actual label text so screen readers read the question out loud!
    val itemDesc = displayLabel

    // SDC logic: enableWhen
    if (item.isHidden()) return
    val isEnabled = isItemEnabled(item, answers)

    val shouldShow = !hideDisabledItems || isEnabled

    androidx.compose.runtime.LaunchedEffect(isEnabled) {
        if (!isEnabled && answers.containsKey(linkId)) {
            onAnswerChanged(linkId, null)
        }
    }

    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val effectiveReadOnly = readOnly || !isEnabled

        val isRequired = item.required?.value == true
        val isTouched = touchedFields.contains(linkId)
        val answerValue = answers[linkId]
        val isMissingRequired =
            isRequired &&
                (
                    answerValue == null ||
                        (answerValue is String && answerValue.isBlank()) ||
                        (answerValue is List<*> && answerValue.isEmpty())
                )
        val isError = !effectiveReadOnly && (showValidationErrors || isTouched) && isMissingRequired
        val errorMessage = if (isError) stringResource(Res.string.error_required_field) else null

        val alpha = if (isEnabled) 1.0f else 0.5f
        androidx.compose.foundation.layout.Box(modifier = Modifier.alpha(alpha).fillMaxWidth()) {
            if (type == Questionnaire.QuestionnaireItemType.Group) {
                androidx.compose.material3.ElevatedCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .semantics {
                                contentDescription = itemDesc
                                heading()
                            },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = displayLabel,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        item.item.forEach { nestedItem ->
                            RenderQuestionnaireItem(
                                nestedItem,
                                answers,
                                readOnly,
                                showValidationErrors,
                                hideDisabledItems,
                                touchedFields,
                                onAnswerChanged,
                                focusManager,
                                attachments,
                                onTakePhotoRequested,
                            )
                        }
                    }
                }
            } else {
                if (effectiveReadOnly) {
                    val labelText = displayLabel
                    val answerDisplay =
                        when (type) {
                            Questionnaire.QuestionnaireItemType.String,
                            Questionnaire.QuestionnaireItemType.Text,
                            Questionnaire.QuestionnaireItemType.Date,
                            Questionnaire.QuestionnaireItemType.DateTime,
                            Questionnaire.QuestionnaireItemType.Decimal,
                            -> answers[linkId] as? String ?: ""
                            Questionnaire.QuestionnaireItemType.Boolean -> {
                                val checked = answers[linkId] as? Boolean
                                if (checked == null) {
                                    ""
                                } else if (checked) {
                                    stringResource(Res.string.yes)
                                } else {
                                    stringResource(Res.string.no)
                                }
                            }
                            Questionnaire.QuestionnaireItemType.Choice -> {
                                if (item.repeats?.value == true) {
                                    ((answers[linkId] as? List<*>)?.filterIsInstance<String>() ?: emptyList()).joinToString(", ")
                                } else {
                                    answers[linkId] as? String ?: ""
                                }
                            }
                            Questionnaire.QuestionnaireItemType.Integer -> {
                                val v = (answers[linkId] as? Float) ?: (answers[linkId] as? String)?.toFloatOrNull()
                                v?.let { if (it % 1.0f == 0.0f) it.toInt().toString() else it.toString() } ?: ""
                            }
                            else -> answers[linkId]?.toString() ?: ""
                        }

                    if (type == Questionnaire.QuestionnaireItemType.Attachment) {
                        val relatedAttachments =
                            attachments.filter {
                                val answerCode =
                                    it.context
                                        ?.related
                                        ?.firstOrNull()
                                        ?.identifier
                                        ?.value
                                        ?.value
                                answerCode == linkId
                            }
                        if (relatedAttachments.isNotEmpty()) {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {
                                        contentDescription =
                                            "$itemDesc: ${relatedAttachments.size} attachments"
                                    },
                            ) {
                                Text(
                                    text = labelText,
                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                )
                                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                    columns =
                                        androidx.compose.foundation.lazy.grid.GridCells
                                            .Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(
                                                (150 * ((relatedAttachments.size + 1) / 2)).dp,
                                            ).padding(vertical = 8.dp),
                                ) {
                                    items(relatedAttachments) { photo ->
                                        io.healthplatform.chartcam.ui
                                            .PhotoGridItem(photo)
                                    }
                                }
                            }
                        } else {
                            val notAnsweredString = stringResource(Res.string.not_answered)
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {
                                        contentDescription =
                                            "$itemDesc: $notAnsweredString"
                                    },
                            ) {
                                Text(
                                    text = labelText,
                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = notAnsweredString,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    } else {
                        val notAnsweredString = stringResource(Res.string.not_answered)
                        Column(
                            modifier =
                                Modifier.fillMaxWidth().padding(vertical = 4.dp).semantics(mergeDescendants = true) {
                                    contentDescription =
                                        "$itemDesc: ${if (answerDisplay.isNotBlank()) answerDisplay else notAnsweredString}"
                                },
                        ) {
                            Text(
                                text = labelText,
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            )
                            androidx.compose.material3.Surface(
                                color =
                                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                        .copy(alpha = 0.3f),
                                shape = androidx.compose.material3.MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            ) {
                                if (answerDisplay.isNotBlank()) {
                                    Text(
                                        text = answerDisplay,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(8.dp),
                                    )
                                } else {
                                    Text(
                                        text = notAnsweredString,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(8.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    when (type) {
                        Questionnaire.QuestionnaireItemType.String -> {
                            val text = answers[linkId] as? String ?: ""
                            io.healthplatform.chartcam.ui.components.FormBuilderTextInput(
                                value = text,
                                onValueChange = { onAnswerChanged(linkId, it) },
                                label = displayLabel,
                                isRequired = isRequired,
                                isError = isError,
                                errorMessage = errorMessage,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                modifier =
                                    Modifier
                                        .padding(vertical = 8.dp)
                                        .semantics(mergeDescendants = true) {
                                            contentDescription = itemDesc
                                            if (isError && errorMessage != null) {
                                                error(errorMessage)
                                            }
                                        }.tabFocusNext(focusManager),
                            )
                        }
                        Questionnaire.QuestionnaireItemType.Boolean -> {
                            val checked = answers[linkId] as? Boolean ?: false
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .testTag("CheckboxRow $displayLabel")
                                        .toggleable(
                                            value = checked,
                                            onValueChange = { onAnswerChanged(linkId, it) },
                                            role = Role.Checkbox,
                                        ).padding(vertical = 8.dp)
                                        .semantics(mergeDescendants = true) {
                                            contentDescription = itemDesc
                                            if (isError && errorMessage != null) {
                                                error(errorMessage)
                                            }
                                        },
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                )
                                io.healthplatform.chartcam.ui.components.FormLabel(
                                    text = displayLabel,
                                    isRequired = isRequired,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                        Questionnaire.QuestionnaireItemType.Choice -> {
                            var expanded by remember { mutableStateOf(false) }
                            val options =
                                item.answerOption.mapNotNull { option ->
                                    val stringValue =
                                        option.value
                                            ?.asString()
                                            ?.value
                                            ?.value
                                    if (stringValue != null) return@mapNotNull stringValue

                                    val codingValue = option.value?.asCoding()?.value
                                    val display = codingValue?.display?.value
                                    val code = codingValue?.code?.value
                                    display ?: code
                                }

                            val itemControl = item.getItemControl()

                            val isMultiSelect = item.repeats?.value == true

                            if (isMultiSelect && itemControl != "check-box") {
                                val selectedOptions = (answers[linkId] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                FormBuilderMultiSelectDropdown(
                                    selectedOptions = selectedOptions,
                                    options = options,
                                    onSelectionChanged = { onAnswerChanged(linkId, it) },
                                    label = displayLabel,
                                    isRequired = isRequired,
                                    isError = isError,
                                    errorMessage = errorMessage,
                                    modifier =
                                        Modifier.semantics(mergeDescendants = true) {
                                            contentDescription = itemDesc
                                            if (isError && errorMessage != null) {
                                                error(errorMessage)
                                                liveRegion = LiveRegionMode.Polite
                                            }
                                        },
                                )
                            } else if (itemControl == "radio-button" || itemControl == "check-box") {
                                val isCheckboxes = itemControl == "check-box"
                                val selectedOptions =
                                    if (isMultiSelect) {
                                        (answers[linkId] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                    } else {
                                        listOfNotNull(answers[linkId] as? String)
                                    }

                                Column(
                                    modifier =
                                        Modifier
                                            .padding(vertical = 8.dp)
                                            .semantics(mergeDescendants = true) {
                                                contentDescription = itemDesc
                                                if (isError && errorMessage != null) {
                                                    error(errorMessage)
                                                }
                                            }.then(if (!isMultiSelect) Modifier.selectableGroup() else Modifier),
                                ) {
                                    io.healthplatform.chartcam.ui.components.FormLabel(
                                        displayLabel,
                                        isRequired,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                    if (isError && errorMessage != null) {
                                        Text(
                                            errorMessage,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(bottom = 4.dp),
                                        )
                                    }
                                    options.forEach { option ->
                                        val isSelected = selectedOptions.contains(option)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (isMultiSelect) {
                                                            Modifier.toggleable(
                                                                value = isSelected,
                                                                onValueChange = { checked ->
                                                                    val newSelections =
                                                                        if (checked) {
                                                                            selectedOptions + option
                                                                        } else {
                                                                            selectedOptions - option
                                                                        }
                                                                    onAnswerChanged(
                                                                        linkId,
                                                                        if (newSelections.isEmpty()) null else newSelections,
                                                                    )
                                                                },
                                                                role = Role.Checkbox,
                                                            )
                                                        } else {
                                                            Modifier.selectable(
                                                                selected = isSelected,
                                                                onClick = {
                                                                    if (isSelected && isCheckboxes) {
                                                                        onAnswerChanged(linkId, null)
                                                                    } else {
                                                                        onAnswerChanged(linkId, option)
                                                                    }
                                                                },
                                                                role = if (isCheckboxes) Role.Checkbox else Role.RadioButton,
                                                            )
                                                        },
                                                    ).padding(vertical = 4.dp),
                                        ) {
                                            if (isCheckboxes) {
                                                androidx.compose.material3.Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = null,
                                                )
                                            } else {
                                                androidx.compose.material3.RadioButton(
                                                    selected = isSelected,
                                                    onClick = null,
                                                )
                                            }
                                            Text(text = option, modifier = Modifier.padding(start = 8.dp))
                                        }
                                    }
                                }
                            } else {
                                val selectedOption = answers[linkId] as? String ?: ""
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it },
                                    modifier =
                                        Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {
                                            contentDescription = itemDesc
                                            if (isError && errorMessage != null) {
                                                error(errorMessage)
                                            }
                                        },
                                ) {
                                    OutlinedTextField(
                                        value = selectedOption.ifEmpty { stringResource(Res.string.select_an_option) },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = {
                                            io.healthplatform.chartcam.ui.components
                                                .FormLabel(displayLabel, isRequired)
                                        },
                                        isError = isError,
                                        supportingText = { if (isError && errorMessage != null) Text(errorMessage) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        modifier =
                                            Modifier
                                                .menuAnchor(
                                                    androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                                ).fillMaxWidth()
                                                .tabFocusNext(focusManager),
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                    ) {
                                        options.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    onAnswerChanged(linkId, option)
                                                    expanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Questionnaire.QuestionnaireItemType.Attachment -> {
                            val relatedAttachments =
                                attachments.filter {
                                    val answerCode =
                                        it.context
                                            ?.related
                                            ?.firstOrNull()
                                            ?.identifier
                                            ?.value
                                            ?.value
                                    answerCode == linkId
                                }

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                io.healthplatform.chartcam.ui.components.FormLabel(
                                    text = displayLabel,
                                    isRequired = isRequired,
                                    modifier =
                                        Modifier.semantics(mergeDescendants = true) {
                                            contentDescription = itemDesc
                                        },
                                )

                                if (relatedAttachments.isNotEmpty()) {
                                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                        columns =
                                            androidx.compose.foundation.lazy.grid.GridCells
                                                .Fixed(2),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(
                                                    (150 * ((relatedAttachments.size + 1) / 2)).dp,
                                                ).padding(vertical = 8.dp),
                                    ) {
                                        items(relatedAttachments) { photo ->
                                            io.healthplatform.chartcam.ui
                                                .PhotoGridItem(photo)
                                        }
                                    }
                                }
                            }
                        }
                        Questionnaire.QuestionnaireItemType.Text -> {
                            val text = answers[linkId] as? String ?: ""
                            FormBuilderTextArea(
                                value = text,
                                onValueChange = { onAnswerChanged(linkId, it) },
                                label = displayLabel,
                                isRequired = isRequired,
                                isError = isError,
                                errorMessage = errorMessage,
                                modifier =
                                    Modifier.semantics(mergeDescendants = true) {
                                        contentDescription = itemDesc
                                        if (isError && errorMessage != null) {
                                            error(errorMessage)
                                            liveRegion = LiveRegionMode.Polite
                                        }
                                    },
                            )
                        }
                        Questionnaire.QuestionnaireItemType.Date -> {
                            val text = answers[linkId] as? String ?: ""
                            FormBuilderDatePicker(
                                value = text,
                                onValueChange = { onAnswerChanged(linkId, it) },
                                label = displayLabel,
                                isRequired = isRequired,
                                isError = isError,
                                errorMessage = errorMessage,
                                modifier =
                                    Modifier.semantics(mergeDescendants = true) {
                                        contentDescription = itemDesc
                                        if (isError && errorMessage != null) {
                                            error(errorMessage)
                                            liveRegion = LiveRegionMode.Polite
                                        }
                                    },
                            )
                        }
                        Questionnaire.QuestionnaireItemType.DateTime -> {
                            val text = answers[linkId] as? String ?: ""
                            FormBuilderDateTimePicker(
                                value = text,
                                onValueChange = { onAnswerChanged(linkId, it) },
                                label = displayLabel,
                                isRequired = isRequired,
                                isError = isError,
                                errorMessage = errorMessage,
                                modifier =
                                    Modifier.semantics(mergeDescendants = true) {
                                        contentDescription = itemDesc
                                        if (isError && errorMessage != null) {
                                            error(errorMessage)
                                            liveRegion = LiveRegionMode.Polite
                                        }
                                    },
                            )
                        }
                        Questionnaire.QuestionnaireItemType.Decimal -> {
                            val text = answers[linkId] as? String ?: ""
                            FormBuilderNumericInput(
                                value = text,
                                onValueChange = { onAnswerChanged(linkId, it) },
                                label = displayLabel,
                                isRequired = isRequired,
                                isError = isError,
                                errorMessage = errorMessage,
                                modifier =
                                    Modifier.semantics(mergeDescendants = true) {
                                        contentDescription = itemDesc
                                        if (isError && errorMessage != null) {
                                            error(errorMessage)
                                            liveRegion = LiveRegionMode.Polite
                                        }
                                    },
                            )
                        }
                        Questionnaire.QuestionnaireItemType.Integer -> {
                            val minValue = item.getMinValue() ?: 0f
                            val maxValue = item.getMaxValue() ?: 100f
                            val value = (answers[linkId] as? Float) ?: (answers[linkId] as? String)?.toFloatOrNull() ?: minValue
                            io.healthplatform.chartcam.ui.components.FormBuilderRangeSlider(
                                value = value,
                                valueRange = minValue..maxValue,
                                onValueChange = { onAnswerChanged(linkId, it) },
                                label = displayLabel,
                                isRequired = isRequired,
                                isError = isError,
                                errorMessage = errorMessage,
                                modifier =
                                    Modifier.semantics(mergeDescendants = true) {
                                        contentDescription = itemDesc
                                        if (isError && errorMessage != null) {
                                            error(errorMessage)
                                            liveRegion = LiveRegionMode.Polite
                                        }
                                    },
                            )
                        }
                        else -> {}
                    }

                    // Also render nested items if a non-group item has them
                }

                if (item.item.isNotEmpty()) {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        item.item.forEach { nestedItem ->
                            RenderQuestionnaireItem(
                                nestedItem,
                                answers,
                                readOnly,
                                showValidationErrors,
                                hideDisabledItems,
                                touchedFields,
                                onAnswerChanged,
                                focusManager,
                                attachments,
                                onTakePhotoRequested,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Evaluates enableWhen conditions for a given item against the current answers.
 *
 * @param item The Questionnaire item to evaluate conditions for.
 * @param answers The map of currently supplied answers.
 * @return True if the item should be enabled (visible), false otherwise.
 */
fun isItemEnabled(
    item: Questionnaire.Item,
    answers: Map<String, Any>,
): Boolean {
    if (item.enableWhen.isEmpty()) return true

    val behavior = item.enableBehavior?.value ?: Questionnaire.EnableWhenBehavior.Any

    val conditions =
        item.enableWhen.map { ew ->
            val targetQuestion = ew.question.value ?: return@map false
            val operator = ew.operator.value ?: return@map false
            val targetAnswer = answers[targetQuestion]

            val ewAnswer = ew.answer

            when (operator) {
                Questionnaire.QuestionnaireItemOperator.EqualTo -> {
                    when {
                        ewAnswer.asString() != null -> targetAnswer == ewAnswer.asString()?.value?.value
                        ewAnswer.asBoolean() != null -> targetAnswer == ewAnswer.asBoolean()?.value?.value
                        else -> false
                    }
                }
                Questionnaire.QuestionnaireItemOperator.NotEqualTo -> {
                    when {
                        ewAnswer.asString() != null -> targetAnswer != ewAnswer.asString()?.value?.value
                        ewAnswer.asBoolean() != null -> targetAnswer != ewAnswer.asBoolean()?.value?.value
                        else -> false
                    }
                }
                Questionnaire.QuestionnaireItemOperator.Exists -> {
                    val exists = ewAnswer.asBoolean()?.value?.value ?: true
                    if (exists) targetAnswer != null else targetAnswer == null
                }
                else -> false
            }
        }

    return if (behavior == Questionnaire.EnableWhenBehavior.All) {
        conditions.all { it }
    } else {
        conditions.any { it }
    }
}
