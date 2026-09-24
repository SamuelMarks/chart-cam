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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import chartcam.chartcam.generated.resources.add_entry
import chartcam.chartcam.generated.resources.attachments_count
import chartcam.chartcam.generated.resources.cd_take_photo_for_item
import chartcam.chartcam.generated.resources.cd_unnamed_group
import chartcam.chartcam.generated.resources.cd_unnamed_item
import chartcam.chartcam.generated.resources.entry_format
import chartcam.chartcam.generated.resources.error_required_field
import chartcam.chartcam.generated.resources.label_value_format
import chartcam.chartcam.generated.resources.no
import chartcam.chartcam.generated.resources.not_answered
import chartcam.chartcam.generated.resources.remove_entry
import chartcam.chartcam.generated.resources.select_an_option
import chartcam.chartcam.generated.resources.take_photo
import chartcam.chartcam.generated.resources.yes
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.fhir.getItemControl
import io.healthplatform.chartcam.fhir.getLocalizedText
import io.healthplatform.chartcam.fhir.getMaxValue
import io.healthplatform.chartcam.fhir.getMinValue
import io.healthplatform.chartcam.fhir.isBodyMap
import io.healthplatform.chartcam.fhir.isFacialProfileSeries
import io.healthplatform.chartcam.fhir.isFitzpatrickPalette
import io.healthplatform.chartcam.fhir.isHidden
import io.healthplatform.chartcam.fhir.isSegmentedControl
import io.healthplatform.chartcam.fhir.isVisualPainControl
import io.healthplatform.chartcam.ui.components.FormBuilderDatePicker
import io.healthplatform.chartcam.ui.components.FormBuilderDateTimePicker
import io.healthplatform.chartcam.ui.components.FormBuilderMultiSelectDropdown
import io.healthplatform.chartcam.ui.components.FormBuilderNumericInput
import io.healthplatform.chartcam.ui.components.FormBuilderRangeSlider
import io.healthplatform.chartcam.ui.components.FormBuilderTextArea
import io.healthplatform.chartcam.ui.components.tabFocusNext
import io.healthplatform.chartcam.ui.sdc.controls.FacialSeriesCardControl
import io.healthplatform.chartcam.ui.theme.AppSpacing
import io.healthplatform.chartcam.utils.formatLocalizedDate
import io.healthplatform.chartcam.utils.formatLocalizedDateTime
import io.healthplatform.chartcam.utils.formatLocalizedDecimal
import org.jetbrains.compose.resources.pluralStringResource
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
    val onTakeVideoRequested: (String) -> Unit = onTakePhotoRequested,
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
 * @param onTakeVideoRequested Callback invoked when video capture is requested.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdcQuestionnaireForm(
    questionnaire: Questionnaire,
    answers: Map<String, Any>,
    config: SdcFormConfig = SdcFormConfig(),
    onFormUpdated: (Map<String, Any>, dev.ohs.fhir.model.r4.QuestionnaireResponse) -> Unit,
    onTakePhotoRequested: (String) -> Unit = {},
    onTakeVideoRequested: (String) -> Unit = onTakePhotoRequested,
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
                onTakeVideoRequested = onTakeVideoRequested,
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
 * @param onTakeVideoRequested Callback for when video capture is requested.
 */
@Composable
fun RenderQuestionnaireItem(
    item: Questionnaire.Item,
    state: SdcFormState,
    onAnswerChanged: (String, Any?) -> Unit,
    focusManager: FocusManager,
    onTakePhotoRequested: (String) -> Unit = {},
    onTakeVideoRequested: (String) -> Unit = onTakePhotoRequested,
) {
    val linkId = item.linkId.value
    val type = item.type.value

    if (linkId != null && type != null && !item.isHidden()) {
        RenderQuestionnaireItemImpl(
            item = item,
            state = state,
            onAnswerChanged = onAnswerChanged,
            focusManager = focusManager,
            onTakePhotoRequested = onTakePhotoRequested,
            onTakeVideoRequested = onTakeVideoRequested,
        )
    }
}

/**
 * Internal helper function.
 * @param item The item.
 * @param state The state.
 * @param onAnswerChanged The onAnswerChanged.
 * @param focusManager The focusManager.
 * @param onTakePhotoRequested The onTakePhotoRequested.
 * @param onTakeVideoRequested The onTakeVideoRequested.
 */
@Composable
private fun RenderQuestionnaireItemImpl(
    item: Questionnaire.Item,
    state: SdcFormState,
    onAnswerChanged: (String, Any?) -> Unit,
    focusManager: FocusManager,
    onTakePhotoRequested: (String) -> Unit,
    onTakeVideoRequested: (String) -> Unit,
) {
    val linkId = item.linkId.value!!
    val type = item.type.value!!
    val currentLang by io.healthplatform.chartcam.ui.currentLanguageState
        .collectAsState()
    val localizedText = item.getLocalizedText(currentLang)
    val displayLabel =
        if (localizedText.isNotBlank()) {
            localizedText
        } else if (type == Questionnaire.QuestionnaireItemType.Group) {
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
                onTakeVideoRequested = onTakeVideoRequested,
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
 * @param answerValue The answerValue.
 * @param isRequired The isRequired.
 * @return The result.
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

/**
 * Internal helper function to render a question group.
 * @param ctx The render context.
 */
@Composable
private fun RenderGroupItem(ctx: RenderContext) {
    if (ctx.item.isFacialProfileSeries()) {
        FacialSeriesCardControl(
            title = ctx.displayLabel,
            items = ctx.item.item,
            answers = ctx.state.answers,
            existingAttachments = ctx.state.config.attachments,
            readOnly = ctx.state.config.readOnly || ctx.item.readOnly?.value == true,
            onCaptureSeries = { ctx.onTakePhotoRequested(ctx.linkId) },
            onCaptureSingle = { slotLinkId -> ctx.onTakePhotoRequested(slotLinkId) },
        )
    } else if (ctx.item.repeats?.value == true) {
        RenderRepeatingGroupItem(ctx)
    } else {
        androidx.compose.material3.ElevatedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.sm)
                    .semantics {
                        contentDescription = ctx.displayLabel
                        heading()
                    },
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                Text(
                    text = ctx.displayLabel,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = AppSpacing.sm).semantics { heading() },
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
}

/**
 * Internal helper function for rendering repeating question groups.
 * @param ctx The render context.
 */
@Composable
private fun RenderRepeatingGroupItem(ctx: RenderContext) {
    val isReadOnly = ctx.state.config.readOnly || ctx.item.readOnly?.value == true
    val removeEntryLabel = stringResource(Res.string.remove_entry)
    val addEntryLabel = stringResource(Res.string.add_entry)

    var instanceCount by remember(ctx.linkId) {
        val existingMax =
            ctx.state.answers.keys
                .filter { it.startsWith("${ctx.linkId}#") }
                .mapNotNull { key ->
                    val afterHash = key.substringAfter("${ctx.linkId}#")
                    afterHash.substringBefore('.').toIntOrNull()
                }.maxOrNull()
        mutableStateOf(maxOf(1, (existingMax ?: 0) + 1))
    }

    androidx.compose.material3.ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.sm)
                .semantics {
                    contentDescription = ctx.displayLabel
                    heading()
                },
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Text(
                text = ctx.displayLabel,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = AppSpacing.sm).semantics { heading() },
            )

            for (i in 0 until instanceCount) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs),
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(Res.string.entry_format, i + 1),
                                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            )
                            if (instanceCount > 1 && !isReadOnly) {
                                TextButton(
                                    onClick = {
                                        if (instanceCount > 1) {
                                            instanceCount--
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("RemoveGroupEntry_${ctx.linkId}_$i")
                                            .semantics {
                                                contentDescription = removeEntryLabel
                                            },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(removeEntryLabel)
                                }
                            }
                        }

                        val scopedAnswers =
                            remember(ctx.state.answers, i) {
                                ctx.state.answers
                                    .mapNotNull { (k, v) ->
                                        if (k.startsWith("${ctx.linkId}#$i.")) {
                                            k.substringAfter("${ctx.linkId}#$i.") to v
                                        } else if (i == 0 && !k.contains('#')) {
                                            k to v
                                        } else {
                                            null
                                        }
                                    }.toMap()
                            }
                        val scopedState =
                            remember(ctx.state, scopedAnswers) {
                                ctx.state.copy(answers = scopedAnswers)
                            }

                        ctx.item.item.forEach { nestedItem ->
                            RenderQuestionnaireItem(
                                item = nestedItem,
                                state = scopedState,
                                onAnswerChanged = { childId, childVal ->
                                    val indexedKey = "${ctx.linkId}#$i.$childId"
                                    ctx.onAnswerChanged(indexedKey, childVal)
                                    if (i == 0) {
                                        ctx.onAnswerChanged(childId, childVal)
                                    }
                                },
                                focusManager = ctx.focusManager,
                                onTakePhotoRequested = ctx.onTakePhotoRequested,
                            )
                        }
                    }
                }
            }

            if (!isReadOnly) {
                OutlinedButton(
                    onClick = { instanceCount++ },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = AppSpacing.sm)
                            .minimumInteractiveComponentSize()
                            .testTag("AddGroupEntry_${ctx.linkId}")
                            .semantics {
                                contentDescription = addEntryLabel
                            },
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(addEntryLabel)
                }
            }
        }
    }
}

