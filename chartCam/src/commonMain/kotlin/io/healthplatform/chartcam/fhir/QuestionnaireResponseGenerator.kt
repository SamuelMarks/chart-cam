/**
 * @file QuestionnaireResponseGenerator.kt
 * Contains declarations for QuestionnaireResponseGenerator.kt.
 */
package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Attachment
import dev.ohs.fhir.model.r4.Canonical
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Date
import dev.ohs.fhir.model.r4.DateTime
import dev.ohs.fhir.model.r4.Decimal
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.FhirDecimal
import dev.ohs.fhir.model.r4.Integer
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Url
import io.healthplatform.chartcam.models.BodyMapLocation
import io.healthplatform.chartcam.models.FitzpatrickSkinType
import io.healthplatform.chartcam.sdc.SdcEvaluator
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Utility functions for generating QuestionnaireResponse resources from UI Maps.
 */
object QuestionnaireResponseGenerator {
    /**
     * Converts a map of generic answers to a structured FHIR QuestionnaireResponse.
     *
     * @param questionnaire The source FHIR Questionnaire being answered.
     * @param answers The untyped map of answers collected from the UI.
     * @return A standard FHIR QuestionnaireResponse resource.
     */
    fun generate(
        questionnaire: Questionnaire,
        answers: Map<String, Any>,
    ): QuestionnaireResponse {
        val responseItemBuilders =
            questionnaire.item.flatMap { item ->
                createResponseItemBuilders(item, emptyList(), answers)
            }

        val canonicalUri =
            questionnaire.id?.let {
                Canonical(value = "Questionnaire/$it")
            }

        return QuestionnaireResponse(
            status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
            item = responseItemBuilders.map { it.build() },
            questionnaire = canonicalUri,
        )
    }

    /**
     * Converts a map of generic answers to a structured FHIR QuestionnaireResponse, returning a [Result].
     *
     * @param questionnaire The source FHIR Questionnaire being answered.
     * @param answers The untyped map of answers collected from the UI.
     * @return A [Result] enclosing the standard FHIR QuestionnaireResponse resource.
     */
    fun generateResult(
        questionnaire: Questionnaire,
        answers: Map<String, Any>,
    ): Result<QuestionnaireResponse> = runCatching { generate(questionnaire, answers) }

    /**
     * Creates builders for [QuestionnaireResponse.Item], handling both single and repeating items.
     *
     * @param item The questionnaire item.
     * @param ancestors The list of ancestor items.
     * @param answers The map of answers.
     * @param scopedKeyPrefix Optional repeating group prefix.
     * @return A list of builders for the questionnaire response item.
     */
    private fun createResponseItemBuilders(
        item: Questionnaire.Item,
        ancestors: List<Questionnaire.Item>,
        answers: Map<String, Any>,
        scopedKeyPrefix: String? = null,
    ): List<QuestionnaireResponse.Item.Builder> {
        val isEnabled = SdcEvaluator.isItemHierarchyEnabled(item, ancestors, answers)
        if (!isEnabled) {
            return emptyList()
        }

        val linkId = item.linkId.value
        val isRepeatingGroup =
            linkId != null &&
                item.type.value == Questionnaire.QuestionnaireItemType.Group &&
                item.repeats?.value == true

        return if (isRepeatingGroup) {
            val repeatIndices =
                answers.keys
                    .filter { it.startsWith("$linkId#") }
                    .mapNotNull { key ->
                        val afterHash = key.substringAfter("$linkId#")
                        afterHash.substringBefore('.').toIntOrNull()
                    }.distinct()
                    .sorted()

            val indices = if (repeatIndices.isEmpty()) listOf(0) else repeatIndices
            indices.mapNotNull { idx ->
                val childPrefix = "$linkId#$idx"
                buildSingleItemBuilder(item, ancestors, answers, scopedKeyPrefix = childPrefix)
            }
        } else {
            listOfNotNull(buildSingleItemBuilder(item, ancestors, answers, scopedKeyPrefix = scopedKeyPrefix))
        }
    }

