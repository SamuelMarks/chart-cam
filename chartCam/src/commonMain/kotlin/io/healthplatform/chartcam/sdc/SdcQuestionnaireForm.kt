/**
 * @file SdcQuestionnaireForm.kt
 * Questionnaire Form component.
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

private const val ALPHA_DISABLED = 0.5f
private const val ALPHA_ENABLED = 1.0f
private const val SURFACE_ALPHA_VARIANT = 0.3f
private const val PHOTO_GRID_ITEM_HEIGHT = 150
private const val DEFAULT_MIN_VALUE = 0f
private const val DEFAULT_MAX_VALUE = 100f

/**
 * Configuration options for rendering a Questionnaire form.
 *
 * @property readOnly Whether the form is completely read-only.
 * @property showValidationErrors Whether to display validation errors immediately.
 * @property hideDisabledItems Whether to hide items that are disabled by enableWhen.
 * @property attachments Optional list of attachments for rendering inline image contexts.
 */
data class SdcFormConfig(
    val readOnly: Boolean = false,
    val showValidationErrors: Boolean = false,
    val hideDisabledItems: Boolean = false,
    val attachments: List<DocumentReference> = emptyList(),
)

/**
 * State snapshot of the current Questionnaire form.
 *
 * @property answers The current map of answers keyed by linkId.
 * @property touchedFields The set of fields the user has interacted with.
 * @property config The form rendering configuration.
 */
data class SdcFormState(
    val answers: Map<String, Any>,
    val touchedFields: Set<String>,
    val config: SdcFormConfig,
)

/**
 * Context object bundling properties required to render form fields, reducing parameter lists.
 */
private data class RenderContext(
    val item: Questionnaire.Item,
    val type: Questionnaire.QuestionnaireItemType,
    val linkId: String,
    val displayLabel: String,
    val isRequired: Boolean,
    val isError: Boolean,
    val errorMessage: String?,
    val state: SdcFormState,
    val focusManager: FocusManager,
    val onAnswerChanged: (String, Any?) -> Unit,
    val onTakePhotoRequested: (String) -> Unit,
)

/**
 * Dynamically renders a Questionnaire based on the resource items.
 * Acts as a KMP equivalent SDC engine supporting enableWhen, calculatedExpression,
 * and automatic QuestionnaireResponse generation.
 *
 * @param questionnaire The FHIR Questionnaire resource to render.
 * @param answers A map containing the current answers, keyed by linkId.
 * @param config Form rendering configuration options.
 * @param onFormUpdated Callback invoked when the user interacts with the input.
 * @param onTakePhotoRequested Callback invoked when the user taps to take a photo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdcQuestionnaireForm(
    questionnaire: Questionnaire,
    answers: Map<String, Any>,
    config: SdcFormConfig = SdcFormConfig(),
    onFormUpdated: (Map<String, Any>, com.google.fhir.model.r4.QuestionnaireResponse) -> Unit,
    onTakePhotoRequested: (String) -> Unit = {},
) {
    var touchedFields by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(setOf<String>())
    }

    val handleAnswerChange: (String, Any?) -> Unit = { linkId, value ->
        touchedFields = touchedFields + linkId
        val updatedAnswers = answers.toMutableMap()
        if (value == null) {
            updatedAnswers.remove(linkId)
        } else {
            updatedAnswers[linkId] = value
        }

        val evaluatedAnswers =
            SdcEvaluator.evaluateCalculatedExpressions(
                questionnaire,
                updatedAnswers,
            )
        val response =
            io.healthplatform.chartcam.fhir.QuestionnaireResponseGenerator
                .generate(questionnaire, evaluatedAnswers)

        onFormUpdated(evaluatedAnswers, response)
    }

    val focusManager = LocalFocusManager.current
    val formState = SdcFormState(answers, touchedFields, config)
    Column {
        questionnaire.item.forEach { item ->
            RenderQuestionnaireItem(
                item = item,
                state = formState,
                onAnswerChanged = handleAnswerChange,
                focusManager = focusManager,
                onTakePhotoRequested = onTakePhotoRequested,
            )
        }
    }
}

/**
 * Recursively renders an individual Questionnaire Item (and its nested items).
 * Manages visibility based on FHIR SDC enableWhen logic, read-only formatting,
 * and widget delegation based on item type and extensions.
 *
 * @param item The specific Questionnaire Item to render.
 * @param state State snapshot including answers, configuration, and touched fields.
 * @param onAnswerChanged Callback invoked when the user updates an answer.
 * @param focusManager Compose focus manager to handle 'Next' keyboard actions.
 * @param onTakePhotoRequested Callback for when photo capture is requested.
 */