/**
 * Internal helper function.
 * @param ctx The ctx.
 * @param effectiveReadOnly The effectiveReadOnly.
 */
@Composable
private fun RenderInputItem(
    ctx: RenderContext,
    effectiveReadOnly: Boolean,
) {
    if (ctx.item.isVisualPainControl()) {
        RenderVisualPainField(ctx, effectiveReadOnly)
    } else if (ctx.item.isFitzpatrickPalette()) {
        RenderFitzpatrickField(ctx, effectiveReadOnly)
    } else if (ctx.item.isBodyMap()) {
        RenderBodyMapField(ctx, effectiveReadOnly)
    } else if (ctx.item.isSegmentedControl()) {
        RenderSegmentedChoiceField(ctx, effectiveReadOnly)
    } else if (effectiveReadOnly) {
        RenderReadOnlyField(ctx)
    } else {
        RenderEditableField(ctx)
    }

    if (ctx.item.item.isNotEmpty()) {
        Column(modifier = Modifier.padding(start = AppSpacing.md)) {
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

/**
 * Renders an interactive or read-only visual pain scale control.
 *
 * @param ctx The rendering context.
 * @param readOnly Whether the control is rendered in read-only mode.
 */
@Composable
private fun RenderVisualPainField(
    ctx: RenderContext,
    readOnly: Boolean,
) {
    val currentScore =
        (ctx.state.answers[ctx.linkId] as? Number)?.toInt()
            ?: (ctx.state.answers[ctx.linkId] as? String)?.toIntOrNull()
    io.healthplatform.chartcam.ui.sdc.controls.VisualPainScaleControl(
        value = currentScore,
        onValueChange = { ctx.onAnswerChanged(ctx.linkId, it) },
        label = ctx.displayLabel,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        readOnly = readOnly,
    )
}

/**
 * Renders an interactive or read-only Fitzpatrick skin phototyping swatch control.
 *
 * @param ctx The rendering context.
 * @param readOnly Whether the control is rendered in read-only mode.
 */
@Composable
private fun RenderFitzpatrickField(
    ctx: RenderContext,
    readOnly: Boolean,
) {
    val currentAnswer = ctx.state.answers[ctx.linkId]
    val selectedType =
        when (currentAnswer) {
            is io.healthplatform.chartcam.models.FitzpatrickSkinType -> currentAnswer
            is String -> {
                io.healthplatform.chartcam.models.FitzpatrickScaleDefaults.ALL_TYPES.firstOrNull {
                    it.romanNumeral.equals(currentAnswer.removePrefix("Type ").trim(), ignoreCase = true) ||
                        it.type.toString() == currentAnswer
                }
            }
            is Number -> {
                io.healthplatform.chartcam.models.FitzpatrickScaleDefaults.ALL_TYPES.firstOrNull {
                    it.type == currentAnswer.toInt()
                }
            }
            else -> null
        }
    io.healthplatform.chartcam.ui.sdc.controls.FitzpatrickPaletteControl(
        selectedType = selectedType,
        onTypeSelected = { ctx.onAnswerChanged(ctx.linkId, "Type ${it.romanNumeral}") },
        label = ctx.displayLabel,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        readOnly = readOnly,
    )
}

/**
 * Renders an interactive or read-only anatomical body map pin-drop control.
 *
 * @param ctx The rendering context.
 * @param readOnly Whether the control is rendered in read-only mode.
 */
@Composable
private fun RenderBodyMapField(
    ctx: RenderContext,
    readOnly: Boolean,
) {
    val currentAnswer = ctx.state.answers[ctx.linkId]
    val location =
        when (currentAnswer) {
            is io.healthplatform.chartcam.models.BodyMapLocation -> currentAnswer
            is String -> {
                if (currentAnswer.isNotBlank()) {
                    io.healthplatform.chartcam.models.BodyMapLocation(
                        regionId = "custom",
                        displayName = currentAnswer,
                        xPercent = 50f,
                        yPercent = 30f,
                    )
                } else {
                    null
                }
            }
            else -> null
        }
    io.healthplatform.chartcam.ui.sdc.controls.BodyMapPinDropControl(
        location = location,
        onLocationChanged = { ctx.onAnswerChanged(ctx.linkId, it?.toSerializedString()) },
        label = ctx.displayLabel,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        readOnly = readOnly,
    )
}

/**
 * Renders an interactive or read-only segmented choice tiles control.
 *
 * @param ctx The rendering context.
 * @param readOnly Whether the control is rendered in read-only mode.
 */
@Composable
private fun RenderSegmentedChoiceField(
    ctx: RenderContext,
    readOnly: Boolean,
) {
    val options =
        ctx.item.answerOption.mapNotNull { opt ->
            val coding = opt.value as? Questionnaire.Item.AnswerOption.Value.Coding
            coding?.value?.display?.value
                ?: opt.value
                    .asString()
                    ?.value
                    ?.value
        }
    val isMultiSelect = ctx.item.repeats?.value == true
    val currentAnswer = ctx.state.answers[ctx.linkId]
    val selectedOptions: List<String> =
        when (currentAnswer) {
            is List<*> -> currentAnswer.filterIsInstance<String>()
            is String -> if (currentAnswer.isNotBlank()) listOf(currentAnswer) else emptyList()
            else -> emptyList()
        }

    io.healthplatform.chartcam.ui.sdc.controls.SegmentedVisualTilesControl(
        selectedOptions = selectedOptions,
        options = options,
        onOptionToggled = { option ->
            if (isMultiSelect) {
                val updated =
                    if (selectedOptions.contains(option)) {
                        selectedOptions - option
                    } else {
                        selectedOptions + option
                    }
                ctx.onAnswerChanged(ctx.linkId, updated)
            } else {
                ctx.onAnswerChanged(ctx.linkId, option)
            }
        },
        label = ctx.displayLabel,
        isMultiSelect = isMultiSelect,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        readOnly = readOnly,
    )
}

/**
 * Internal helper function.
 * @param ctx The ctx.
 */
@Composable
private fun RenderReadOnlyField(ctx: RenderContext) {
    val answerDisplay = getAnswerDisplayText(ctx.type, ctx.item, ctx.linkId, ctx.state.answers)

    if (ctx.type == Questionnaire.QuestionnaireItemType.Attachment) {
        RenderReadOnlyAttachment(ctx)
    } else {
        val notAnsweredString = stringResource(Res.string.not_answered)
        val disp = if (answerDisplay.isNotBlank()) answerDisplay else notAnsweredString
        val readOnlyContentDescription = stringResource(Res.string.label_value_format, ctx.displayLabel, disp)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.xs)
                    .semantics(mergeDescendants = true) {
                        contentDescription = readOnlyContentDescription
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
                modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.xs),
            ) {
                if (answerDisplay.isNotBlank()) {
                    Text(
                        text = answerDisplay,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(AppSpacing.sm),
                    )
                } else {
                    Text(
                        text = notAnsweredString,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(AppSpacing.sm),
                    )
                }
            }
        }
    }
}