    /**
     * Resolves the answer lookup key based on repeating group prefix.
     *
     * @param linkId The question link identifier.
     * @param prefix Optional scoped repeating prefix.
     * @return The resolved lookup key.
     */
    private fun resolveLookupKey(linkId: String, prefix: String?): String =
        if (prefix != null && !linkId.startsWith(prefix)) "$prefix.$linkId" else linkId

    /**
     * Determines whether an answer object carries meaningful content.
     *
     * @param answerValue The answer candidate.
     * @return True if populated.
     */
    private fun hasPopulatedAnswer(answerValue: Any?): Boolean =
        when (answerValue) {
            null -> false
            is String -> answerValue.isNotBlank()
            is List<*> -> answerValue.isNotEmpty()
            else -> true
        }

    /**
     * Builds a single builder for a [QuestionnaireResponse.Item] from a given [Questionnaire.Item] and answers map.
     *
     * @param item The questionnaire item.
     * @param ancestors The list of ancestor items.
     * @param answers The map of answers.
     * @param scopedKeyPrefix Optional repeating group prefix scoping this item's answer lookup.
     * @return The populated builder, or null if the item has no answer and no populated nested children.
     */
    private fun buildSingleItemBuilder(
        item: Questionnaire.Item,
        ancestors: List<Questionnaire.Item>,
        answers: Map<String, Any>,
        scopedKeyPrefix: String?,
    ): QuestionnaireResponse.Item.Builder? {
        val linkId = item.linkId.value ?: return null
        val lookupKey = resolveLookupKey(linkId, scopedKeyPrefix)
        val answerValue = answers[lookupKey] ?: answers[linkId]
        val nestedItemBuilders =
            item.item.flatMap { child ->
                createResponseItemBuilders(child, ancestors + item, answers, scopedKeyPrefix)
            }

        val hasAnswer = hasPopulatedAnswer(answerValue)
        return if (hasAnswer || nestedItemBuilders.isNotEmpty()) {
            QuestionnaireResponse.Item.Builder(FhirString(value = linkId).toBuilder()).also { builder ->
                builder.text = item.text?.toBuilder()
                if (answerValue != null) {
                    populateAnswers(item, answerValue, builder)
                }
                if (nestedItemBuilders.isNotEmpty()) {
                    builder.item.addAll(nestedItemBuilders)
                }
            }
        } else {
            null
        }
    }

    /**
     * Helper function for processing questionnaire answers.
     *
     * @param item The item.
     * @param answerValue The answerValue.
     * @param builder The builder.
     */
    private fun populateAnswers(
        item: Questionnaire.Item,
        answerValue: Any,
        builder: QuestionnaireResponse.Item.Builder,
    ) {
        val itemType = item.type.value ?: return
        if (itemType == Questionnaire.QuestionnaireItemType.Choice) {
            populateChoiceAnswer(item, answerValue, builder)
        } else {
            populateNonChoiceAnswer(itemType, answerValue, builder)
        }
    }

    /**
     * Populates choice question answer into the response builder.
     * Generates Value.Coding when matching an answerOption.
     *
     * @param item The template question item containing options.
     * @param answerValue The raw answer value.
     * @param builder The response item builder.
     */
    private fun populateChoiceAnswer(
        item: Questionnaire.Item,
        answerValue: Any,
        builder: QuestionnaireResponse.Item.Builder,
    ) {
        when (answerValue) {
            is FitzpatrickSkinType -> populateStringOption(item, "Type ${answerValue.romanNumeral}", builder)
            is Coding -> addCodingAnswer(builder, answerValue)
            is List<*> -> answerValue.forEach { populateListChoiceAnswer(item, it, builder) }
            is String -> populateStringOption(item, answerValue, builder)
        }
    }

    /**
     * Populates list element choice answers.
     *
     * @param item The template item.
     * @param opt The raw option element.
     * @param builder The response item builder.
     */
    private fun populateListChoiceAnswer(
        item: Questionnaire.Item,
        opt: Any?,
        builder: QuestionnaireResponse.Item.Builder,
    ) {
        when (opt) {
            is Coding -> addCodingAnswer(builder, opt)
            is String -> populateStringOption(item, opt, builder)
        }
    }

