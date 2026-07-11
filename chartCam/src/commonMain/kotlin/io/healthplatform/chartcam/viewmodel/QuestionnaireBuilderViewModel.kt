package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Defines the state of the Questionnaire Builder.
 *
 * @property title The title of the questionnaire.
 * @property items The list of configured items for the questionnaire.
 * @property isPreviewMode Whether the builder is currently in preview mode.
 */
data class QuestionnaireBuilderState(
    val title: kotlin.String = "",
    val items: List<BuilderItem> = emptyList(),
    val isPreviewMode: kotlin.Boolean = false,
)

/**
 * Represents an item being built in the builder before converting to FHIR.
 *
 * @property linkId The unique ID for the item.
 * @property label The display text (instruction) for the item.
 * @property widgetType The type of Material 3 widget to render.
 * @property options Options for dropdowns, if applicable.
 * @property isError Whether the item has a validation error (e.g., empty label, missing options).
 */
data class BuilderItem(
    val linkId: kotlin.String,
    val label: kotlin.String,
    val widgetType: WidgetType,
    val options: List<kotlin.String> = emptyList(),
    val isError: kotlin.Boolean = false,
)

/**
 * Enum defining the supported widget types in the builder.
 */
enum class WidgetType {
    PHOTO_CAMERA,
    VIDEO_CAMERA,
    SWITCH,
    CHECKBOX,
    SINGLE_SELECT,
    MULTI_SELECT,
    SINGLE_LINE_TEXT,
    MULTI_LINE_TEXT,
    DATE,
    DATETIME,
    NUMERIC,
    RANGE,
}

/**
 * ViewModel for managing the state of the Questionnaire Builder.
 * This ViewModel directly consumes and emits native FHIR R4 `Resource` models (e.g., `Questionnaire`) without relying on intermediary DTOs.
 *
 * @param repository The repository to save the resulting FHIR Questionnaire.
 */