/**
 * Internal helper function.
 * @param type The type.
 * @param item The item.
 * @param linkId The linkId.
 * @param answers The answers.
 * @return The result.
 */
@Composable
private fun getAnswerDisplayText(
    type: Questionnaire.QuestionnaireItemType,
    item: Questionnaire.Item,
    linkId: String,
    answers: Map<String, Any>,
): String {
    val currentLang by io.healthplatform.chartcam.ui.currentLanguageState
        .collectAsState()
    return when (type) {
        Questionnaire.QuestionnaireItemType.Boolean -> getBooleanAnswerText(answers[linkId] as? Boolean)
        Questionnaire.QuestionnaireItemType.Choice -> getChoiceAnswerText(item, answers[linkId], currentLang)
        Questionnaire.QuestionnaireItemType.Integer -> getIntegerAnswerText(answers[linkId], currentLang)
        Questionnaire.QuestionnaireItemType.Decimal -> getIntegerAnswerText(answers[linkId], currentLang)
        Questionnaire.QuestionnaireItemType.Date -> getDateAnswerText(answers[linkId], currentLang)
        Questionnaire.QuestionnaireItemType.DateTime -> getDateTimeAnswerText(answers[linkId], currentLang)
        Questionnaire.QuestionnaireItemType.Attachment -> ""
        else -> answers[linkId]?.toString() ?: ""
    }
}

