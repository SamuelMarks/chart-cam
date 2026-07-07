package io.healthplatform.chartcam.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.repository.QuestionnaireSharingService
import io.healthplatform.chartcam.utils.createShareService
import kotlinx.coroutines.launch

/**
 * Screen displaying the list of available questionnaires.
 * Allows viewing existing questionnaires and creating new ones.
 *
 * @param questionnaireRepository The repository for fetching and creating questionnaires.
 * @param onBack Callback invoked when the back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireListScreen(
    questionnaireRepository: QuestionnaireRepository,
    onBack: () -> Unit,
    onNavigateToBuilder: () -> Unit = {},
) {
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
    var showImportOptions by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var previewQuestionnaire by remember { mutableStateOf<Questionnaire?>(null) }

    val bottomSheetState = rememberModalBottomSheetState()
    val importBottomSheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Questionnaires") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showImportOptions = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Import Questionnaire")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToBuilder) {
                Icon(Icons.Default.Add, contentDescription = "Create Questionnaire")
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
                    text = "Import Error: $importError",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(questionnaires) { q ->
                    val titleText = q.title?.value ?: q.id ?: "Unknown"
                    ListItem(
                        headlineContent = { Text(titleText, style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text(q.id ?: "", style = MaterialTheme.typography.bodyMedium) },
                        trailingContent = {
                            IconButton(onClick = {
                                selectedQuestionnaireForShare = q
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                        },
                    )
                    HorizontalDivider()
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
                    text = "Import Questionnaire",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Button(
                    onClick = {
                        coroutineScope
                            .launch {
                                try {
                                    val text = clipboard.getPlainText() ?: ""
                                    if (text.isNotBlank()) {
                                        previewQuestionnaire = questionnaireSharingService.deserializeQuestionnaire(text)
                                        importError = null
                                    } else {
                                        importError = "Clipboard is empty"
                                    }
                                } catch (e: Exception) {
                                    importError = e.message
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
                    Text("Paste from Clipboard")
                }

                // Placeholder for File Import and QR Code Scanner
                Text(
                    text = "File Import and QR Code Scanner coming soon",
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
            title = { Text("Import Questionnaire") },
            text = {
                Column {
                    Text("Title: ${q.title?.value ?: q.id ?: "Unknown"}")
                    Text("Number of items: ${q.item.size}")
                    Text("Are you sure you want to import this questionnaire?")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    questionnaireRepository.saveQuestionnaire(q)
                    questionnaires = questionnaireRepository.getAvailableQuestionnaires()
                    previewQuestionnaire = null
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewQuestionnaire = null }) {
                    Text("Cancel")
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
                    text = "Share Questionnaire",
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
                    Text("Copy to Clipboard")
                }

                Button(
                    onClick = {
                        try {
                            val json = questionnaireSharingService.serializeQuestionnaire(q)
                            shareService.shareText(json)
                        } catch (e: Exception) {
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
                    Text("Share text/JSON")
                }

                // Placeholder for QR Code and direct file export
                Text(
                    text = "QR Code and File Export coming soon",
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
            title = { Text("Create Questionnaire") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = newPhotosCount,
                        onValueChange = { newPhotosCount = it },
                        label = { Text("Number of photos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = newLabels,
                        onValueChange = { newLabels = it },
                        label = { Text("Labels (comma-separated)") },
                        modifier = Modifier.fillMaxWidth(),
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
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newTitle = ""
                    newPhotosCount = ""
                    newLabels = ""
                }) {
                    Text("Cancel")
                }
            },
        )
    }
}
