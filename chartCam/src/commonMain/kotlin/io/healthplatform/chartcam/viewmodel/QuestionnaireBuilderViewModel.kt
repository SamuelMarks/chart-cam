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
 */
data class BuilderItem(
    val linkId: kotlin.String,
    val label: kotlin.String,
    val widgetType: WidgetType,
    val options: List<kotlin.String> = emptyList(),
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
                options =
                    if (widgetType == WidgetType.SINGLE_SELECT ||
                        widgetType == WidgetType.MULTI_SELECT
                    ) {
                        listOf("Option 1", "Option 2")
                    } else {
                        emptyList()
                    },
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
                            item.copy(label = newLabel, options = newOptions)
                        } else {
                            item
                        }
                    },
            )
        }
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
     * Toggles the preview mode of the builder.
     */
    fun togglePreviewMode() {
        _state.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    /**
     * Saves the current builder state as a FHIR Questionnaire in the repository.
     * @return The ID of the newly created Questionnaire.
     */
    fun saveQuestionnaire(): kotlin.String {
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
                    builderItem.options.forEach { optionValue ->
                        itemBuilder.answerOption.add(
                            Questionnaire.Item.AnswerOption.Builder(
                                Questionnaire.Item.AnswerOption.Value.String(
                                    String.Builder().apply { value = optionValue }.build(),
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

        val questionnaire =
            Questionnaire
                .Builder(Enumeration(value = PublicationStatus.Active))
                .apply {
                    this.id = id
                    this.title = String.Builder().apply { value = currentState.title }
                    this.item.addAll(fhirItems)
                }.build()

        repository.saveQuestionnaire(questionnaire)
        return id
    }
}