@Composable
fun RenderQuestionnaireItem(
    item: Questionnaire.Item,
    state: SdcFormState,
    onAnswerChanged: (String, Any?) -> Unit,
    focusManager: FocusManager,
    onTakePhotoRequested: (String) -> Unit = {},
) {
    val linkId = item.linkId.value
    val type = item.type.value

    if (linkId != null && type != null && !item.isHidden()) {
        RenderQuestionnaireItemImpl(item, state, onAnswerChanged, focusManager, onTakePhotoRequested)
    }
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderQuestionnaireItemImpl(
    item: Questionnaire.Item,
    state: SdcFormState,
    onAnswerChanged: (String, Any?) -> Unit,
    focusManager: FocusManager,
    onTakePhotoRequested: (String) -> Unit,
) {
    val linkId = item.linkId.value!!
    val type = item.type.value!!
    val displayLabel =
        item.text?.value ?: if (type == Questionnaire.QuestionnaireItemType.Group) {
            stringResource(Res.string.cd_unnamed_group)
        } else {
            stringResource(Res.string.cd_unnamed_item)
        }

    val isEnabled = isItemEnabled(item, state.answers)
    val shouldShow = !state.config.hideDisabledItems || isEnabled

    androidx.compose.runtime.LaunchedEffect(isEnabled) {
        if (!isEnabled && state.answers.containsKey(linkId)) {
            onAnswerChanged(linkId, null)
        }
    }

    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val isRequired = item.required?.value == true
        val isTouched = state.touchedFields.contains(linkId)
        val answerValue = state.answers[linkId]

        val isMissingReq = isMissingRequired(answerValue, isRequired)
        val effectiveReadOnly = state.config.readOnly || !isEnabled

        var isError = false
        if (!effectiveReadOnly) {
            if (state.config.showValidationErrors || isTouched) {
                if (isMissingReq) {
                    isError = true
                }
            }
        }

        val errorMessage = if (isError) stringResource(Res.string.error_required_field) else null

        val ctx =
            RenderContext(
                item = item,
                type = type,
                linkId = linkId,
                displayLabel = displayLabel,
                isRequired = isRequired,
                isError = isError,
                errorMessage = errorMessage,
                state = state,
                focusManager = focusManager,
                onAnswerChanged = onAnswerChanged,
                onTakePhotoRequested = onTakePhotoRequested,
            )

        val alpha = if (isEnabled) ALPHA_ENABLED else ALPHA_DISABLED
        Box(modifier = Modifier.alpha(alpha).fillMaxWidth()) {
            if (type == Questionnaire.QuestionnaireItemType.Group) {
                RenderGroupItem(ctx)
            } else {
                RenderInputItem(ctx, effectiveReadOnly)
            }
        }
    }
}

/**
 * Internal helper function.
 */
