/**
 * @file QuestionnaireBuilderScreen.kt
 * Contains the UI for the Questionnaire Builder feature.
 * This allows dynamic creation and editing of FHIR Questionnaires.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.add_option
import chartcam.chartcam.generated.resources.add_widget
import chartcam.chartcam.generated.resources.at_least_one_option_required
import chartcam.chartcam.generated.resources.build_questionnaire
import chartcam.chartcam.generated.resources.bullet_format
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.cd_back
import chartcam.chartcam.generated.resources.cd_delete_item
import chartcam.chartcam.generated.resources.cd_delete_option
import chartcam.chartcam.generated.resources.cd_more_widgets
import chartcam.chartcam.generated.resources.cd_move_down
import chartcam.chartcam.generated.resources.cd_move_up
import chartcam.chartcam.generated.resources.cd_preview
import chartcam.chartcam.generated.resources.cd_save
import chartcam.chartcam.generated.resources.confirm_delete_item_message
import chartcam.chartcam.generated.resources.confirm_delete_item_title
import chartcam.chartcam.generated.resources.confirm_delete_option_message
import chartcam.chartcam.generated.resources.confirm_delete_option_title
import chartcam.chartcam.generated.resources.delete
import chartcam.chartcam.generated.resources.error_duplicate_id
import chartcam.chartcam.generated.resources.error_required_field
import chartcam.chartcam.generated.resources.label
import chartcam.chartcam.generated.resources.preview_mode
import chartcam.chartcam.generated.resources.questionnaire_title
import chartcam.chartcam.generated.resources.type_format
import chartcam.chartcam.generated.resources.widget_checkbox
import chartcam.chartcam.generated.resources.widget_date
import chartcam.chartcam.generated.resources.widget_datetime
import chartcam.chartcam.generated.resources.widget_multi_line_text
import chartcam.chartcam.generated.resources.widget_multi_select
import chartcam.chartcam.generated.resources.widget_numeric
import chartcam.chartcam.generated.resources.widget_photo_camera
import chartcam.chartcam.generated.resources.widget_range
import chartcam.chartcam.generated.resources.widget_single_line_text
import chartcam.chartcam.generated.resources.widget_single_select
import chartcam.chartcam.generated.resources.widget_switch
import chartcam.chartcam.generated.resources.widget_video_camera
import io.healthplatform.chartcam.sdc.SdcQuestionnaireForm
import io.healthplatform.chartcam.ui.components.tabFocusNext
import io.healthplatform.chartcam.viewmodel.BuilderItem
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderState
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import io.healthplatform.chartcam.viewmodel.WidgetType
import org.jetbrains.compose.resources.stringResource

/**
 * Translates a [WidgetType] to its localized string representation for UI display.
 *
 * @param type The type of the widget.
 * @return The localized string name for the widget.
 */
@Composable
fun getWidgetNameString(type: WidgetType): String {
    val res =
        when (type) {
            WidgetType.PHOTO_CAMERA -> Res.string.widget_photo_camera
            WidgetType.VIDEO_CAMERA -> Res.string.widget_video_camera
            WidgetType.SWITCH -> Res.string.widget_switch
            WidgetType.CHECKBOX -> Res.string.widget_checkbox
            WidgetType.SINGLE_SELECT -> Res.string.widget_single_select
            WidgetType.MULTI_SELECT -> Res.string.widget_multi_select
            WidgetType.SINGLE_LINE_TEXT -> Res.string.widget_single_line_text
            WidgetType.MULTI_LINE_TEXT -> Res.string.widget_multi_line_text
            WidgetType.DATE -> Res.string.widget_date
            WidgetType.DATETIME -> Res.string.widget_datetime
            WidgetType.NUMERIC -> Res.string.widget_numeric
            WidgetType.RANGE -> Res.string.widget_range
        }
    return stringResource(res)
}

/**
 * Maps a [WidgetType] to its corresponding [ImageVector] icon for display in the builder UI.
 *
 * @param type The type of the widget.
 * @return The material icon representing the widget type.
 */
fun getWidgetIcon(type: WidgetType): ImageVector =
    when (type) {
        WidgetType.PHOTO_CAMERA -> Icons.Default.PhotoCamera
        WidgetType.VIDEO_CAMERA -> Icons.Default.Videocam
        WidgetType.SWITCH -> Icons.Default.ToggleOn
        WidgetType.CHECKBOX -> Icons.Default.CheckBox
        WidgetType.SINGLE_SELECT -> Icons.Default.RadioButtonChecked
        WidgetType.MULTI_SELECT -> Icons.Default.Checklist
        WidgetType.SINGLE_LINE_TEXT -> Icons.AutoMirrored.Filled.ShortText
        WidgetType.MULTI_LINE_TEXT -> Icons.AutoMirrored.Filled.Notes
        WidgetType.DATE -> Icons.Default.DateRange
        WidgetType.DATETIME -> Icons.Default.AccessTime
        WidgetType.NUMERIC -> Icons.Default.Numbers
        WidgetType.RANGE -> Icons.Default.LinearScale
    }

