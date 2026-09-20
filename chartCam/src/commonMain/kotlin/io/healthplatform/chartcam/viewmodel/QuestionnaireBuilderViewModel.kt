/**
 * @file QuestionnaireBuilderViewModel.kt
 * Contains declarations for QuestionnaireBuilderViewModel.kt.
 */
package io.healthplatform.chartcam.viewmodel

import androidx.lifecycle.ViewModel
import dev.ohs.fhir.model.r4.Decimal
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Integer
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.fhir.getItemControl
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Defines the state of the Questionnaire Builder.
 *
 * @param title The title of the questionnaire.
 * @param items The list of configured items for the questionnaire.
 * @param isPreviewMode Whether the builder is currently in preview mode.
 * @param isDuplicateNameError Whether there is an error due to a duplicate item name.
 */
data class QuestionnaireBuilderState(
    val title: String = "",
    val items: List<BuilderItem> = emptyList(),
    val isPreviewMode: Boolean = false,
    val isDuplicateNameError: Boolean = false,
)

/**
 * Represents an enableWhen condition configured on an item in the questionnaire builder.
 *
 * @param question The linkId of the target question that this condition depends on.
 * @param operator The operator to compare with.
 * @param answerString The string representation of expected answer, or null.
 * @param answerBoolean The boolean expected answer, or null.
 * @param answerDecimal The decimal expected answer, or null.
 * @param answerInteger The integer expected answer, or null.
 */
data class BuilderEnableWhen(
    val question: kotlin.String,
    val operator: Questionnaire.QuestionnaireItemOperator = Questionnaire.QuestionnaireItemOperator.EqualTo,
    val answerString: kotlin.String? = null,
    val answerBoolean: kotlin.Boolean? = null,
    val answerDecimal: Double? = null,
    val answerInteger: Int? = null,
)

/**
 * Represents an item being built in the builder before converting to FHIR.
 *
 * @param linkId The unique ID for the item.
 * @param label The display text (instruction) for the item.
 * @param widgetType The type of Material 3 widget to render.
 * @param options Options for dropdowns, if applicable.
 * @param isError Whether the item has a validation error (e.g., empty label, missing options).
 * @param enableWhen The list of enableWhen condition rules configured for this item.
 * @param enableBehavior The behavior for combining multiple conditions (All or Any).
 * @param repeats Whether the item supports repeating responses in the questionnaire.
 */
