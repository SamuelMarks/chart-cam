/**
 * @file EnableWhenEditorDialog.kt
 * Form Builder dialog for configuring FHIR SDC enableWhen conditional logic rules on form questions.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.cd_delete_condition_format
import chartcam.chartcam.generated.resources.confirm
import chartcam.chartcam.generated.resources.enable_when_add_condition
import chartcam.chartcam.generated.resources.enable_when_behavior_all
import chartcam.chartcam.generated.resources.enable_when_behavior_any
import chartcam.chartcam.generated.resources.enable_when_expected_answer
import chartcam.chartcam.generated.resources.enable_when_operator_equal
import chartcam.chartcam.generated.resources.enable_when_operator_exists
import chartcam.chartcam.generated.resources.enable_when_operator_greater_or_equal
import chartcam.chartcam.generated.resources.enable_when_operator_greater_than
import chartcam.chartcam.generated.resources.enable_when_operator_label
import chartcam.chartcam.generated.resources.enable_when_operator_less_or_equal
import chartcam.chartcam.generated.resources.enable_when_operator_less_than
import chartcam.chartcam.generated.resources.enable_when_operator_not_equal
import chartcam.chartcam.generated.resources.enable_when_target_question
import chartcam.chartcam.generated.resources.enable_when_title
import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.ui.theme.AppSpacing
import io.healthplatform.chartcam.viewmodel.BuilderEnableWhen
import io.healthplatform.chartcam.viewmodel.BuilderItem
import io.healthplatform.chartcam.viewmodel.WidgetType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Creates an updated [BuilderEnableWhen] with the new answer string.
 *
 * @param condition The current condition rule.
 * @param newAnswer The new answer text.
 * @return A new [BuilderEnableWhen] instance with the updated answer string.
 */
fun updateConditionAnswer(
    condition: BuilderEnableWhen,
    newAnswer: String,
): BuilderEnableWhen =
    BuilderEnableWhen(
        question = condition.question,
        operator = condition.operator,
        answerString = newAnswer,
        answerBoolean = condition.answerBoolean,
        answerDecimal = condition.answerDecimal,
        answerInteger = condition.answerInteger,
    )

/**
 * Resolves the localized string resource corresponding to the given [Questionnaire.QuestionnaireItemOperator].
 *
 * @param operator The questionnaire item operator.
 * @return The corresponding [StringResource].
 */
fun getOperatorStringResource(operator: Questionnaire.QuestionnaireItemOperator): StringResource =
    when (operator) {
        Questionnaire.QuestionnaireItemOperator.EqualTo -> Res.string.enable_when_operator_equal
        Questionnaire.QuestionnaireItemOperator.NotEqualTo -> Res.string.enable_when_operator_not_equal
        Questionnaire.QuestionnaireItemOperator.GreaterThan -> Res.string.enable_when_operator_greater_than
        Questionnaire.QuestionnaireItemOperator.LessThan -> Res.string.enable_when_operator_less_than
        Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo ->
            Res.string.enable_when_operator_greater_or_equal
        Questionnaire.QuestionnaireItemOperator.LessThanOrEqualTo ->
            Res.string.enable_when_operator_less_or_equal
        Questionnaire.QuestionnaireItemOperator.Exists -> Res.string.enable_when_operator_exists
    }