/**
 * Internal helper function to format a localized date answer string.
 *
 * @param answer The raw date answer.
 * @param language The current language tag.
 * @return The localized date string, or raw representation.
 */
private fun getDateAnswerText(
    answer: Any?,
    language: String,
): String {
    val raw: String =
        when (answer) {
            is String -> answer
            is dev.ohs.fhir.model.r4.Date -> answer.value?.toString() ?: ""
            else -> answer?.toString() ?: ""
        }
    if (raw.isBlank()) return ""
    return runCatching {
        formatLocalizedDate(raw, language)
    }.getOrDefault(raw)
}

/**
 * Internal helper function to format a localized datetime answer string.
 *
 * @param answer The raw datetime answer.
 * @param language The current language tag.
 * @return The localized datetime string, or raw representation.
 */
private fun getDateTimeAnswerText(
    answer: Any?,
    language: String,
): String {
    val raw: String =
        when (answer) {
            is String -> answer
            is dev.ohs.fhir.model.r4.DateTime -> answer.value?.toString() ?: ""
            else -> answer?.toString() ?: ""
        }
    if (raw.isBlank()) return ""
    return runCatching {
        formatLocalizedDateTime(raw, language)
    }.getOrDefault(raw)
}

/**
 * Internal helper function.
 * @param checked The checked.
 * @return The result.
 */
