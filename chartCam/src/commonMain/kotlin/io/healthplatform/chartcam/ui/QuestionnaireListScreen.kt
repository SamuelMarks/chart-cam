/**
 * @file QuestionnaireListScreen.kt
 * Contains declarations for QuestionnaireListScreen.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.cd_back
import chartcam.chartcam.generated.resources.cd_delete
import chartcam.chartcam.generated.resources.cd_duplicate
import chartcam.chartcam.generated.resources.copy_to_clipboard
import chartcam.chartcam.generated.resources.create
import chartcam.chartcam.generated.resources.create_questionnaire
import chartcam.chartcam.generated.resources.file_import_coming_soon
import chartcam.chartcam.generated.resources.id_format
import chartcam.chartcam.generated.resources.import_action
import chartcam.chartcam.generated.resources.import_confirmation
import chartcam.chartcam.generated.resources.import_error_format
import chartcam.chartcam.generated.resources.import_questionnaire
import chartcam.chartcam.generated.resources.invalid_fhir_format
import chartcam.chartcam.generated.resources.labels_comma
import chartcam.chartcam.generated.resources.number_of_items_format
import chartcam.chartcam.generated.resources.number_of_photos
import chartcam.chartcam.generated.resources.paste_from_clipboard
import chartcam.chartcam.generated.resources.qr_code_coming_soon
import chartcam.chartcam.generated.resources.questionnaires
import chartcam.chartcam.generated.resources.share_questionnaire
import chartcam.chartcam.generated.resources.share_text_json
import chartcam.chartcam.generated.resources.title
import chartcam.chartcam.generated.resources.title_format
import chartcam.chartcam.generated.resources.unknown
import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.repository.QuestionnaireSharingService
import io.healthplatform.chartcam.ui.components.tabFocusNext
import io.healthplatform.chartcam.utils.createShareService
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Screen displaying the list of available questionnaires.
 * Allows viewing existing questionnaires and creating new ones.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param questionnaireRepository The repository for fetching and creating questionnaires.
 * @param onBack Callback invoked when the back button is pressed.
 * @param onNavigateToBuilder Callback invoked to navigate to the questionnaire builder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireListScreen(
    questionnaireRepository: QuestionnaireRepository,
    onBack: () -> Unit,
    onNavigateToBuilder: (String?) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var questionnaires by remember { mutableStateOf(questionnaireRepository.getAvailableQuestionnaires()) }
    var showCreateDialog by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newPhotosCount by remember { mutableStateOf("") }
    var newLabels by remember { mutableStateOf("") }

    val shareService = remember { createShareService() }
    val questionnaireSharingService = remember { QuestionnaireSharingService() }
    val clipboard = androidx.compose.ui.platform.LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    var selectedQuestionnaireForShare by remember { mutableStateOf<Questionnaire?>(null) }
    var selectedQuestionnaireForView by remember { mutableStateOf<Questionnaire?>(null) }
    var showImportOptions by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var previewQuestionnaire by remember { mutableStateOf<Questionnaire?>(null) }

    val bottomSheetState = rememberModalBottomSheetState()
    val importBottomSheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.questionnaires),
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
                    IconButton(onClick = { showImportOptions = true }) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = stringResource(Res.string.import_questionnaire),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToBuilder(null) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.create_questionnaire))
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (importError != null) {
                Text(
                    text = stringResource(Res.string.import_error_format, importError ?: ""),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(questionnaires) { q ->
                    val titleText = q.title?.value ?: q.id ?: stringResource(Res.string.unknown)
                    ListItem(
                        modifier =
                            Modifier.minimumInteractiveComponentSize().clickable(role = Role.Button) {
                                selectedQuestionnaireForView =
                                    q
                            },
                        headlineContent = { Text(titleText, style = MaterialTheme.typography.titleMedium) },
                        supportingContent = {
                            Text(
                                stringResource(Res.string.id_format, q.id ?: stringResource(Res.string.unknown)),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                selectedQuestionnaireForShare = q
                            }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = stringResource(Res.string.share_questionnaire),
                                )
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    selectedQuestionnaireForView?.let { q ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedQuestionnaireForView = null },
            properties =
                androidx.compose.ui.window
                    .DialogProperties(usePlatformDefaultWidth = false),
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Text(
                                q.title?.value ?: q.id ?: stringResource(Res.string.unknown),
                                modifier = Modifier.semantics { heading() },
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedQuestionnaireForView = null }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(Res.string.cancel),
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                q.id?.let { id ->
                                    selectedQuestionnaireForView = null
                                    onNavigateToBuilder(id)
                                }
                            }) {
                                Icon(
                                    Icons.Default.FileCopy,
                                    contentDescription = stringResource(Res.string.cd_duplicate),
                                )
                            }
                            IconButton(onClick = {
                                q.id?.let { id ->
                                    questionnaireRepository.deleteQuestionnaire(id)
                                    questionnaires = questionnaireRepository.getAvailableQuestionnaires()
                                }
                                selectedQuestionnaireForView = null
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.cd_delete))
                            }
                        },
                    )
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                    ) {
                        io.healthplatform.chartcam.sdc.SdcQuestionnaireForm(
                            questionnaire = q,
                            answers = emptyMap(),
                            config =
                                io.healthplatform.chartcam.sdc
                                    .SdcFormConfig(readOnly = true),
                            onFormUpdated = { _, _ -> },
                        )
                    }
                }
            }
        }
    }

    if (showImportOptions) {
        ModalBottomSheet(
            onDismissRequest = { showImportOptions = false },
            sheetState = importBottomSheetState,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.import_questionnaire),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                val invalidFormatStr = stringResource(Res.string.invalid_fhir_format)
                Button(
                    onClick = {
                        coroutineScope
                            .launch {
                                try {
                                    val text = clipboard.getPlainText() ?: ""
                                    if (text.isNotBlank()) {
                                        previewQuestionnaire =
                                            questionnaireSharingService.deserializeQuestionnaire(text)
                                        importError = null
                                    } else {
                                        importError = "Empty"
                                    }
                                } catch (e: Exception) {
                                    println(e.message)

                                    importError = invalidFormatStr
                                }
                                importBottomSheetState.hide()
                            }.invokeOnCompletion {
                                if (!importBottomSheetState.isVisible) {
                                    showImportOptions = false
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    Text(stringResource(Res.string.paste_from_clipboard))
                }

                // Placeholder for File Import and QR Code Scanner
                Text(
                    text = stringResource(Res.string.file_import_coming_soon),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
            }
        }
    }

    previewQuestionnaire?.let { q ->
        AlertDialog(
            onDismissRequest = { previewQuestionnaire = null },
            title = {
                Text(
                    stringResource(Res.string.import_questionnaire),
                    modifier = Modifier.semantics { heading() },
                )
            },
            text = {
                Column {
                    val qTitle = q.title?.value ?: q.id ?: "Unknown"
                    Text(stringResource(Res.string.title_format, qTitle))
                    val sizeStr = q.item.size.toString()
                    Text(stringResource(Res.string.number_of_items_format, sizeStr))
                    Text(
                        stringResource(Res.string.import_confirmation),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    questionnaireRepository.saveQuestionnaire(q)
                    questionnaires = questionnaireRepository.getAvailableQuestionnaires()
                    previewQuestionnaire = null
                }) {
                    Text(stringResource(Res.string.import_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { previewQuestionnaire = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    selectedQuestionnaireForShare?.let { q ->
        ModalBottomSheet(
            onDismissRequest = { selectedQuestionnaireForShare = null },
            sheetState = bottomSheetState,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.share_questionnaire),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Button(
                    onClick = {
                        coroutineScope
                            .launch {
                                try {
                                    val json = questionnaireSharingService.serializeQuestionnaire(q)
                                    clipboard.setPlainText(json)
                                } catch (e: Exception) {
                                    println(e.message)

                                    // Handle error
                                }
                                bottomSheetState.hide()
                            }.invokeOnCompletion {
                                if (!bottomSheetState.isVisible) {
                                    selectedQuestionnaireForShare = null
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    Text(stringResource(Res.string.copy_to_clipboard))
                }

                Button(
                    onClick = {
                        try {
                            val json = questionnaireSharingService.serializeQuestionnaire(q)
                            shareService.shareText(json)
                        } catch (e: Exception) {
                            println(e.message)

                            // Handle error
                        }
                        coroutineScope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                            if (!bottomSheetState.isVisible) {
                                selectedQuestionnaireForShare = null
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    Text(stringResource(Res.string.share_text_json))
                }

                // Placeholder for QR Code and direct file export
                Text(
                    text = stringResource(Res.string.qr_code_coming_soon),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    stringResource(Res.string.create_questionnaire),
                    modifier = Modifier.semantics { heading() },
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(stringResource(Res.string.title)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).tabFocusNext(focusManager),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    )
                    OutlinedTextField(
                        value = newPhotosCount,
                        onValueChange = { newPhotosCount = it },
                        label = { Text(stringResource(Res.string.number_of_photos)) },
                        keyboardOptions =
                            KeyboardOptions(keyboardType = KeyboardType.Number)
                                .copy(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).tabFocusNext(focusManager),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    )
                    OutlinedTextField(
                        value = newLabels,
                        onValueChange = { newLabels = it },
                        label = { Text(stringResource(Res.string.labels_comma)) },
                        modifier = Modifier.fillMaxWidth().tabFocusNext(focusManager),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val count = newPhotosCount.toIntOrNull() ?: 0
                    questionnaireRepository.createQuestionnaire(
                        title = newTitle,
                        photos = count,
                        labels = newLabels,
                    )
                    questionnaires = questionnaireRepository.getAvailableQuestionnaires()
                    showCreateDialog = false
                    newTitle = ""
                    newPhotosCount = ""
                    newLabels = ""
                }) {
                    Text(stringResource(Res.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newTitle = ""
                    newPhotosCount = ""
                    newLabels = ""
                }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}