/**
 * Secondary widget dropdown.
 *
 * @param secondaryWidgets The secondary widgets list.
 * @param onWidgetSelected The callback on widget selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecondaryWidgetDropdown(
    secondaryWidgets: List<WidgetType>,
    onWidgetSelected: (WidgetType) -> Unit,
) {
    var showDropdown by remember { mutableStateOf(false) }
    Box {
        TooltipBox(
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above,
                ),
            tooltip = { PlainTooltip { Text(stringResource(Res.string.cd_more_widgets)) } },
            state = rememberTooltipState(),
        ) {
            IconButton(onClick = { showDropdown = true }) {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = stringResource(Res.string.cd_more_widgets),
                )
            }
        }
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
        ) {
            secondaryWidgets.forEach { widget ->
                DropdownMenuItem(
                    text = { Text(getWidgetNameString(widget)) },
                    leadingIcon = {
                        Icon(
                            getWidgetIcon(widget),
                            contentDescription = getWidgetNameString(widget),
                        )
                    },
                    onClick = {
                        onWidgetSelected(widget)
                        showDropdown = false
                    },
                )
            }
        }
    }
}

/**
 * Displays a row for selecting a widget type to add to the questionnaire.
 * Shows primary widgets as icons and a dropdown for secondary widgets.
 *
 * @param onWidgetSelected Callback invoked when a widget type is selected.
 * @param modifier The modifier to apply to this row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSelectionRow(
    onWidgetSelected: (WidgetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryWidgets =
        listOf(
            WidgetType.SINGLE_LINE_TEXT,
            WidgetType.PHOTO_CAMERA,
            WidgetType.SINGLE_SELECT,
            WidgetType.CHECKBOX,
            WidgetType.DATE,
        )
    val secondaryWidgets = WidgetType.entries - primaryWidgets.toSet()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(Res.string.add_widget), style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            primaryWidgets.forEach { widget ->
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(
                            positioning = TooltipAnchorPosition.Above,
                        ),
                    tooltip = { PlainTooltip { Text(getWidgetNameString(widget)) } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(onClick = { onWidgetSelected(widget) }) {
                        Icon(
                            getWidgetIcon(widget),
                            contentDescription = getWidgetNameString(widget),
                        )
                    }
                }
            }

            SecondaryWidgetDropdown(secondaryWidgets, onWidgetSelected)
        }
    }
}

/**
 * Screen that allows users to build a dynamic FHIR Questionnaire.
 *
 * @param viewModel The view model managing the builder state.
 * @param onBack Callback when the back button is pressed.
 * @param onSaved Callback when the questionnaire is successfully saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireBuilderScreen(
    viewModel: QuestionnaireBuilderViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.build_questionnaire),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePreviewMode() }) {
                        Icon(Icons.Default.Preview, contentDescription = stringResource(Res.string.cd_preview))
                    }
                    IconButton(onClick = {
                        val id = viewModel.saveQuestionnaire()
                        if (id != null) {
                            onSaved(id)
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(Res.string.cd_save))
                    }
                },
            )
        },
    ) { paddingValues ->
        QuestionnaireBuilderContent(paddingValues, state, viewModel, focusRequesters)
    }
}

/**
 * Actions for a builder item row.
 *
 * @property onUpdate Callback to update the item's label and options.
 * @property onDelete Callback to delete the item.
 * @property onMoveUp Callback to move the item up.
 * @property onMoveDown Callback to move the item down.
 */
data class BuilderItemRowActions(
    val onUpdate: (String, List<String>) -> Unit,
    val onDelete: () -> Unit,
    val onMoveUp: () -> Unit,
    val onMoveDown: () -> Unit,
)

/**
 * Delete option confirm dialog.
 *
 * @param onDismiss The dismiss callback.
 * @param onConfirm The confirm callback.
 */
