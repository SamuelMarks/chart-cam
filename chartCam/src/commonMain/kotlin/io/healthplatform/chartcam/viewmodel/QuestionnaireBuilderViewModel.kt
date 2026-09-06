/**
 * @file QuestionnaireBuilderViewModel.kt
 * Contains declarations for QuestionnaireBuilderViewModel.kt.
 */
package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.fhir.getItemControl
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Defines the state of the Questionnaire Builder.
 *
 * @param title The title of the questionnaire.
 * @param items The list of configured items for the questionnaire.
 * @param isPreviewMode Whether the builder is currently in preview mode.
 * @param isDuplicateNameError Whether there is an error due to a duplicate item name.
 */
data class QuestionnaireBuilderState(
    val title: kotlin.String = "",
    val items: List<BuilderItem> = emptyList(),
    val isPreviewMode: kotlin.Boolean = false,
    val isDuplicateNameError: kotlin.Boolean = false,
)

/**
 * Represents an item being built in the builder before converting to FHIR.
 *
 * @param linkId The unique ID for the item.
 * @param label The display text (instruction) for the item.
 * @param widgetType The type of Material 3 widget to render.
 * @param options Options for dropdowns, if applicable.
 * @param isError Whether the item has a validation error (e.g., empty label, missing options).
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
    /** PHOTO_CAMERA */
    PHOTO_CAMERA,

    /** VIDEO_CAMERA */
    VIDEO_CAMERA,

    /** SWITCH */
    SWITCH,

    /** CHECKBOX */
    CHECKBOX,

    /** SINGLE_SELECT */
    SINGLE_SELECT,

    /** MULTI_SELECT */
    MULTI_SELECT,

    /** SINGLE_LINE_TEXT */
    SINGLE_LINE_TEXT,

    /** MULTI_LINE_TEXT */
    MULTI_LINE_TEXT,

    /** DATE */
    DATE,

    /** DATE */
    DATETIME,

    /** NUMERIC */
    NUMERIC,

    /** RANGE */
    RANGE,
}

/**
 * ViewModel for managing the state of the Questionnaire Builder.
 * This ViewModel directly consumes and emits native FHIR R4 `Resource` models
 * (e.g., `Questionnaire`) without relying on intermediary DTOs.
 *
 * @param repository The repository to save the resulting FHIR Questionnaire.
 * @param duplicateFromId Optional ID of a questionnaire to duplicate from.
 * @param copyTitleResolver Function to generate duplicate questionnaire title.
 * @param defaultItemLabelResolver Function to provide fallback item label when missing.
 * @param widgetItemLabelResolver Function to generate default label for a newly added widget.
 * @param unknownTitleResolver Function to provide fallback title for unknown questionnaire.
 */