/**
 * Dialog for configuring enableWhen conditional display rules in the questionnaire form builder.
 *
 * @param currentItem The item whose conditional display is being configured.
 * @param candidateQuestions Prior candidate questions in the form that this question may depend upon.
 * @param onDismiss Request to dismiss dialog without saving.
 * @param onSave Callback passing the configured conditions and behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnableWhenEditorDialog(
    currentItem: BuilderItem,
    candidateQuestions: List<BuilderItem>,
    onDismiss: () -> Unit,
    onSave: (conditions: List<BuilderEnableWhen>, behavior: Questionnaire.EnableWhenBehavior) -> Unit,
) {
    var conditions by remember { mutableStateOf(currentItem.enableWhen) }
    var behavior by remember {
        mutableStateOf(currentItem.enableBehavior ?: Questionnaire.EnableWhenBehavior.All)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.enable_when_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.moderate),
            ) {
                // Behavior selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            if (behavior == Questionnaire.EnableWhenBehavior.All) {
                                stringResource(Res.string.enable_when_behavior_all)
                            } else {
                                stringResource(Res.string.enable_when_behavior_any)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            behavior =
                                if (behavior == Questionnaire.EnableWhenBehavior.All) {
                                    Questionnaire.EnableWhenBehavior.Any
                                } else {
                                    Questionnaire.EnableWhenBehavior.All
                                }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text(
                            text =
                                if (behavior == Questionnaire.EnableWhenBehavior.All) {
                                    stringResource(Res.string.enable_when_behavior_any)
                                } else {
                                    stringResource(Res.string.enable_when_behavior_all)
                                },
                        )
                    }
                }

                if (conditions.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.enable_when_add_condition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    conditions.forEachIndexed { index, cond ->
                        ConditionRow(
                            condition = cond,
                            candidateQuestions = candidateQuestions,
                            onUpdate = { updated ->
                                conditions = conditions.toMutableList().apply { set(index, updated) }
                            },
                            onDelete = {
                                conditions = conditions.toMutableList().apply { removeAt(index) }
                            },
                            onAnswerChange = { newAnswer ->
                                conditions =
                                    conditions.toMutableList().apply {
                                        set(index, updateConditionAnswer(cond, newAnswer))
                                    }
                            },
                        )
                    }
                }

                Button(
                    onClick = {
                        conditions =
                            conditions +
                            BuilderEnableWhen(
                                question = candidateQuestions.first().linkId,
                                operator = Questionnaire.QuestionnaireItemOperator.EqualTo,
                                answerString = "",
                                answerBoolean = null,
                                answerDecimal = null,
                                answerInteger = null,
                            )
                    },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    enabled = candidateQuestions.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(stringResource(Res.string.enable_when_add_condition))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(conditions, behavior) },
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

/**
 * Composable row representing a single enableWhen rule in the editor.
 *
 * @param condition The condition to display.
 * @param candidateQuestions Prior questions that can be selected as target.
 * @param onUpdate Callback with updated condition.
 * @param onDelete Callback requesting deletion of this condition.
 * @param onAnswerChange Callback with updated expected answer string.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConditionRow(
    condition: BuilderEnableWhen,
    candidateQuestions: List<BuilderItem>,
    onUpdate: (BuilderEnableWhen) -> Unit,
    onDelete: () -> Unit,
    onAnswerChange: (String) -> Unit,
) {
    var questionExpanded by remember { mutableStateOf(false) }
    var operatorExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        val matchedCandidate = candidateQuestions.firstOrNull { it.linkId == condition.question }
        val currentTargetLabel =
            if (matchedCandidate != null) {
                matchedCandidate.label
            } else {
                condition.question
            }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Target Question Dropdown
            ExposedDropdownMenuBox(
                expanded = questionExpanded,
                onExpandedChange = { questionExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = currentTargetLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.enable_when_target_question)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = questionExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = questionExpanded,
                    onDismissRequest = { questionExpanded = false },
                ) {
                    candidateQuestions.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.label) },
                            onClick = {
                                onUpdate(
                                    BuilderEnableWhen(
                                        question = candidate.linkId,
                                        operator = condition.operator,
                                        answerString = condition.answerString,
                                        answerBoolean = condition.answerBoolean,
                                        answerDecimal = condition.answerDecimal,
                                        answerInteger = condition.answerInteger,
                                    ),
                                )
                                questionExpanded = false
                            },
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.cd_delete_condition_format, currentTargetLabel),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Operator Dropdown
            ExposedDropdownMenuBox(
                expanded = operatorExpanded,
                onExpandedChange = { operatorExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                val currentOpRes = getOperatorStringResource(condition.operator)
                OutlinedTextField(
                    value = stringResource(currentOpRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.enable_when_operator_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = operatorExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = operatorExpanded,
                    onDismissRequest = { operatorExpanded = false },
                ) {
                    Questionnaire.QuestionnaireItemOperator.entries.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(stringResource(getOperatorStringResource(op))) },
                            onClick = {
                                onUpdate(
                                    BuilderEnableWhen(
                                        question = condition.question,
                                        operator = op,
                                        answerString = condition.answerString,
                                        answerBoolean = condition.answerBoolean,
                                        answerDecimal = condition.answerDecimal,
                                        answerInteger = condition.answerInteger,
                                    ),
                                )
                                operatorExpanded = false
                            },
                        )
                    }
                }
            }

            // Expected Answer
            if (condition.operator != Questionnaire.QuestionnaireItemOperator.Exists) {
                val targetItem = candidateQuestions.firstOrNull { it.linkId == condition.question }
                val keyboardType =
                    when (targetItem?.widgetType) {
                        WidgetType.NUMERIC, WidgetType.RANGE -> KeyboardType.Number
                        else -> KeyboardType.Text
                    }
                OutlinedTextField(
                    value = condition.answerString ?: "",
                    onValueChange = onAnswerChange,
                    label = { Text(stringResource(Res.string.enable_when_expected_answer)) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