@Composable
private fun getBooleanAnswerText(checked: Boolean?): String {
    if (checked == null) return ""
    return if (checked) stringResource(Res.string.yes) else stringResource(Res.string.no)
}

/**
 * Internal helper function.
 * @param item The item.
 * @param answer The answer.
 * @param language The language tag to choose the appropriate list separator.
 * @return The result.
 */
private fun getChoiceAnswerText(
    item: Questionnaire.Item,
    answer: Any?,
    language: String,
): String {
    if (item.repeats?.value == true) {
        val list = (answer as? List<*>)?.filterIsInstance<String>()
        val separator =
            when (language.lowercase().split("-", "_").first()) {
                "zh", "ja" -> "、"
                "ar", "fa", "ur" -> "، "
                else -> ", "
            }
        return list?.joinToString(separator) ?: ""
    }
    return answer as? String ?: ""
}

/**
 * Internal helper function.
 * @param answer The answer.
 * @param language The language tag to format the decimal answer.
 * @return The result.
 */
private fun getIntegerAnswerText(
    answer: Any?,
    language: String,
): String {
    val v = (answer as? Number)?.toDouble() ?: (answer as? String)?.toDoubleOrNull()
    return v?.let {
        val places = if (it % 1.0 == 0.0) 0 else 2
        formatLocalizedDecimal(it, language, places)
    } ?: ""
}

/**
 * Internal helper function.
 * @param relatedAttachments The relatedAttachments.
 */

@Composable
private fun RenderAttachmentGrid(relatedAttachments: List<dev.ohs.fhir.model.r4.DocumentReference>) {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns =
            androidx.compose.foundation.lazy.grid.GridCells
                .Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        modifier =
            Modifier
                .fillMaxWidth()
                .height((PHOTO_GRID_ITEM_HEIGHT * ((relatedAttachments.size + 1) / 2)).dp)
                .padding(vertical = AppSpacing.sm),
    ) {
        items(relatedAttachments) { photo ->
            io.healthplatform.chartcam.ui
                .PhotoGridItem(photo)
        }
    }
}