@Composable
private fun DeleteOptionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(Res.string.confirm_delete_option_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(Res.string.confirm_delete_option_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

/**
 * Option item row.
 *
 * @param option Option text.
 * @param onDelete The delete callback.
 */
@Composable
private fun OptionItemRow(
    option: String,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(Res.string.bullet_format, option), modifier = Modifier.weight(1f))
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(Res.string.cd_delete_option),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Add option field.
 *
 * @param item The builder item.
 * @param focusManager The focus manager.
 * @param actions The actions.
 */
@Composable
private fun AddOptionField(
    item: BuilderItem,
    focusManager: FocusManager,
    actions: BuilderItemRowActions,
) {
    var newOptionText by remember { mutableStateOf("") }
    val handleAddOption = {
        if (newOptionText.isNotBlank()) {
            actions.onUpdate(item.label, item.options + newOptionText.trim())
            newOptionText = ""
        }
    }

    OutlinedTextField(
        value = newOptionText,
        onValueChange = { newOptionText = it },
        label = { Text(stringResource(Res.string.add_option)) },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .onKeyEvent {
                    if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                        val d = if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
                        focusManager.moveFocus(d)
                        true
                    } else if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                        handleAddOption()
                        true
                    } else {
                        false
                    }
                },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
        trailingIcon = {
            IconButton(onClick = handleAddOption) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_option))
            }
        },
    )
}

/**
 * Internal helper for options.
 *
 * @param item The builder item.
 * @param focusManager Focus manager.
 * @param actions The actions.
 */
@Composable
fun BuilderItemOptions(
    item: BuilderItem,
    focusManager: FocusManager,
    actions: BuilderItemRowActions,
) {
    if (item.widgetType != WidgetType.SINGLE_SELECT && item.widgetType != WidgetType.MULTI_SELECT) {
        return
    }

    val noOptionsError = item.isError && item.options.isEmpty()
    var optionToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    if (optionToDeleteIndex != null) {
        DeleteOptionDialog(
            onDismiss = { optionToDeleteIndex = null },
            onConfirm = {
                val index = optionToDeleteIndex
                if (index != null) {
                    val newOptions = item.options.toMutableList().apply { removeAt(index) }
                    actions.onUpdate(item.label, newOptions)
                }
                optionToDeleteIndex = null
            },
        )
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        if (noOptionsError) {
            Text(
                stringResource(Res.string.at_least_one_option_required),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        item.options.forEachIndexed { index, option ->
            OptionItemRow(option, onDelete = { optionToDeleteIndex = index })
        }

        AddOptionField(item, focusManager, actions)
    }
}

/**
 * Actions for builder item row.
 *
 * @param canMoveUp Can move up.
 * @param canMoveDown Can move down.
 * @param actions The actions.
 * @param onShowDeleteConfirm Delete confirm callback.
 */
@Composable
private fun BuilderItemActionButtons(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    actions: BuilderItemRowActions,
    onShowDeleteConfirm: () -> Unit,
) {
    Column {
        IconButton(onClick = actions.onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(Res.string.cd_move_up))
        }
        IconButton(onClick = onShowDeleteConfirm) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.cd_delete_item))
        }
        IconButton(onClick = actions.onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(Res.string.cd_move_down))
        }
    }
}

/**
 * Delete item confirm dialog.
 *
 * @param onDismiss The dismiss callback.
 * @param onConfirm The confirm callback.
 */
@Composable
private fun DeleteItemDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(Res.string.confirm_delete_item_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(Res.string.confirm_delete_item_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

/**
 * Renders a delete confirmation dialog.
 *
 * @param show Whether to show the dialog.
 * @param actions The row actions.
 * @param dismiss The dismiss callback.
 */
@Composable
private fun RenderDeleteConfirm(
    show: Boolean,
    actions: BuilderItemRowActions,
    dismiss: () -> Unit,
) {
    if (show) {
        DeleteItemDialog(
            onDismiss = dismiss,
            onConfirm = {
                dismiss()
                actions.onDelete()
            },
        )
    }
}

/**
 * Composable for displaying a single builder item row.
 *
 * @param item The builder item to display.
 * @param canMoveUp Whether the item can be moved up.
 * @param canMoveDown Whether the item can be moved down.
 * @param actions The actions that can be performed on this item.
 * @param modifier The modifier to be applied to this item row.
 */
@Composable
fun BuilderItemRow(
    item: BuilderItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    actions: BuilderItemRowActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    RenderDeleteConfirm(showDeleteConfirm, actions, { showDeleteConfirm = false })

    val focusRequester = remember { FocusRequester() }
    val borderColor = if (item.isError) MaterialTheme.colorScheme.error else null

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .semantics {
                    if (item.isError) {
                        error("Validation error in item")
                    }
                },
        border = borderColor?.let { BorderStroke(1.dp, it) },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.type_format, getWidgetNameString(item.widgetType)),
                    style = MaterialTheme.typography.bodySmall,
                    color = borderColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = item.label,
                    onValueChange = { actions.onUpdate(it, item.options) },
                    label = { Text(stringResource(Res.string.label)) },
                    isError = item.isError && item.label.isBlank(),
                    supportingText = {
                        if (item.isError && item.label.isBlank()) {
                            Text(stringResource(Res.string.error_required_field))
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .focusRequester(focusRequester)
                            .tabFocusNext(focusManager),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                )

                BuilderItemOptions(item, focusManager, actions)
            }
            BuilderItemActionButtons(canMoveUp, canMoveDown, actions) { showDeleteConfirm = true }
        }
    }
}