class QuestionnaireBuilderViewModel(
    private val repository: QuestionnaireRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(QuestionnaireBuilderState())
    private var nextItemId = 1

    /** The observable state of the builder. */
    val state: StateFlow<QuestionnaireBuilderState> = _state.asStateFlow()

    /**
     * Updates the title of the questionnaire.
     *
     * @param newTitle The new title.
     */
    fun updateTitle(newTitle: kotlin.String) {
        _state.update { it.copy(title = newTitle) }
    }

    /**
     * Adds a new item to the builder.
     *
     * @param widgetType The type of widget to add.
     */
    fun addItem(widgetType: WidgetType) {
        val currentItems = _state.value.items
        val newId = "item_${nextItemId++}"
        val newItem =
            BuilderItem(
                linkId = newId,
                label = "New ${widgetType.name} Item",
                widgetType = widgetType,
                options = emptyList(),
            )
        _state.update { it.copy(items = currentItems + newItem) }
    }

    /**
     * Updates an existing item in the builder.
     *
     * @param linkId The ID of the item to update.
     * @param newLabel The new label for the item.
     * @param newOptions The new options for the item (if applicable).
     */
    fun updateItem(
        linkId: kotlin.String,
        newLabel: kotlin.String,
        newOptions: List<kotlin.String>,
    ) {
        _state.update { currentState ->
            currentState.copy(
                items =
                    currentState.items.map { item ->
                        if (item.linkId == linkId) {
                            val isError =
                                newLabel.isBlank() ||
                                    (
                                        (item.widgetType == WidgetType.SINGLE_SELECT || item.widgetType == WidgetType.MULTI_SELECT) &&
                                            newOptions.isEmpty()
                                    )
                            item.copy(label = newLabel, options = newOptions, isError = isError)
                        } else {
                            item
                        }
                    },
            )
        }
    }

    /**
     * Validates the current builder state.
     * @return True if valid, false if there are validation errors.
     */
    fun validate(): kotlin.Boolean {
        val questionnaire = buildQuestionnaire()
        return io.healthplatform.chartcam.validation.FhirValidator
            .validate(questionnaire)
    }

    /**
     * Removes an item from the builder.
     *
     * @param linkId The ID of the item to remove.
     */
    fun removeItem(linkId: kotlin.String) {
        _state.update { currentState ->
            currentState.copy(items = currentState.items.filter { it.linkId != linkId })
        }
    }

    /**
     * Moves an item up in the list.
     *
     * @param linkId The ID of the item to move up.
     */
    fun moveItemUp(linkId: kotlin.String) {
        _state.update { currentState ->
            val items = currentState.items.toMutableList()
            val index = items.indexOfFirst { it.linkId == linkId }
            if (index > 0) {
                val item = items.removeAt(index)
                items.add(index - 1, item)
            }
            currentState.copy(items = items.toList())
        }
    }

    /**
     * Moves an item down in the list.
     *
     * @param linkId The ID of the item to move down.
     */
    fun moveItemDown(linkId: kotlin.String) {
        _state.update { currentState ->
            val items = currentState.items.toMutableList()
            val index = items.indexOfFirst { it.linkId == linkId }
            if (index != -1 && index < items.size - 1) {
                val item = items.removeAt(index)
                items.add(index + 1, item)
            }
            currentState.copy(items = items.toList())
        }
    }

    /**
     * Toggles the preview mode of the builder.
     */
    fun togglePreviewMode() {
        _state.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    /**
     * Builds a FHIR Questionnaire resource from the current state.
     * @return The built Questionnaire.
     */
    fun buildQuestionnaire(): Questionnaire {
        val currentState = _state.value
        val id = "custom-${currentState.title.lowercase().replace(Regex("[^a-z0-9]+"), "-")}"

        val fhirItems =
            currentState.items.map { builderItem ->
                val fhirType =
                    when (builderItem.widgetType) {
                        WidgetType.PHOTO_CAMERA, WidgetType.VIDEO_CAMERA -> Questionnaire.QuestionnaireItemType.Attachment
                        WidgetType.SWITCH, WidgetType.CHECKBOX -> Questionnaire.QuestionnaireItemType.Boolean
                        WidgetType.SINGLE_SELECT, WidgetType.MULTI_SELECT -> Questionnaire.QuestionnaireItemType.Choice
                        WidgetType.SINGLE_LINE_TEXT -> Questionnaire.QuestionnaireItemType.String
                        WidgetType.MULTI_LINE_TEXT -> Questionnaire.QuestionnaireItemType.Text
                        WidgetType.DATE -> Questionnaire.QuestionnaireItemType.Date
                        WidgetType.DATETIME -> Questionnaire.QuestionnaireItemType.DateTime
                        WidgetType.NUMERIC -> Questionnaire.QuestionnaireItemType.Decimal
                        WidgetType.RANGE -> Questionnaire.QuestionnaireItemType.Integer
                    }

                val itemBuilder =
                    Questionnaire.Item
                        .Builder(
                            String.Builder().apply { value = builderItem.linkId },
                            Enumeration(value = fhirType),
                        ).apply {
                            text = String.Builder().apply { value = builderItem.label }
                            required = Boolean.Builder().apply { value = false }
                        }

                // Map options for Choice types
                if (fhirType == Questionnaire.QuestionnaireItemType.Choice) {
                    if (builderItem.widgetType == WidgetType.MULTI_SELECT) {
                        itemBuilder.repeats = Boolean.Builder().apply { value = true }
                    }
                    builderItem.options.forEachIndexed { index, optionValue ->
                        itemBuilder.answerOption.add(
                            Questionnaire.Item.AnswerOption.Builder(
                                Questionnaire.Item.AnswerOption.Value.Coding(
                                    com.google.fhir.model.r4.Coding
                                        .Builder()
                                        .apply {
                                            system =
                                                com.google.fhir.model.r4.Uri
                                                    .Builder()
                                                    .apply { value = "http://chartcam.local/custom-options" }
                                            code =
                                                com.google.fhir.model.r4.Code
                                                    .Builder()
                                                    .apply { value = "opt-$index" }
                                            display = String.Builder().apply { value = optionValue }
                                        }.build(),
                                ),
                            ),
                        )
                    }
                }

                // Map extensions/itemControls to differentiate visually similar items
                // e.g. PHOTO vs VIDEO
                val itemControlCode =
                    when (builderItem.widgetType) {
                        WidgetType.VIDEO_CAMERA -> "video"
                        WidgetType.PHOTO_CAMERA -> "photo"
                        WidgetType.SWITCH -> "switch"
                        WidgetType.RANGE -> "slider"
                        else -> null
                    }

                if (itemControlCode != null) {
                    itemBuilder.extension.add(
                        com.google.fhir.model.r4.Extension
                            .Builder(
                                url = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                            ).apply {
                                value =
                                    com.google.fhir.model.r4.Extension.Value.CodeableConcept(
                                        com.google.fhir.model.r4.CodeableConcept
                                            .Builder()
                                            .apply {
                                                coding.add(
                                                    com.google.fhir.model.r4.Coding.Builder().apply {
                                                        code =
                                                            com.google.fhir.model.r4.Code
                                                                .Builder()
                                                                .apply { value = itemControlCode }
                                                    },
                                                )
                                            }.build(),
                                    )
                            },
                    )
                }

                itemBuilder
            }

        return Questionnaire
            .Builder(Enumeration(value = PublicationStatus.Active))
            .apply {
                this.id = id
                this.title = String.Builder().apply { value = currentState.title }
                this.item.addAll(fhirItems)
            }.build()
    }

    /**
     * Saves the current builder state as a FHIR Questionnaire in the repository.
     * @return The ID of the newly created Questionnaire, or null if validation failed.
     */
    fun saveQuestionnaire(): kotlin.String? {
        if (!validate()) return null
        val questionnaire = buildQuestionnaire()
        repository.saveQuestionnaire(questionnaire)
        return questionnaire.id ?: ""
    }
}