/**
 * Renders read only attachment.
 * @param ctx The ctx.
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
        val attachmentsCountText =
            pluralStringResource(
                Res.plurals.attachments_count,
                relatedAttachments.size,
                relatedAttachments.size,
            )
        val cdWithAttachments = stringResource(Res.string.label_value_format, ctx.displayLabel, attachmentsCountText)
        Column(
            modifier =
                Modifier.fillMaxWidth().padding(vertical = AppSpacing.sm).semantics(mergeDescendants = true) {
                    contentDescription = cdWithAttachments
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
        val cdNoAttachments = stringResource(Res.string.label_value_format, ctx.displayLabel, notAnsweredString)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.sm)
                    .semantics(mergeDescendants = true) {
                        contentDescription = cdNoAttachments
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

/**
 * Internal helper function.
 * @param ctx The ctx.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
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

/**
 * Internal helper function.
 * @param ctx The ctx.
 */
@Composable
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
                .padding(vertical = AppSpacing.sm)
                .semantics {
                    if (ctx.isError && ctx.errorMessage != null) {
                        error(ctx.errorMessage)
                        liveRegion = LiveRegionMode.Polite
                    }
                }.tabFocusNext(ctx.focusManager),
    )
}

/**
 * Internal helper function.
 * @param ctx The ctx.
 */
@Composable
private fun RenderBooleanField(ctx: RenderContext) {
    val checked = ctx.state.answers[ctx.linkId] as? Boolean ?: false
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("CheckboxRow ${ctx.displayLabel}")
                .toggleable(
                    value = checked,
                    onValueChange = { ctx.onAnswerChanged(ctx.linkId, it) },
                    role = Role.Checkbox,
                ).padding(vertical = AppSpacing.sm)
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
            modifier = Modifier.padding(start = AppSpacing.sm),
        )
    }
}

/**
 * Internal helper function.
 * @param ctx The ctx.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderChoiceField(ctx: RenderContext) {
    var expanded by remember { mutableStateOf(false) }
    val options =
        ctx.item.answerOption.mapNotNull { option ->
            val v1 =
                option.value
                    .asString()
                    ?.value
                    ?.value
            val v2 =
                option.value
                    .asCoding()
                    ?.value
                    ?.display
                    ?.value
            val v3 =
                option.value
                    .asCoding()
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

/**
 * Internal helper function.
 * @param ctx The ctx.
 * @param itemControl The itemControl.
 * @param isMultiSelect The isMultiSelect.
 * @param options The options.
 */
@Composable
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
                .padding(vertical = AppSpacing.sm)
                .semantics {
                    if (ctx.isError && ctx.errorMessage != null) {
                        error(ctx.errorMessage)
                    }
                }.then(if (!isMultiSelect) Modifier.selectableGroup() else Modifier),
    ) {
        io.healthplatform.chartcam.ui.components.FormLabel(
            ctx.displayLabel,
            ctx.isRequired,
            modifier = Modifier.padding(bottom = AppSpacing.xs),
        )
        if (ctx.isError && ctx.errorMessage != null) {
            Text(
                ctx.errorMessage,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier =
                    Modifier
                        .padding(bottom = AppSpacing.xs)
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        options.forEach { option ->
            RenderRadioOrCheckboxOption(ctx, option, isMultiSelect, isCheckboxes, selectedOptions)
        }
    }
}

/**
 * Internal helper function.
 * @param ctx The ctx.
 * @param option The option.
 * @param isMultiSelect The isMultiSelect.
 * @param isCheckboxes The isCheckboxes.
 * @param selectedOptions The selectedOptions.
 */
@Composable
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
                .minimumInteractiveComponentSize()
                .semantics(mergeDescendants = true) {}
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
                ).padding(vertical = AppSpacing.xs),
    ) {
        if (isCheckboxes) {
            androidx.compose.material3.Checkbox(checked = isSelected, onCheckedChange = null)
        } else {
            androidx.compose.material3.RadioButton(selected = isSelected, onClick = null)
        }
        Text(text = option, modifier = Modifier.padding(start = AppSpacing.sm))
    }
}