/**
 * List of builder items.
 *
 * @param itemsList The list.
 * @param viewModel The view model.
 * @param focusRequesters Focus requesters.
 */
@Composable
private fun BuilderItemList(
    itemsList: List<BuilderItem>,
    viewModel: QuestionnaireBuilderViewModel,
    focusRequesters: MutableMap<String, FocusRequester>,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = itemsList.size,
            key = { index -> itemsList[index].linkId },
        ) { index ->
            val item = itemsList[index]
            val currentFocusRequester = focusRequesters.getOrPut(item.linkId) { FocusRequester() }

            BuilderItemRow(
                item = item,
                canMoveUp = index > 0,
                canMoveDown = index < itemsList.size - 1,
                actions =
                    BuilderItemRowActions(
                        onUpdate = { newLabel, newOptions ->
                            viewModel.updateItem(item.linkId, newLabel, newOptions)
                        },
                        onDelete = {
                            focusRequesters.remove(item.linkId)
                            viewModel.removeItem(item.linkId)
                        },
                        onMoveUp = { viewModel.moveItemUp(item.linkId) },
                        onMoveDown = { viewModel.moveItemDown(item.linkId) },
                    ),
                modifier =
                    Modifier
                        .focusProperties {
                            next =
                                if (index < itemsList.size - 1) {
                                    focusRequesters.getOrPut(itemsList[index + 1].linkId) { FocusRequester() }
                                } else {
                                    FocusRequester.Default
                                }
                            previous =
                                if (index > 0) {
                                    focusRequesters.getOrPut(itemsList[index - 1].linkId) { FocusRequester() }
                                } else {
                                    FocusRequester.Default
                                }
                        }.focusRequester(currentFocusRequester),
            )
        }
    }
}

/**
 * Preview mode for the questionnaire builder.
 *
 * @param state The builder state.
 * @param viewModel The view model.
 */
@Composable
private fun PreviewQuestionnaire(
    state: QuestionnaireBuilderState,
    viewModel: QuestionnaireBuilderViewModel,
) {
    Text(
        stringResource(Res.string.preview_mode),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 16.dp),
    )

    val previewQuestionnaire =
        remember(state.items, state.title) {
            viewModel.buildQuestionnaire()
        }
    var previewAnswers by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            SdcQuestionnaireForm(
                questionnaire = previewQuestionnaire,
                answers = previewAnswers,
                onFormUpdated = { updatedAnswers, _ ->
                    previewAnswers = updatedAnswers
                },
            )
        }
    }
}

/**
 * Builder active view.
 *
 * @param state The state.
 * @param viewModel The VM.
 * @param focusRequesters Focus requesters.
 * @param focusManager Focus manager.
 */
@Composable
private fun BuilderActiveView(
    state: QuestionnaireBuilderState,
    viewModel: QuestionnaireBuilderViewModel,
    focusRequesters: MutableMap<String, FocusRequester>,
    focusManager: FocusManager,
) {
    if (state.isDuplicateNameError) {
        Text(
            text = stringResource(Res.string.error_duplicate_id),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    val titleError = state.title.isBlank()
    OutlinedTextField(
        value = state.title,
        onValueChange = { viewModel.updateTitle(it) },
        label = { Text(stringResource(Res.string.questionnaire_title)) },
        isError = titleError,
        supportingText = {
            if (titleError) {
                Text(stringResource(Res.string.error_required_field))
            }
        },
        singleLine = true,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).tabFocusNext(focusManager),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
    )

    WidgetSelectionRow(
        onWidgetSelected = { viewModel.addItem(it) },
        modifier = Modifier.padding(bottom = 8.dp),
    )

    BuilderItemList(state.items, viewModel, focusRequesters)
}

/**
 * Content of the builder screen.
 *
 * @param paddingValues Padding values.
 * @param state The state.
 * @param viewModel The view model.
 * @param focusRequesters Focus requesters map.
 */
@Composable
private fun QuestionnaireBuilderContent(
    paddingValues: PaddingValues,
    state: QuestionnaireBuilderState,
    viewModel: QuestionnaireBuilderViewModel,
    focusRequesters: MutableMap<String, FocusRequester>,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
    ) {
        if (state.isPreviewMode) {
            PreviewQuestionnaire(state, viewModel)
        } else {
            BuilderActiveView(state, viewModel, focusRequesters, focusManager)
        }
    }
}