private fun isMissingRequired(
    answerValue: Any?,
    isRequired: Boolean,
): Boolean {
    if (!isRequired) return false
    return answerValue == null ||
        (answerValue is String && answerValue.isBlank()) ||
        (answerValue is List<*> && answerValue.isEmpty())
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderGroupItem(ctx: RenderContext) {
    androidx.compose.material3.ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .semantics {
                    contentDescription = ctx.displayLabel
                    heading()
                },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = ctx.displayLabel,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ctx.item.item.forEach { nestedItem ->
                RenderQuestionnaireItem(
                    item = nestedItem,
                    state = ctx.state,
                    onAnswerChanged = ctx.onAnswerChanged,
                    focusManager = ctx.focusManager,
                    onTakePhotoRequested = ctx.onTakePhotoRequested,
                )
            }
        }
    }
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderInputItem(
    ctx: RenderContext,
    effectiveReadOnly: Boolean,
) {
    if (effectiveReadOnly) {
        RenderReadOnlyField(ctx)
    } else {
        RenderEditableField(ctx)
    }

    if (ctx.item.item.isNotEmpty()) {
        Column(modifier = Modifier.padding(start = 16.dp)) {
            ctx.item.item.forEach { nestedItem ->
                RenderQuestionnaireItem(
                    item = nestedItem,
                    state = ctx.state,
                    onAnswerChanged = ctx.onAnswerChanged,
                    focusManager = ctx.focusManager,
                    onTakePhotoRequested = ctx.onTakePhotoRequested,
                )
            }
        }
    }
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderReadOnlyField(ctx: RenderContext) {
    val answerDisplay = getAnswerDisplayText(ctx.type, ctx.item, ctx.linkId, ctx.state.answers)

    if (ctx.type == Questionnaire.QuestionnaireItemType.Attachment) {
        RenderReadOnlyAttachment(ctx)
    } else {
        val notAnsweredString = stringResource(Res.string.not_answered)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .semantics(mergeDescendants = true) {
                        val disp = if (answerDisplay.isNotBlank()) answerDisplay else notAnsweredString
                        contentDescription = "${ctx.displayLabel}: $disp"
                    },
        ) {
            Text(
                text = ctx.displayLabel,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            )
            androidx.compose.material3.Surface(
                color =
                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                        .copy(alpha = SURFACE_ALPHA_VARIANT),
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
}

@Composable
/**
 * Internal helper function.
 */
private fun getAnswerDisplayText(
    type: Questionnaire.QuestionnaireItemType,
    item: Questionnaire.Item,
    linkId: String,
    answers: Map<String, Any>,
): String =
    when (type) {
        Questionnaire.QuestionnaireItemType.Boolean -> getBooleanAnswerText(answers[linkId] as? Boolean)
        Questionnaire.QuestionnaireItemType.Choice -> getChoiceAnswerText(item, answers[linkId])
        Questionnaire.QuestionnaireItemType.Integer -> getIntegerAnswerText(answers[linkId])
        Questionnaire.QuestionnaireItemType.Attachment -> ""
        else -> answers[linkId]?.toString() ?: ""
    }

@Composable
/**
 * Internal helper function.
 */
private fun getBooleanAnswerText(checked: Boolean?): String {
    if (checked == null) return ""
    return if (checked) stringResource(Res.string.yes) else stringResource(Res.string.no)
}

/**
 * Internal helper function.
 */
private fun getChoiceAnswerText(
    item: Questionnaire.Item,
    answer: Any?,
): String {
    if (item.repeats?.value == true) {
        val list = (answer as? List<*>)?.filterIsInstance<String>()
        return list?.joinToString(", ") ?: ""
    }
    return answer as? String ?: ""
}

/**
 * Internal helper function.
 */
private fun getIntegerAnswerText(answer: Any?): String {
    val v = (answer as? Float) ?: (answer as? String)?.toFloatOrNull()
    return v?.let { if (it % 1.0f == 0.0f) it.toInt().toString() else it.toString() } ?: ""
}

/**
 * Internal helper function.
 */

@Composable
private fun RenderAttachmentGrid(relatedAttachments: List<com.google.fhir.model.r4.DocumentReference>) {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns =
            androidx.compose.foundation.lazy.grid.GridCells
                .Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .height((PHOTO_GRID_ITEM_HEIGHT * ((relatedAttachments.size + 1) / 2)).dp)
                .padding(vertical = 8.dp),
    ) {
        items(relatedAttachments) { photo ->
            io.healthplatform.chartcam.ui
                .PhotoGridItem(photo)
        }
    }
}

/**
 * Renders read only attachment.
 */
@Composable
private fun RenderReadOnlyAttachment(ctx: RenderContext) {
    val relatedAttachments =
        ctx.state.config.attachments.filter {
            it.context
                ?.related
                ?.firstOrNull()
                ?.identifier
                ?.value
                ?.value == ctx.linkId
        }
    if (relatedAttachments.isNotEmpty()) {
        Column(
            modifier =
                Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {
                    contentDescription = "${ctx.displayLabel}: ${relatedAttachments.size} attachments"
                },
        ) {
            Text(
                text = ctx.displayLabel,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            )
            RenderAttachmentGrid(relatedAttachments)
        }
    } else {
        val notAnsweredString = stringResource(Res.string.not_answered)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${ctx.displayLabel}: $notAnsweredString"
                    },
        ) {
            Text(
                text = ctx.displayLabel,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Internal helper function.
 */
private fun RenderEditableField(ctx: RenderContext) {
    when (ctx.type) {
        Questionnaire.QuestionnaireItemType.String -> RenderStringField(ctx)
        Questionnaire.QuestionnaireItemType.Boolean -> RenderBooleanField(ctx)
        Questionnaire.QuestionnaireItemType.Choice -> RenderChoiceField(ctx)
        Questionnaire.QuestionnaireItemType.Attachment -> RenderAttachmentField(ctx)
        Questionnaire.QuestionnaireItemType.Text -> RenderTextField(ctx)
        Questionnaire.QuestionnaireItemType.Date -> RenderDateField(ctx)
        Questionnaire.QuestionnaireItemType.DateTime -> RenderDateTimeField(ctx)
        Questionnaire.QuestionnaireItemType.Decimal -> RenderDecimalField(ctx)
        Questionnaire.QuestionnaireItemType.Integer -> RenderIntegerField(ctx)
        else -> {}
    }
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderStringField(ctx: RenderContext) {
    val text = ctx.state.answers[ctx.linkId] as? String ?: ""
    io.healthplatform.chartcam.ui.components.FormBuilderTextInput(
        value = text,
        onValueChange = { ctx.onAnswerChanged(ctx.linkId, it) },
        label = ctx.displayLabel,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { ctx.focusManager.moveFocus(FocusDirection.Next) }),
        modifier =
            Modifier
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = ctx.displayLabel
                    if (ctx.isError && ctx.errorMessage != null) {
                        error(ctx.errorMessage)
                    }
                }.tabFocusNext(ctx.focusManager),
    )
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderBooleanField(ctx: RenderContext) {
    val checked = ctx.state.answers[ctx.linkId] as? Boolean ?: false
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("CheckboxRow ${ctx.displayLabel}")
                .toggleable(
                    value = checked,
                    onValueChange = { ctx.onAnswerChanged(ctx.linkId, it) },
                    role = Role.Checkbox,
                ).padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = ctx.displayLabel
                    if (ctx.isError && ctx.errorMessage != null) {
                        error(ctx.errorMessage)
                    }
                },
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
        )
        io.healthplatform.chartcam.ui.components.FormLabel(
            text = ctx.displayLabel,
            isRequired = ctx.isRequired,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Internal helper function.
 */
private fun RenderChoiceField(ctx: RenderContext) {
    var expanded by remember { mutableStateOf(false) }
    val options =
        ctx.item.answerOption.mapNotNull { option ->
            val v1 =
                option.value
                    ?.asString()
                    ?.value
                    ?.value
            val v2 =
                option.value
                    ?.asCoding()
                    ?.value
                    ?.display
                    ?.value
            val v3 =
                option.value
                    ?.asCoding()
                    ?.value
                    ?.code
                    ?.value
            v1 ?: v2 ?: v3
        }

    val itemControl = ctx.item.getItemControl()
    val isMultiSelect = ctx.item.repeats?.value == true

    if (isMultiSelect && itemControl != "check-box") {
        val selectedOptions =
            (ctx.state.answers[ctx.linkId] as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
        FormBuilderMultiSelectDropdown(
            selectedOptions = selectedOptions,
            options = options,
            onSelectionChanged = { ctx.onAnswerChanged(ctx.linkId, it) },
            label = ctx.displayLabel,
            isRequired = ctx.isRequired,
            isError = ctx.isError,
            errorMessage = ctx.errorMessage,
            modifier =
                Modifier.semantics(mergeDescendants = true) {
                    contentDescription = ctx.displayLabel
                    if (ctx.isError && ctx.errorMessage != null) {
                        error(ctx.errorMessage)
                        liveRegion = LiveRegionMode.Polite
                    }
                },
        )
    } else if (itemControl == "radio-button" || itemControl == "check-box") {
        RenderRadioOrCheckboxGroup(ctx, itemControl, isMultiSelect, options)
    } else {
        RenderDropdownField(ctx, options, expanded, { expanded = it })
    }
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderRadioOrCheckboxGroup(
    ctx: RenderContext,
    itemControl: String,
    isMultiSelect: Boolean,
    options: List<String>,
) {
    val isCheckboxes = itemControl == "check-box"
    val selectedOptions =
        if (isMultiSelect) {
            (ctx.state.answers[ctx.linkId] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        } else {
            listOfNotNull(ctx.state.answers[ctx.linkId] as? String)
        }

    Column(
        modifier =
            Modifier
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = ctx.displayLabel
                    if (ctx.isError && ctx.errorMessage != null) {
                        error(ctx.errorMessage)
                    }
                }.then(if (!isMultiSelect) Modifier.selectableGroup() else Modifier),
    ) {
        io.healthplatform.chartcam.ui.components.FormLabel(
            ctx.displayLabel,
            ctx.isRequired,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        if (ctx.isError && ctx.errorMessage != null) {
            Text(
                ctx.errorMessage,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        options.forEach { option ->
            RenderRadioOrCheckboxOption(ctx, option, isMultiSelect, isCheckboxes, selectedOptions)
        }
    }
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderRadioOrCheckboxOption(
    ctx: RenderContext,
    option: String,
    isMultiSelect: Boolean,
    isCheckboxes: Boolean,
    selectedOptions: List<String>,
) {
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
                                val newVal = if (newSelections.isEmpty()) null else newSelections
                                ctx.onAnswerChanged(ctx.linkId, newVal)
                            },
                            role = Role.Checkbox,
                        )
                    } else {
                        Modifier.selectable(
                            selected = isSelected,
                            onClick = {
                                if (isSelected && isCheckboxes) {
                                    ctx.onAnswerChanged(ctx.linkId, null)
                                } else {
                                    ctx.onAnswerChanged(ctx.linkId, option)
                                }
                            },
                            role = if (isCheckboxes) Role.Checkbox else Role.RadioButton,
                        )
                    },
                ).padding(vertical = 4.dp),
    ) {
        if (isCheckboxes) {
            androidx.compose.material3.Checkbox(checked = isSelected, onCheckedChange = null)
        } else {
            androidx.compose.material3.RadioButton(selected = isSelected, onClick = null)
        }
        Text(text = option, modifier = Modifier.padding(start = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Internal helper function.
 */
private fun RenderDropdownField(
    ctx: RenderContext,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val selectedOption = ctx.state.answers[ctx.linkId] as? String ?: ""
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = ctx.displayLabel
                    if (ctx.isError && ctx.errorMessage != null) error(ctx.errorMessage)
                },
    ) {
        OutlinedTextField(
            value = selectedOption.ifEmpty { stringResource(Res.string.select_an_option) },
            onValueChange = {},
            readOnly = true,
            label = {
                io.healthplatform.chartcam.ui.components
                    .FormLabel(ctx.displayLabel, ctx.isRequired)
            },
            isError = ctx.isError,
            supportingText = { if (ctx.isError && ctx.errorMessage != null) Text(ctx.errorMessage) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier =
                Modifier
                    .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .tabFocusNext(ctx.focusManager),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        ctx.onAnswerChanged(ctx.linkId, option)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderAttachmentField(ctx: RenderContext) {
    val relatedAttachments =
        ctx.state.config.attachments.filter {
            it.context
                ?.related
                ?.firstOrNull()
                ?.identifier
                ?.value
                ?.value == ctx.linkId
        }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        io.healthplatform.chartcam.ui.components.FormLabel(
            text = ctx.displayLabel,
            isRequired = ctx.isRequired,
            modifier =
                Modifier.semantics(mergeDescendants = true) {
                    contentDescription = ctx.displayLabel
                },
        )

        if (relatedAttachments.isNotEmpty()) {
            RenderAttachmentGrid(relatedAttachments)
        }
    }
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderTextField(ctx: RenderContext) {
    val text = ctx.state.answers[ctx.linkId] as? String ?: ""
    FormBuilderTextArea(
        value = text,
        onValueChange = { ctx.onAnswerChanged(ctx.linkId, it) },
        label = ctx.displayLabel,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                contentDescription = ctx.displayLabel
                if (ctx.isError && ctx.errorMessage != null) {
                    error(ctx.errorMessage)
                    liveRegion = LiveRegionMode.Polite
                }
            },
    )
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderDateField(ctx: RenderContext) {
    val text = ctx.state.answers[ctx.linkId] as? String ?: ""
    FormBuilderDatePicker(
        value = text,
        onValueChange = { ctx.onAnswerChanged(ctx.linkId, it) },
        label = ctx.displayLabel,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                contentDescription = ctx.displayLabel
                if (ctx.isError && ctx.errorMessage != null) {
                    error(ctx.errorMessage)
                    liveRegion = LiveRegionMode.Polite
                }
            },
    )
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderDateTimeField(ctx: RenderContext) {
    val text = ctx.state.answers[ctx.linkId] as? String ?: ""
    FormBuilderDateTimePicker(
        value = text,
        onValueChange = { ctx.onAnswerChanged(ctx.linkId, it) },
        label = ctx.displayLabel,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                contentDescription = ctx.displayLabel
                if (ctx.isError && ctx.errorMessage != null) {
                    error(ctx.errorMessage)
                    liveRegion = LiveRegionMode.Polite
                }
            },
    )
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderDecimalField(ctx: RenderContext) {
    val text = ctx.state.answers[ctx.linkId] as? String ?: ""
    FormBuilderNumericInput(
        value = text,
        onValueChange = { ctx.onAnswerChanged(ctx.linkId, it) },
        label = ctx.displayLabel,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                contentDescription = ctx.displayLabel
                if (ctx.isError && ctx.errorMessage != null) {
                    error(ctx.errorMessage)
                    liveRegion = LiveRegionMode.Polite
                }
            },
    )
}

@Composable
/**
 * Internal helper function.
 */
private fun RenderIntegerField(ctx: RenderContext) {
    val minValue = ctx.item.getMinValue() ?: DEFAULT_MIN_VALUE
    val maxValue = ctx.item.getMaxValue() ?: DEFAULT_MAX_VALUE
    val val1 = ctx.state.answers[ctx.linkId] as? Float
    val val2 = (ctx.state.answers[ctx.linkId] as? String)?.toFloatOrNull()
    val value = val1 ?: val2 ?: minValue
    io.healthplatform.chartcam.ui.components.FormBuilderRangeSlider(
        value = value,
        valueRange = minValue..maxValue,
        onValueChange = { ctx.onAnswerChanged(ctx.linkId, it) },
        label = ctx.displayLabel,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                contentDescription = ctx.displayLabel
                if (ctx.isError && ctx.errorMessage != null) {
                    error(ctx.errorMessage)
                    liveRegion = LiveRegionMode.Polite
                }
            },
    )
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
    var enabled = true
    if (item.enableWhen.isNotEmpty()) {
        val behavior = item.enableBehavior?.value ?: Questionnaire.EnableWhenBehavior.Any
        val conditions = item.enableWhen.map { ew -> evaluateCondition(ew, answers) }
        enabled =
            if (behavior == Questionnaire.EnableWhenBehavior.All) {
                conditions.all { it }
            } else {
                conditions.any { it }
            }
    }
    return enabled
}

/**
 * Internal helper function.
 */
private fun evaluateCondition(
    ew: Questionnaire.Item.EnableWhen,
    answers: Map<String, Any>,
): Boolean {
    val targetQuestion = ew.question.value
    val operator = ew.operator.value

    if (targetQuestion == null || operator == null) return false

    val targetAnswer = answers[targetQuestion]
    val ewAnswer = ew.answer

    return when (operator) {
        Questionnaire.QuestionnaireItemOperator.EqualTo -> evaluateEqualTo(ewAnswer, targetAnswer)
        Questionnaire.QuestionnaireItemOperator.NotEqualTo -> evaluateNotEqualTo(ewAnswer, targetAnswer)
        Questionnaire.QuestionnaireItemOperator.Exists -> {
            val exists = ewAnswer.asBoolean()?.value?.value ?: true
            if (exists) targetAnswer != null else targetAnswer == null
        }
        else -> false
    }
}

/**
 * Internal helper function.
 */
private fun evaluateEqualTo(
    ewAnswer: Questionnaire.Item.EnableWhen.Answer,
    targetAnswer: Any?,
): Boolean =
    when {
        ewAnswer.asString() != null -> targetAnswer == ewAnswer.asString()?.value?.value
        ewAnswer.asBoolean() != null -> targetAnswer == ewAnswer.asBoolean()?.value?.value
        else -> false
    }

/**
 * Internal helper function.
 */
private fun evaluateNotEqualTo(
    ewAnswer: Questionnaire.Item.EnableWhen.Answer,
    targetAnswer: Any?,
): Boolean =
    when {
        ewAnswer.asString() != null -> targetAnswer != ewAnswer.asString()?.value?.value
        ewAnswer.asBoolean() != null -> targetAnswer != ewAnswer.asBoolean()?.value?.value
        else -> false
    }
