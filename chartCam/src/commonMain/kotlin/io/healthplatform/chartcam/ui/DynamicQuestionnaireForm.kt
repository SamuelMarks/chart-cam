package io.healthplatform.chartcam.ui

import org.jetbrains.compose.resources.stringResource
import chartcam.chartcam.generated.resources.*


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.fhir.model.r4.Questionnaire

/**
 * Dynamically renders a Questionnaire based on the resource items.
 * Supports String, Boolean, and Choice. Attachment is rendered externally.
 * Follows SDC logic for conditional visibility (enableWhen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicQuestionnaireForm(
    questionnaire: Questionnaire,
    answers: Map<String, Any>,
    onAnswerChanged: (String, Any?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column {
        questionnaire.item.forEach { item ->
            val linkId = item.linkId.value ?: return@forEach
            val itemDesc = stringResource(Res.string.cd_item, linkId)
            val type = item.type.value ?: return@forEach
            
            // SDC logic: enableWhen
            if (isItemHidden(item)) return@forEach
            val isEnabled = isItemEnabled(item, answers)
            
            if (isEnabled) {
                when (type) {
                    Questionnaire.QuestionnaireItemType.String -> {
                        val text = answers[linkId] as? String ?: ""
                        OutlinedTextField(
                            value = text,
                            onValueChange = { onAnswerChanged(linkId, it) },
                            label = { Text(item.text?.value ?: linkId) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics { contentDescription = itemDesc }.onKeyEvent {
                                if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                    focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                    true
                                } else false
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                        )
                    }
                    Questionnaire.QuestionnaireItemType.Boolean -> {
                        val checked = answers[linkId] as? Boolean ?: false
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onAnswerChanged(linkId, it) },
                                modifier = Modifier.semantics { contentDescription = itemDesc }
                            )
                            Text(text = item.text?.value ?: linkId, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                                        Questionnaire.QuestionnaireItemType.Choice -> {
                        val selectedOption = answers[linkId] as? String ?: ""
                        var expanded by remember { mutableStateOf(false) }
                        val options = item.answerOption.mapNotNull { it.value.asString()?.value?.value }

                        val itemControl = item.extension.firstOrNull { it.url == "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl" }
                            ?.value?.asCodeableConcept()?.value?.coding?.firstOrNull()?.code?.value
                        
                        if (itemControl == "radio-button") {
                            Column(modifier = Modifier.padding(vertical = 8.dp).semantics { contentDescription = itemDesc }) {
                                Text(item.text?.value ?: linkId, modifier = Modifier.padding(bottom = 4.dp))
                                options.forEach { option ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.material3.RadioButton(
                                            selected = selectedOption == option,
                                            onClick = { onAnswerChanged(linkId, option) }
                                        )
                                        Text(text = option, modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics { contentDescription = itemDesc }
                            ) {
                                OutlinedTextField(
                                    value = selectedOption.ifEmpty { stringResource(Res.string.select_an_option) },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(item.text?.value ?: linkId) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth().onKeyEvent {
                                        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                            focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                            true
                                        } else false
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    options.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                onAnswerChanged(linkId, option)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Questionnaire.QuestionnaireItemType.Attachment -> {
                        // Handled separately by the camera button and photo grid
                    }
                    else -> {
                        // Not implemented
                    }
                }
            }
        }
    }
}

/**
 * Evaluates enableWhen conditions for a given item against the current answers.
 */
fun isItemEnabled(item: Questionnaire.Item, answers: Map<String, Any>): Boolean {
    if (item.enableWhen.isEmpty()) return true
    
    val behavior = item.enableBehavior?.value ?: Questionnaire.EnableWhenBehavior.Any
    
    val conditions = item.enableWhen.map { ew ->
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

/**
 * Evaluates the SDC hidden extension.
 */
fun isItemHidden(item: Questionnaire.Item): Boolean {
    val hiddenExt = item.extension.firstOrNull { it.url == "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden" }
    return hiddenExt?.value?.asBoolean()?.value?.value == true
}