package io.healthplatform.chartcam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.add_widget
import chartcam.chartcam.generated.resources.build_questionnaire
import chartcam.chartcam.generated.resources.cd_back
import chartcam.chartcam.generated.resources.cd_delete_item
import chartcam.chartcam.generated.resources.cd_more_widgets
import chartcam.chartcam.generated.resources.cd_preview
import chartcam.chartcam.generated.resources.cd_save
import chartcam.chartcam.generated.resources.items
import chartcam.chartcam.generated.resources.label
import chartcam.chartcam.generated.resources.options_comma_separated
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
import io.healthplatform.chartcam.viewmodel.BuilderItem
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import io.healthplatform.chartcam.viewmodel.WidgetType
import org.jetbrains.compose.resources.stringResource

/**
 * Maps a [WidgetType] to its corresponding [ImageVector] icon for display in the builder UI.
 *
 * @param type The type of the widget.
 * @return The material icon representing the widget type.
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
 * Displays a row for selecting a widget type to add to the questionnaire.
 * Shows primary widgets as icons and a dropdown for secondary widgets.
 *
 * @param onWidgetSelected Callback invoked when a widget type is selected.
 * @param modifier The modifier to apply to this row.
 */
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

    var showDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(Res.string.add_widget), style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            primaryWidgets.forEach { widget ->
                IconButton(onClick = { onWidgetSelected(widget) }) {
                    Icon(getWidgetIcon(widget), contentDescription = getWidgetNameString(widget))
                }
            }

            Box {
                IconButton(onClick = { showDropdown = true }) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = stringResource(Res.string.cd_more_widgets))
                }
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                ) {
                    secondaryWidgets.forEach { widget ->
                        DropdownMenuItem(
                            text = { Text(getWidgetNameString(widget)) },
                            leadingIcon = { Icon(getWidgetIcon(widget), contentDescription = null) },
                            onClick = {
                                onWidgetSelected(widget)
                                showDropdown = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen that allows users to build a dynamic FHIR Questionnaire.
 *
 * @param viewModel The view model managing the builder state.
 * @param onBack Callback when the back button is pressed.
 * @param onSaved Callback when the questionnaire is successfully saved, passing the new ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireBuilderScreen(
    viewModel: QuestionnaireBuilderViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.build_questionnaire)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePreviewMode() }) {
                        Icon(Icons.Default.Preview, contentDescription = stringResource(Res.string.cd_preview))
                    }
                    IconButton(onClick = {
                        val id = viewModel.saveQuestionnaire()
                        onSaved(id)
                    }) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(Res.string.cd_save))
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
            if (state.isPreviewMode) {
                Text(stringResource(Res.string.preview_mode), style = MaterialTheme.typography.titleLarge)
                LazyColumn {
                    items(state.items) { item ->
                        Text("${item.label} (${item.widgetType.name})", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            } else {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text(stringResource(Res.string.questionnaire_title)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )

                WidgetSelectionRow(
                    onWidgetSelected = { viewModel.addItem(it) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.items) { item ->
                        BuilderItemRow(
                            item = item,
                            onUpdate = { newLabel, newOptions -> viewModel.updateItem(item.linkId, newLabel, newOptions) },
                            onDelete = { viewModel.removeItem(item.linkId) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable for displaying a single builder item row.
 *
 * @param item The builder item to display.
 * @param onUpdate Callback to update the item's label and options.
 * @param onDelete Callback to delete the item.
 */
@Composable
private fun BuilderItemRow(
    item: BuilderItem,
    onUpdate: (String, List<String>) -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.type_format, item.widgetType.name), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = item.label,
                    onValueChange = { onUpdate(it, item.options) },
                    label = { Text(stringResource(Res.string.label)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                if (item.widgetType == WidgetType.SINGLE_SELECT || item.widgetType == WidgetType.MULTI_SELECT) {
                    OutlinedTextField(
                        value = item.options.joinToString(", "),
                        onValueChange = { text ->
                            onUpdate(item.label, text.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                        },
                        label = { Text(stringResource(Res.string.options_comma_separated)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.cd_delete_item))
            }
        }
    }
}