data class BuilderItem(
    val linkId: kotlin.String,
    val label: kotlin.String,
    val widgetType: WidgetType,
    val options: List<kotlin.String> = emptyList(),
    val isError: kotlin.Boolean = false,
    val enableWhen: List<BuilderEnableWhen> = emptyList(),
    val enableBehavior: Questionnaire.EnableWhenBehavior? = null,
    val repeats: kotlin.Boolean = false,
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

    /** PAIN_SCALE */
    PAIN_SCALE,

    /** FITZPATRICK_PALETTE */
    FITZPATRICK_PALETTE,

    /** BODY_MAP */
    BODY_MAP,

    /** SEGMENTED_TILES */
    SEGMENTED_TILES,

    /** GROUP */
    GROUP,
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
 * @param defaultOptionsResolver Function to provide localized default options for specialized widgets.
 */
class QuestionnaireBuilderViewModel(
    private val repository: QuestionnaireRepository,
    private val duplicateFromId: kotlin.String? = null,
    private val copyTitleResolver: (kotlin.String) -> kotlin.String = { "$it (Copy)" },
    private val defaultItemLabelResolver: () -> kotlin.String = { "New Item" },
    private val widgetItemLabelResolver: (WidgetType) -> kotlin.String = { "New ${it.name} Item" },
    private val unknownTitleResolver: () -> kotlin.String = { "Unknown" },
    private val defaultOptionsResolver: (WidgetType) -> List<kotlin.String> = { type ->
        when (type) {
            WidgetType.FITZPATRICK_PALETTE ->
                listOf("Type I", "Type II", "Type III", "Type IV", "Type V", "Type VI")
            WidgetType.SEGMENTED_TILES ->
                listOf("Mild", "Moderate", "Severe")
            else -> emptyList()
        }
    },
) : ViewModel() {
    private val _state = MutableStateFlow(QuestionnaireBuilderState())
    private var nextItemId = 1

    /** The observable state of the builder. */
    val state: StateFlow<QuestionnaireBuilderState> = _state.asStateFlow()

    init {
        if (duplicateFromId != null) {
            val source = repository.getQuestionnaire(duplicateFromId)
            if (source != null) {
                val tObj = source.title
                val sourceTitle = if (tObj != null && tObj.value != null) tObj.value!! else unknownTitleResolver()
                _state.update {
                    it.copy(
                        title = copyTitleResolver(sourceTitle),
                        items = source.item.map { fhirItem -> parseFhirItemToBuilderItem(fhirItem) },
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
     * Resolves the [WidgetType] for an imported FHIR Questionnaire item based on itemControl or type.
     *
     * @param fhirItem The source FHIR item.
     * @return The corresponding [WidgetType].
     */
    private fun resolveWidgetTypeFromFhir(fhirItem: Questionnaire.Item): WidgetType {
        val rep = fhirItem.repeats
        val isRepeats = rep != null && rep.value == true
        return when (fhirItem.getItemControl()) {
            "photo" -> WidgetType.PHOTO_CAMERA
            "video" -> WidgetType.VIDEO_CAMERA
            "switch" -> WidgetType.SWITCH
            "slider" -> WidgetType.RANGE
            "pain-vas", "wong-baker" -> WidgetType.PAIN_SCALE
            "palette", "color-palette", "fitzpatrick" -> WidgetType.FITZPATRICK_PALETTE
            "body-map" -> WidgetType.BODY_MAP
            "segmented-control", "choice-cards" -> WidgetType.SEGMENTED_TILES
            "check-box" -> if (isRepeats) WidgetType.MULTI_SELECT else WidgetType.SINGLE_SELECT
            else -> {
                val typeVal = fhirItem.type.value
                val fhirType = if (typeVal != null) typeVal else Questionnaire.QuestionnaireItemType.String
                resolveWidgetTypeFallback(fhirType, isRepeats)
            }
        }
    }

    /**
     * Fallback resolution of [WidgetType] based on standard FHIR item types.
     *
     * @param type The FHIR item type.
     * @param isRepeats Whether the item repeats.
     * @return The corresponding [WidgetType].
     */
    private fun resolveWidgetTypeFallback(
        type: Questionnaire.QuestionnaireItemType,
        isRepeats: Boolean,
    ): WidgetType =
        when (type) {
            Questionnaire.QuestionnaireItemType.Attachment -> WidgetType.PHOTO_CAMERA
            Questionnaire.QuestionnaireItemType.Boolean -> WidgetType.SWITCH
            Questionnaire.QuestionnaireItemType.Choice ->
                if (isRepeats) WidgetType.MULTI_SELECT else WidgetType.SINGLE_SELECT
            Questionnaire.QuestionnaireItemType.String -> WidgetType.SINGLE_LINE_TEXT
            Questionnaire.QuestionnaireItemType.Text -> WidgetType.MULTI_LINE_TEXT
            Questionnaire.QuestionnaireItemType.Date -> WidgetType.DATE
            Questionnaire.QuestionnaireItemType.DateTime -> WidgetType.DATETIME
            Questionnaire.QuestionnaireItemType.Decimal -> WidgetType.NUMERIC
            Questionnaire.QuestionnaireItemType.Integer -> WidgetType.RANGE
            Questionnaire.QuestionnaireItemType.Group -> WidgetType.GROUP
            else -> WidgetType.SINGLE_LINE_TEXT
        }

    /**
     * Parses FHIR EnableWhen conditions into builder representations.
     *
     * @param enableWhen The list of FHIR enableWhen items.
     * @return The parsed [BuilderEnableWhen] rules.
     */
    private fun parseEnableWhenList(
        enableWhen: List<Questionnaire.Item.EnableWhen>,
    ): List<BuilderEnableWhen> =
        enableWhen.mapNotNull { ew ->
            val q = ew.question.value
            if (q == null) return@mapNotNull null
            val op = ew.operator.value ?: Questionnaire.QuestionnaireItemOperator.EqualTo
            val ans = ew.answer
            val strAns = ans.asString()
            val strVal = if (strAns != null) strAns.value.value else null
            val boolAns = ans.asBoolean()
            val boolVal = if (boolAns != null) boolAns.value.value else null
            val intAns = ans.asInteger()
            val intVal = if (intAns != null) intAns.value.value else null
            val decAns = ans.asDecimal()
            val decVal =
                if (decAns != null) {
                    val raw = decAns.value.value
                    if (raw != null) raw.toString().toDoubleOrNull() else null
                } else {
                    null
                }
            BuilderEnableWhen(
                question = q,
                operator = op,
                answerString = strVal,
                answerBoolean = boolVal,
                answerInteger = intVal,
                answerDecimal = decVal,
            )
        }

    /**
     * Parses a FHIR Questionnaire.Item into a UI BuilderItem.
     *
     * @param fhirItem The source FHIR questionnaire item.
     * @return The populated BuilderItem instance.
     */
    private fun parseFhirItemToBuilderItem(fhirItem: Questionnaire.Item): BuilderItem {
        val widgetType = resolveWidgetTypeFromFhir(fhirItem)
        val options: List<kotlin.String> =
            fhirItem.answerOption.mapNotNull { opt ->
                val codingValue = opt.value as? Questionnaire.Item.AnswerOption.Value.Coding
                val c = if (codingValue != null) codingValue.value else null
                val disp = if (c != null) c.display else null
                if (disp != null) disp.value else null
            }

        val builderEnableWhen = parseEnableWhenList(fhirItem.enableWhen)
        val lId = fhirItem.linkId.value
        val linkId = if (lId != null) lId else "item_${nextItemId++}"
        val txt = fhirItem.text
        val txtVal = if (txt != null) txt.value else null
        val label = if (txtVal != null) txtVal else defaultItemLabelResolver()
        val eb = fhirItem.enableBehavior
        val behavior = if (eb != null) eb.value else null
        val rep = fhirItem.repeats
        val isRepeats = rep != null && rep.value == true

        return BuilderItem(
            linkId = linkId,
            label = label,
            widgetType = widgetType,
            options = options,
            isError = false,
            enableWhen = builderEnableWhen,
            enableBehavior = behavior,
            repeats = isRepeats,
        )
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
     * @param options Optional custom options for the item. Defaults to resolved widget options.
     */
    fun addItem(
        widgetType: WidgetType,
        label: kotlin.String? = null,
        options: List<kotlin.String>? = null,
    ) {
        val currentItems = _state.value.items
        val newId = "item_${nextItemId++}"

        val defaultOptions = options ?: defaultOptionsResolver(widgetType)

        /** SINGLE_SELECT */
        val isError = (widgetType == WidgetType.SINGLE_SELECT || widgetType == WidgetType.MULTI_SELECT)
        val newItem =
            BuilderItem(
                linkId = newId,
                label = label ?: widgetItemLabelResolver(widgetType),
                widgetType = widgetType,
                options = defaultOptions,
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
                                                item.widgetType == WidgetType.MULTI_SELECT ||
                                                item.widgetType == WidgetType.SEGMENTED_TILES
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
     * Updates the enableWhen conditions and enableBehavior for an item.
     *
     * @param linkId The ID of the item to update.
     * @param enableWhen The list of enableWhen condition rules.
     * @param enableBehavior The behavior for combining conditions (All or Any).
     */
    fun updateItemEnableWhen(
        linkId: kotlin.String,
        enableWhen: List<BuilderEnableWhen>,
        enableBehavior: Questionnaire.EnableWhenBehavior? = null,
    ) {
        _state.update { currentState ->
            currentState.copy(
                items =
                    currentState.items.map { item ->
                        if (item.linkId == linkId) {
                            item.copy(enableWhen = enableWhen, enableBehavior = enableBehavior)
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
     * @return A [Result] enclosing the built [Questionnaire] if valid, or a failure result.
     */
    fun validate(): Result<Questionnaire> {
        if (_state.value.items.any { it.isError }) {
            return Result.failure(IllegalStateException("One or more questionnaire items contain errors"))
        }
        val questionnaire = buildQuestionnaire()
        val validationResult =
            io.healthplatform.chartcam.validation.FhirValidator
                .validate(questionnaire)
        return validationResult.map { questionnaire }
    }

    /**
     * Removes an item from the builder and purges dangling enableWhen rules referencing the removed item.
     *
     * @param linkId The ID of the item to remove.
     * @return A [Result] indicating success or failure of the removal operation.
     */
    fun removeItem(linkId: kotlin.String): Result<Unit> =
        runCatching {
            _state.update { currentState ->
                val remaining = currentState.items.filter { it.linkId != linkId }
                val sanitized =
                    remaining.map { item ->
                        val cleanedConditions = item.enableWhen.filter { it.question != linkId }
                        if (cleanedConditions.size != item.enableWhen.size) {
                            item.copy(enableWhen = cleanedConditions)
                        } else {
                            item
                        }
                    }
                currentState.copy(items = sanitized)
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
        val rawSlug =
            currentState.title
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
        val titleHash = kotlin.math.abs(currentState.title.hashCode()).toString(16)
        val id = if (rawSlug.isNotEmpty()) "custom-$rawSlug" else "custom-i18n-$titleHash"

        val fhirItems = currentState.items.map { mapBuilderItemToFhir(it) }

        return Questionnaire
            .Builder(Enumeration(value = PublicationStatus.Active))
            .apply {
                this.id = id
                this.url = Uri(value = "http://healthplatform.io/fhir/Questionnaire/$id").toBuilder()
                this.title = FhirString(value = currentState.title).toBuilder()
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
                    FhirString(value = builderItem.linkId).toBuilder(),
                    Enumeration(value = fhirType),
                ).apply {
                    text = FhirString(value = builderItem.label).toBuilder()
                    required = FhirBoolean(value = false).toBuilder()
                    if (builderItem.repeats) {
                        repeats = FhirBoolean(value = true).toBuilder()
                    }
                }
        applyChoiceOptions(itemBuilder, builderItem, fhirType)
        applyItemControl(itemBuilder, builderItem)
        applyEnableWhen(itemBuilder, builderItem)
        return itemBuilder
    }

    /**
     * Creates an appropriate FHIR EnableWhen.Answer from a BuilderEnableWhen configuration.
     *
     * @param ew The builder enableWhen condition.
     * @return The populated FHIR EnableWhen.Answer variant.
     */
    private fun createEnableWhenAnswer(ew: BuilderEnableWhen): Questionnaire.Item.EnableWhen.Answer =
        when {
            ew.answerBoolean != null ->
                Questionnaire.Item.EnableWhen.Answer.Boolean(
                    FhirBoolean(value = ew.answerBoolean),
                )
            ew.answerInteger != null ->
                Questionnaire.Item.EnableWhen.Answer.Integer(
                    Integer(value = ew.answerInteger),
                )
            ew.answerDecimal != null ->
                Questionnaire.Item.EnableWhen.Answer.Decimal(
                    Decimal(
                        value =
                            dev.ohs.fhir.model.r4.FhirDecimal
                                .fromString(ew.answerDecimal.toString()),
                    ),
                )
            ew.answerString != null ->
                Questionnaire.Item.EnableWhen.Answer.String(
                    FhirString(value = ew.answerString),
                )
            else ->
                Questionnaire.Item.EnableWhen.Answer.Boolean(
                    FhirBoolean(value = true),
                )
        }

    /**
     * Applies enableWhen conditions to the item builder.
     * @param itemBuilder The target Questionnaire.Item.Builder.
     * @param builderItem The source BuilderItem containing configuration.
     */
    private fun applyEnableWhen(
        itemBuilder: Questionnaire.Item.Builder,
        builderItem: BuilderItem,
    ) {
        if (builderItem.enableWhen.isNotEmpty()) {
            if (builderItem.enableBehavior != null) {
                itemBuilder.enableBehavior = Enumeration(value = builderItem.enableBehavior)
            }
            builderItem.enableWhen.forEach { ew ->
                itemBuilder.enableWhen.add(
                    Questionnaire.Item.EnableWhen.Builder(
                        answer = createEnableWhenAnswer(ew),
                        operator = Enumeration(value = ew.operator),
                        question = FhirString(value = ew.question).toBuilder(),
                    ),
                )
            }
        }
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
                itemBuilder.repeats = FhirBoolean(value = true).toBuilder()
            }
            val effectiveOptions =
                if (builderItem.options.isEmpty() && builderItem.widgetType == WidgetType.FITZPATRICK_PALETTE) {
                    listOf("Type I", "Type II", "Type III", "Type IV", "Type V", "Type VI")
                } else {
                    builderItem.options
                }
            effectiveOptions.forEachIndexed { index, optionValue ->
                itemBuilder.answerOption.add(
                    Questionnaire.Item.AnswerOption.Builder(
                        Questionnaire.Item.AnswerOption.Value.Coding(
                            dev.ohs.fhir.model.r4.Coding(
                                system =
                                    dev.ohs.fhir.model.r4
                                        .Uri(value = "http://chartcam.local/custom-options"),
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code(value = "opt-$index"),
                                display = FhirString(value = optionValue),
                            ),
                        ),
                    ),
                )
            }
        }
    }

    /**
     * Resolves the FHIR questionnaire-itemControl extension code.
     *
     * @param widgetType The widget type.
     * @return The item control code string or null.
     */
    private fun resolveItemControlCode(widgetType: WidgetType): String? =
        when (widgetType) {
            WidgetType.VIDEO_CAMERA -> "video"
            WidgetType.PHOTO_CAMERA -> "photo"
            WidgetType.SWITCH -> "switch"
            WidgetType.RANGE -> "slider"
            WidgetType.PAIN_SCALE -> "pain-vas"
            WidgetType.FITZPATRICK_PALETTE -> "palette"
            WidgetType.BODY_MAP -> "body-map"
            WidgetType.SEGMENTED_TILES -> "segmented-control"
            WidgetType.SINGLE_SELECT, WidgetType.MULTI_SELECT -> "check-box"
            else -> null
        }

    /**
     * Applies standard LOINC pain scale coding to the item builder.
     *
     * @param itemBuilder The item builder.
     */
    private fun applyPainScaleCode(itemBuilder: Questionnaire.Item.Builder) {
        itemBuilder.code.add(
            dev.ohs.fhir.model.r4
                .Coding(
                    system =
                        dev.ohs.fhir.model.r4
                            .Uri(value = "http://loinc.org"),
                    code =
                        dev.ohs.fhir.model.r4
                            .Code(value = "72514-3"),
                    display = FhirString(value = "Pain severity - 0-10 verbal numeric rating"),
                ).toBuilder(),
        )
    }

    /**
     * Applies item control.
     *
     * @param itemBuilder The itemBuilder.
     * @param builderItem The builderItem.
     */
    private fun applyItemControl(
        itemBuilder: Questionnaire.Item.Builder,
        builderItem: BuilderItem,
    ) {
        val itemControlCode = resolveItemControlCode(builderItem.widgetType)
        if (itemControlCode != null) {
            itemBuilder.extension.add(
                dev.ohs.fhir.model.r4.Extension
                    .Builder(
                        url = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    ).apply {
                        value =
                            dev.ohs.fhir.model.r4.Extension.Value.CodeableConcept(
                                dev.ohs.fhir.model.r4.CodeableConcept(
                                    coding =
                                        listOf(
                                            dev.ohs.fhir.model.r4.Coding(
                                                code =
                                                    dev.ohs.fhir.model.r4
                                                        .Code(value = itemControlCode),
                                            ),
                                        ),
                                ),
                            )
                    },
            )
        }
        if (builderItem.widgetType == WidgetType.PAIN_SCALE) {
            applyPainScaleCode(itemBuilder)
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
            WidgetType.SINGLE_SELECT,
            WidgetType.MULTI_SELECT,
            WidgetType.FITZPATRICK_PALETTE,
            WidgetType.SEGMENTED_TILES,
            -> Questionnaire.QuestionnaireItemType.Choice
            WidgetType.SINGLE_LINE_TEXT,
            WidgetType.BODY_MAP,
            -> Questionnaire.QuestionnaireItemType.String
            WidgetType.MULTI_LINE_TEXT -> Questionnaire.QuestionnaireItemType.Text
            WidgetType.DATE -> Questionnaire.QuestionnaireItemType.Date
            WidgetType.DATETIME -> Questionnaire.QuestionnaireItemType.DateTime
            WidgetType.NUMERIC -> Questionnaire.QuestionnaireItemType.Decimal
            WidgetType.RANGE,
            WidgetType.PAIN_SCALE,
            -> Questionnaire.QuestionnaireItemType.Integer
            WidgetType.GROUP -> Questionnaire.QuestionnaireItemType.Group
        }

    /**
     * Saves the current builder state as a FHIR Questionnaire in the repository.
     * @return The ID of the newly created Questionnaire, or null if validation failed.
     */
    fun saveQuestionnaire(): kotlin.String? {
        var finalId: kotlin.String? = null

        validate().onSuccess { questionnaire ->
            val currentId = questionnaire.id
            val existing = repository.getAvailableQuestionnaires().firstOrNull { it.id == currentId }

            if (existing != null) {
                _state.update { it.copy(isDuplicateNameError = true) }
            } else {
                repository.saveQuestionnaire(questionnaire)
                finalId = currentId
            }
        }

        return finalId
    }
}