/**
 * Internal helper function.
 * @param ctx The ctx.
 * @param options The options.
 * @param expanded The expanded.
 * @param onExpandedChange The onExpandedChange.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
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
                .padding(vertical = AppSpacing.sm),
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
            supportingText = {
                if (ctx.isError && ctx.errorMessage != null) {
                    Text(
                        text = ctx.errorMessage,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier =
                Modifier
                    .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .semantics {
                        if (ctx.isError && ctx.errorMessage != null) error(ctx.errorMessage)
                    }.tabFocusNext(ctx.focusManager),
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

/**
 * Internal helper function.
 * @param ctx The ctx.
 */
@Composable
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

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.sm)) {
        io.healthplatform.chartcam.ui.components.FormLabel(
            text = ctx.displayLabel,
            isRequired = ctx.isRequired,
            modifier =
                Modifier.semantics(mergeDescendants = true) {
                    contentDescription = ctx.displayLabel
                },
        )

        val isVideo = ctx.item.getItemControl() == "video"
        if (isVideo) {
            io.healthplatform.chartcam.ui.components.FormBuilderVideoCamera(
                label = ctx.displayLabel,
                onClick = { ctx.onTakeVideoRequested(ctx.linkId) },
                modifier =
                    Modifier
                        .padding(top = AppSpacing.sm)
                        .minimumInteractiveComponentSize()
                        .testTag("AttachmentCaptureButton ${ctx.linkId}"),
            )
        } else {
            val buttonContentDescription =
                stringResource(Res.string.cd_take_photo_for_item, ctx.displayLabel)
            Button(
                onClick = { ctx.onTakePhotoRequested(ctx.linkId) },
                modifier =
                    Modifier
                        .padding(top = AppSpacing.sm)
                        .minimumInteractiveComponentSize()
                        .testTag("AttachmentCaptureButton ${ctx.linkId}")
                        .semantics {
                            contentDescription = buttonContentDescription
                        },
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.padding(end = AppSpacing.sm),
                )
                Text(stringResource(Res.string.take_photo))
            }
        }

        if (relatedAttachments.isNotEmpty()) {
            RenderAttachmentGrid(relatedAttachments)
        }
    }
}

/**
 * Internal helper function.
 * @param ctx The ctx.
 */
@Composable
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
            Modifier.semantics {
                if (ctx.isError && ctx.errorMessage != null) {
                    error(ctx.errorMessage)
                    liveRegion = LiveRegionMode.Polite
                }
            },
    )
}

/**
 * Internal helper function.
 * @param ctx The ctx.
 */
@Composable
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

/**
 * Internal helper function.
 * @param ctx The ctx.
 */
@Composable
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

/**
 * Internal helper function.
 * @param ctx The ctx.
 */
@Composable
private fun RenderDecimalField(ctx: RenderContext) {
    val rawAns = ctx.state.answers[ctx.linkId]
    val text =
        when (rawAns) {
            is Number -> rawAns.toString()
            is String -> rawAns
            else -> ""
        }
    FormBuilderNumericInput(
        value = text,
        onValueChange = { ctx.onAnswerChanged(ctx.linkId, it) },
        label = ctx.displayLabel,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        modifier =
            Modifier.semantics {
                if (ctx.isError && ctx.errorMessage != null) {
                    error(ctx.errorMessage)
                    liveRegion = LiveRegionMode.Polite
                }
            },
    )
}

/**
 * Internal helper function.
 * @param ctx The ctx.
 */
@Composable
private fun RenderIntegerField(ctx: RenderContext) {
    val minValue = ctx.item.getMinValue() ?: DEFAULT_MIN_VALUE
    val maxValue = ctx.item.getMaxValue() ?: DEFAULT_MAX_VALUE
    val rawAns = ctx.state.answers[ctx.linkId]
    val val1 = (rawAns as? Number)?.toFloat()
    val val2 = (rawAns as? String)?.toFloatOrNull()
    val value = val1 ?: val2 ?: minValue
    val steps = ((maxValue - minValue).toInt() - 1).coerceAtLeast(0)
    io.healthplatform.chartcam.ui.components.FormBuilderRangeSlider(
        value = value,
        valueRange = minValue..maxValue,
        steps = steps,
        onValueChange = { ctx.onAnswerChanged(ctx.linkId, kotlin.math.round(it)) },
        label = ctx.displayLabel,
        isRequired = ctx.isRequired,
        isError = ctx.isError,
        errorMessage = ctx.errorMessage,
        modifier =
            Modifier.semantics {
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
): Boolean = SdcEvaluator.isItemEnabled(item, answers)