    /**
     * Populates a single string choice option, using Coding when declared in answerOption.
     *
     * @param item The template question item.
     * @param opt The string answer.
     * @param builder The response item builder.
     */
    private fun populateStringOption(
        item: Questionnaire.Item,
        opt: String,
        builder: QuestionnaireResponse.Item.Builder,
    ) {
        val matchingCoding = findMatchingOptionCoding(item, opt)
        if (matchingCoding != null) {
            addCodingAnswer(builder, matchingCoding)
        } else {
            addStringAnswer(builder, opt)
        }
    }

    /**
     * Looks up a matching Coding from the question's declared answerOption list.
     *
     * @param item The template question item.
     * @param answer The raw answer string.
     * @return The matching [Coding], or null if none match.
     */
    private fun findMatchingOptionCoding(
        item: Questionnaire.Item,
        answer: String,
    ): Coding? {
        val trimmed = answer.trim()
        for (option in item.answerOption) {
            val optionValue = option.value
            if (optionValue !is Questionnaire.Item.AnswerOption.Value.Coding) continue
            val coding = optionValue.value
            val code = coding.code?.value?.trim()
            val display = coding.display?.value?.trim()
            if (code.equals(trimmed, ignoreCase = true) || display.equals(trimmed, ignoreCase = true)) {
                return coding
            }
        }
        return null
    }