class QuestionnaireBuilderViewModel(
    private val repository: QuestionnaireRepository,
    private val duplicateFromId: kotlin.String? = null,
    private val copyTitleResolver: (kotlin.String) -> kotlin.String = { "$it (Copy)" },
    private val defaultItemLabelResolver: () -> kotlin.String = { "New Item" },
    private val widgetItemLabelResolver: (WidgetType) -> kotlin.String = { "New ${it.name} Item" },
    private val unknownTitleResolver: () -> kotlin.String = { "Unknown" },
) : ViewModel() {
    private val _state = MutableStateFlow(QuestionnaireBuilderState())
    private var nextItemId = 1

    /** The observable state of the builder. */
    val state: StateFlow<QuestionnaireBuilderState> = _state.asStateFlow()

    init {
        if (duplicateFromId != null) {
            repository.getQuestionnaire(duplicateFromId)?.let { source ->
                val sourceTitle = source.title?.value ?: unknownTitleResolver()
                _state.update {
                    it.copy(
                        title = copyTitleResolver(sourceTitle),
                        items =
                            source.item.map { fhirItem ->
                                val widgetType =
                                    when (fhirItem.getItemControl()) {
                                        "photo" -> WidgetType.PHOTO_CAMERA
                                        "video" -> WidgetType.VIDEO_CAMERA
                                        "switch" -> WidgetType.SWITCH
                                        "slider" -> WidgetType.RANGE
                                        "check-box" ->
                                            if (fhirItem.repeats?.value ==
                                                true
                                            ) {
                                                WidgetType.MULTI_SELECT
                                            } else {
                                                WidgetType.SINGLE_SELECT
                                            }
                                        else ->
                                            when (fhirItem.type.value) {
                                                Questionnaire.QuestionnaireItemType.Attachment ->
                                                    WidgetType.PHOTO_CAMERA
                                                Questionnaire.QuestionnaireItemType.Boolean ->
                                                    WidgetType.SWITCH
                                                Questionnaire.QuestionnaireItemType.Choice ->
                                                    if (fhirItem.repeats?.value ==
                                                        true
                                                    ) {
                                                        WidgetType.MULTI_SELECT
                                                    } else {
                                                        WidgetType.SINGLE_SELECT
                                                    }
                                                Questionnaire.QuestionnaireItemType.String ->
                                                    WidgetType.SINGLE_LINE_TEXT
                                                Questionnaire.QuestionnaireItemType.Text ->
                                                    WidgetType.MULTI_LINE_TEXT
                                                Questionnaire.QuestionnaireItemType.Date ->
                                                    WidgetType.DATE
                                                Questionnaire.QuestionnaireItemType.DateTime ->
                                                    WidgetType.DATETIME
                                                Questionnaire.QuestionnaireItemType.Decimal ->
                                                    WidgetType.NUMERIC
                                                Questionnaire.QuestionnaireItemType.Integer ->
                                                    WidgetType.RANGE
                                                else ->
                                                    WidgetType.SINGLE_LINE_TEXT
                                            }
                                    }

                                val options: List<kotlin.String> =
                                    fhirItem.answerOption.mapNotNull { opt ->
                                        val codingValue = opt.value as? Questionnaire.Item.AnswerOption.Value.Coding
                                        codingValue?.value?.display?.value
                                    }

                                BuilderItem(
                                    linkId = fhirItem.linkId.value ?: "item_${nextItemId++}",
                                    label = fhirItem.text?.value ?: defaultItemLabelResolver(),
                                    widgetType = widgetType,
                                    options = options,
                                    isError = false,
                                )
                            },
                    )
                }
                nextItemId = (
                    _state.value.items.maxOfOrNull {
                        it.linkId.removePrefix("item_").toIntOrNull() ?: 0
                    } ?: 0
                ) + 1
            }
        }
    }

    /**
     * Updates the title of the questionnaire.
     *
     * @param newTitle The new title.
     * @return Unit
     */
    fun updateTitle(newTitle: kotlin.String) {
        _state.update { it.copy(title = newTitle, isDuplicateNameError = false) }
    }

    /**
     * Adds a new item to the builder.
     *
     * @param widgetType The type of widget to add.
     * @param label Optional custom label for the item. Defaults to localized widget item name.
     */
    fun addItem(
        widgetType: WidgetType,
        label: kotlin.String? = null,
    ) {
        val currentItems = _state.value.items
        val newId = "item_${nextItemId++}"

        /** SINGLE_SELECT */
        val isError = (widgetType == WidgetType.SINGLE_SELECT || widgetType == WidgetType.MULTI_SELECT)
        val newItem =
            BuilderItem(
                linkId = newId,
                label = label ?: widgetItemLabelResolver(widgetType),
                widgetType = widgetType,
                options = emptyList(),
                isError = isError,
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
                                        (
                                            /** SINGLE_SELECT */
                                            item.widgetType == WidgetType.SINGLE_SELECT ||
                                                /** MULTI_SELECT */
                                                item.widgetType == WidgetType.MULTI_SELECT
                                        ) &&
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
     * This checks if the questionnaire title is present and valid,
     * and relies on [io.healthplatform.chartcam.validation.FhirValidator]
     * for strict FHIR structural validation (such as ensuring Choice items have options).
     * Additionally, it checks the UI builder state to ensure no items are currently flagged with an error.
     *
     * @return True if valid, false if there are validation errors.
     */
    fun validate(): kotlin.Boolean {
        if (_state.value.items.any { it.isError }) return false
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

        val fhirItems = currentState.items.map { mapBuilderItemToFhir(it) }

        return Questionnaire
            .Builder(Enumeration(value = PublicationStatus.Active))
            .apply {
                this.id = id
                this.title = String.Builder().apply { value = currentState.title }
                this.item.addAll(fhirItems)
            }.build()
    }

    /**
     * Maps item to fhir.
     * @param builderItem The builderItem.
     * @return The result.
     */
    private fun mapBuilderItemToFhir(builderItem: BuilderItem): Questionnaire.Item.Builder {
        val fhirType = getFhirItemType(builderItem.widgetType)
        val itemBuilder =
            Questionnaire.Item
                .Builder(
                    String.Builder().apply { value = builderItem.linkId },
                    Enumeration(value = fhirType),
                ).apply {
                    text = String.Builder().apply { value = builderItem.label }
                    required = Boolean.Builder().apply { value = false }
                }
        applyChoiceOptions(itemBuilder, builderItem, fhirType)
        applyItemControl(itemBuilder, builderItem)
        return itemBuilder
    }

    /**
     * Applies choice options.
     * @param itemBuilder The itemBuilder.
     * @param builderItem The builderItem.
     * @param fhirType The fhirType.
     */
    private fun applyChoiceOptions(
        itemBuilder: Questionnaire.Item.Builder,
        builderItem: BuilderItem,
        fhirType: Questionnaire.QuestionnaireItemType,
    ) {
        if (fhirType == Questionnaire.QuestionnaireItemType.Choice) {
            /** MULTI_SELECT */
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
    }

    /**
     * Applies item control.
     * @param itemBuilder The itemBuilder.
     * @param builderItem The builderItem.
     */
    private fun applyItemControl(
        itemBuilder: Questionnaire.Item.Builder,
        builderItem: BuilderItem,
    ) {
        val itemControlCode =
            when (builderItem.widgetType) {
                WidgetType.VIDEO_CAMERA -> "video"
                WidgetType.PHOTO_CAMERA -> "photo"
                WidgetType.SWITCH -> "switch"
                WidgetType.RANGE -> "slider"
                /** SINGLE_SELECT */
                WidgetType.SINGLE_SELECT, WidgetType.MULTI_SELECT -> "check-box"
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
    }

    /**
     * Gets fhir item type.
     * @param widgetType The widgetType.
     * @return The result.
     */
    private fun getFhirItemType(widgetType: WidgetType): Questionnaire.QuestionnaireItemType =
        when (widgetType) {
            /** PHOTO_CAMERA */
            WidgetType.PHOTO_CAMERA, WidgetType.VIDEO_CAMERA -> Questionnaire.QuestionnaireItemType.Attachment
            /** SWITCH */
            WidgetType.SWITCH, WidgetType.CHECKBOX -> Questionnaire.QuestionnaireItemType.Boolean
            /** SINGLE_SELECT */
            WidgetType.SINGLE_SELECT, WidgetType.MULTI_SELECT -> Questionnaire.QuestionnaireItemType.Choice
            WidgetType.SINGLE_LINE_TEXT -> Questionnaire.QuestionnaireItemType.String
            WidgetType.MULTI_LINE_TEXT -> Questionnaire.QuestionnaireItemType.Text
            WidgetType.DATE -> Questionnaire.QuestionnaireItemType.Date
            WidgetType.DATETIME -> Questionnaire.QuestionnaireItemType.DateTime
            WidgetType.NUMERIC -> Questionnaire.QuestionnaireItemType.Decimal
            WidgetType.RANGE -> Questionnaire.QuestionnaireItemType.Integer
        }

    /**
     * Saves the current builder state as a FHIR Questionnaire in the repository.
     * @return The ID of the newly created Questionnaire, or null if validation failed.
     */
    fun saveQuestionnaire(): kotlin.String? {
        var finalId: kotlin.String? = null

        if (validate()) {
            val questionnaire = buildQuestionnaire()
            val currentId = questionnaire.id

            // Ensure uniqueness
            val existing = currentId?.let { repository.getQuestionnaire(it) }

            if (existing != null) {
                _state.update { it.copy(isDuplicateNameError = true) }
            } else {
                repository.saveQuestionnaire(questionnaire)
                finalId = questionnaire.id ?: ""
            }
        }

        return finalId
    }
}