    /**
     * Adds a [Coding] value answer to the response builder.
     *
     * @param builder The response item builder.
     * @param coding The FHIR [Coding] to append.
     */
    private fun addCodingAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        coding: Coding,
    ) {
        builder.answer.add(
            QuestionnaireResponse.Item.Answer.Builder().apply {
                value =
                    QuestionnaireResponse.Item.Answer.Value
                        .Coding(coding)
            },
        )
    }

    /**
     * Populates non-choice primitive question answers into the response builder.
     *
     * @param itemType The FHIR item type.
     * @param answerValue The raw answer value.
     * @param builder The response item builder.
     */
    private fun populateNonChoiceAnswer(
        itemType: Questionnaire.QuestionnaireItemType,
        answerValue: Any,
        builder: QuestionnaireResponse.Item.Builder,
    ) {
        when (itemType) {
            Questionnaire.QuestionnaireItemType.String,
            Questionnaire.QuestionnaireItemType.Text,
            -> {
                val strVal =
                    when (answerValue) {
                        is BodyMapLocation -> answerValue.toSerializedString()
                        is String -> answerValue
                        else -> answerValue.toString()
                    }
                addStringAnswer(builder, strVal)
            }
            Questionnaire.QuestionnaireItemType.Boolean -> {
                addBooleanAnswer(builder, answerValue as? Boolean ?: false)
            }
            Questionnaire.QuestionnaireItemType.Decimal -> addDecimalAnswer(builder, answerValue)
            Questionnaire.QuestionnaireItemType.Integer -> addIntegerAnswer(builder, answerValue)
            Questionnaire.QuestionnaireItemType.Date,
            Questionnaire.QuestionnaireItemType.DateTime,
            Questionnaire.QuestionnaireItemType.Attachment,
            ->
                populateTemporalOrAttachment(itemType, answerValue, builder)
            else -> {}
        }
    }

    /**
     * Populates temporal and attachment answer variants into the response item builder.
     *
     * @param itemType The FHIR item type.
     * @param answerValue The raw answer value.
     * @param builder The response item builder.
     */
    private fun populateTemporalOrAttachment(
        itemType: Questionnaire.QuestionnaireItemType,
        answerValue: Any,
        builder: QuestionnaireResponse.Item.Builder,
    ) {
        when (itemType) {
            Questionnaire.QuestionnaireItemType.Date -> {
                (answerValue as? String)?.let { addDateAnswer(builder, it) }
            }
            Questionnaire.QuestionnaireItemType.DateTime -> {
                (answerValue as? String)?.let { addDateTimeAnswer(builder, it) }
            }
            Questionnaire.QuestionnaireItemType.Attachment -> {
                when (answerValue) {
                    is Attachment -> addAttachmentAnswer(builder, answerValue)
                    is String -> addAttachmentAnswer(builder, Attachment(url = Url(value = answerValue)))
                    else -> {}
                }
            }
            else -> {}
        }
    }

    /**
     * Helper function for processing attachment questionnaire answers.
     *
     * @param builder The builder.
     * @param attachment The attachment to add.
     */
    private fun addAttachmentAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        attachment: Attachment,
    ) {
        builder.answer.add(
            QuestionnaireResponse.Item.Answer.Builder().apply {
                value =
                    QuestionnaireResponse.Item.Answer.Value
                        .Attachment(attachment)
            },
        )
    }

    /**
     * Helper function for processing string questionnaire answers.
     *
     * @param builder The builder.
     * @param answerValue The answerValue.
     */
    private fun addStringAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: String,
    ) {
        builder.answer.add(
            QuestionnaireResponse.Item.Answer.Builder().apply {
                value =
                    QuestionnaireResponse.Item.Answer.Value
                        .String(FhirString(value = answerValue))
            },
        )
    }

    /**
     * Helper function for processing boolean questionnaire answers.
     *
     * @param builder The builder.
     * @param answerValue The answerValue.
     */
    private fun addBooleanAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: Boolean,
    ) {
        builder.answer.add(
            QuestionnaireResponse.Item.Answer.Builder().apply {
                value =
                    QuestionnaireResponse.Item.Answer.Value
                        .Boolean(FhirBoolean(value = answerValue))
            },
        )
    }

    /**
     * Helper function for processing decimal questionnaire answers.
     *
     * @param builder The builder.
     * @param answerValue The answerValue.
     */
    private fun addDecimalAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: Any,
    ) {
        val decimalResult: Result<FhirDecimal> =
            when (answerValue) {
                is FhirDecimal -> Result.success(answerValue)
                is String -> runCatching { FhirDecimal.fromString(answerValue.trim()) }
                is Number -> runCatching { FhirDecimal.fromString(answerValue.toString()) }
                else -> Result.failure(IllegalArgumentException("Unsupported decimal value: $answerValue"))
            }

        decimalResult.onSuccess { decimalValue ->
            builder.answer.add(
                QuestionnaireResponse.Item.Answer.Builder().apply {
                    value =
                        QuestionnaireResponse.Item.Answer.Value
                            .Decimal(Decimal(value = decimalValue))
                },
            )
        }
    }

    /**
     * Helper function for processing integer questionnaire answers.
     *
     * @param builder The builder.
     * @param answerValue The answerValue.
     */
    private fun addIntegerAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: Any,
    ) {
        val intVal = (answerValue as? Number)?.toInt() ?: (answerValue as? String)?.toIntOrNull()
        if (intVal != null) {
            builder.answer.add(
                QuestionnaireResponse.Item.Answer.Builder().apply {
                    value =
                        QuestionnaireResponse.Item.Answer.Value
                            .Integer(Integer(value = intVal))
                },
            )
        }
    }

    /**
     * Helper function for processing date questionnaire answers safely with Result.
     *
     * @param builder The builder.
     * @param answerValue The raw date answer string.
     * @return A [Result] indicating success or failure.
     */
    private fun addDateAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: String,
    ): Result<Unit> =
        runCatching {
            val trimmed = answerValue.trim()
            val fhirDate = FhirDate.fromString(trimmed)
            builder.answer.add(
                QuestionnaireResponse.Item.Answer.Builder().apply {
                    value =
                        QuestionnaireResponse.Item.Answer.Value
                            .Date(Date(value = fhirDate))
                },
            )
        }

    /**
     * Helper function for processing datetime questionnaire answers safely with Result.
     *
     * @param builder The builder.
     * @param answerValue The raw datetime answer string.
     * @return A [Result] indicating success or failure.
     */
    private fun addDateTimeAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: String,
    ): Result<Unit> =
        runCatching {
            val trimmed = answerValue.trim()
            val fhirDateTime = FhirDateTime.fromString(trimmed)
            builder.answer.add(
                QuestionnaireResponse.Item.Answer.Builder().apply {
                    value =
                        QuestionnaireResponse.Item.Answer.Value
                            .DateTime(DateTime(value = fhirDateTime))
                },
            )
        }
}
