/**
 * @file QuestionnaireResponseGenerator.kt
 * Contains declarations for QuestionnaireResponseGenerator.kt.
 */
package io.healthplatform.chartcam.fhir

import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Date
import com.google.fhir.model.r4.DateTime
import com.google.fhir.model.r4.Decimal
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirDate
import com.google.fhir.model.r4.FhirDateTime
import com.google.fhir.model.r4.Integer
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.String
import com.ionspin.kotlin.bignum.decimal.BigDecimal

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
        answers: Map<kotlin.String, Any>,
    ): QuestionnaireResponse {
        val responseItemBuilders =
            questionnaire.item.flatMap { item ->
                createResponseItemBuilders(item, emptyList(), answers)
            }

        return QuestionnaireResponse
            .Builder(
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
            ).apply {
                this.item.addAll(responseItemBuilders)
                this.questionnaire =
                    com.google.fhir.model.r4.Canonical.Builder().apply {
                        value = questionnaire.id?.let { "Questionnaire/$it" } ?: ""
                    }
            }.build()
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
        answers: Map<kotlin.String, Any>,
    ): Result<QuestionnaireResponse> = runCatching { generate(questionnaire, answers) }

    /**
     * Creates builders for [QuestionnaireResponse.Item], handling both single and repeating items.
     *
     * @param item The questionnaire item.
     * @param ancestors The list of ancestor items.
     * @param answers The map of answers.
     * @return A list of builders for the questionnaire response item.
     */
    private fun createResponseItemBuilders(
        item: Questionnaire.Item,
        ancestors: List<Questionnaire.Item>,
        answers: Map<kotlin.String, Any>,
    ): List<QuestionnaireResponse.Item.Builder> {
        val linkId = item.linkId.value
        val isEnabled =
            linkId != null &&
                io.healthplatform.chartcam.sdc.SdcEvaluator
                    .isItemHierarchyEnabled(item, ancestors, answers)
        if (!isEnabled) {
            return emptyList()
        }

        val isRepeatingGroup =
            item.type.value == Questionnaire.QuestionnaireItemType.Group && item.repeats?.value == true

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
                val scopedAnswers =
                    answers
                        .filterKeys { it.startsWith("$linkId#$idx.") }
                        .mapKeys { it.key.substringAfter("$linkId#$idx.") }
                val mergedAnswers = if (idx == 0) answers + scopedAnswers else scopedAnswers
                createSingleResponseItemBuilder(item, ancestors, mergedAnswers)
            }
        } else {
            val single = createSingleResponseItemBuilder(item, ancestors, answers)
            if (single != null) listOf(single) else emptyList()
        }
    }

    /**
     * Creates a single builder for a [QuestionnaireResponse.Item] from a given [Questionnaire.Item] and answers map.
     *
     * @param item The questionnaire item.
     * @param ancestors The list of ancestor items.
     * @param answers The map of answers.
     * @return A builder for the questionnaire response item, or null if it cannot be built.
     */
    private fun createSingleResponseItemBuilder(
        item: Questionnaire.Item,
        ancestors: List<Questionnaire.Item>,
        answers: Map<kotlin.String, Any>,
    ): QuestionnaireResponse.Item.Builder? {
        val linkId = item.linkId.value ?: return null

        val answerValue = answers[linkId]
        val nextAncestors = ancestors + item
        val nestedItemBuilders = item.item.flatMap { createResponseItemBuilders(it, nextAncestors, answers) }

        return if (answerValue == null && nestedItemBuilders.isEmpty()) {
            null
        } else {
            val builder = QuestionnaireResponse.Item.Builder(linkId = String.Builder().apply { value = linkId })
            builder.text = String.Builder().apply { value = item.text?.value ?: "" }
            if (answerValue != null) {
                populateAnswers(item, answerValue, builder)
            }
            if (nestedItemBuilders.isNotEmpty()) {
                builder.item.addAll(nestedItemBuilders)
            }
            builder
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
            populateChoiceAnswer(answerValue, builder)
        } else {
            populateNonChoiceAnswer(itemType, answerValue, builder)
        }
    }

    /**
     * Populates choice question answer into the response builder.
     *
     * @param answerValue The raw answer value.
     * @param builder The response item builder.
     */
    private fun populateChoiceAnswer(
        answerValue: Any,
        builder: QuestionnaireResponse.Item.Builder,
    ) {
        if (answerValue is io.healthplatform.chartcam.models.FitzpatrickSkinType) {
            addStringAnswer(builder, "Type ${answerValue.romanNumeral}")
        } else {
            addChoiceAnswer(builder, answerValue)
        }
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
            Questionnaire.QuestionnaireItemType.String, Questionnaire.QuestionnaireItemType.Text -> {
                val strVal =
                    when (answerValue) {
                        is io.healthplatform.chartcam.models.BodyMapLocation -> answerValue.toSerializedString()
                        is kotlin.String -> answerValue
                        else -> answerValue.toString()
                    }
                addStringAnswer(builder, strVal)
            }
            Questionnaire.QuestionnaireItemType.Boolean -> {
                addBooleanAnswer(builder, answerValue as? kotlin.Boolean ?: false)
            }
            Questionnaire.QuestionnaireItemType.Decimal -> addDecimalAnswer(builder, answerValue)
            Questionnaire.QuestionnaireItemType.Integer -> addIntegerAnswer(builder, answerValue)
            Questionnaire.QuestionnaireItemType.Date -> {
                addDateAnswer(builder, answerValue as? kotlin.String ?: "")
            }
            Questionnaire.QuestionnaireItemType.DateTime -> {
                addDateTimeAnswer(builder, answerValue as? kotlin.String ?: "")
            }
            else -> {}
        }
    }

    /**
     * Helper function for processing questionnaire answers.
     * @param builder The builder.
     * @param answerValue The answerValue.
     */
    private fun addStringAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: kotlin.String,
    ) {
        builder.answer.add(
            QuestionnaireResponse.Item.Answer.Builder().apply {
                value =
                    QuestionnaireResponse.Item.Answer.Value.String(
                        String.Builder().apply { value = answerValue }.build(),
                    )
            },
        )
    }

    /**
     * Helper function for processing questionnaire answers.
     * @param builder The builder.
     * @param answerValue The answerValue.
     */
    private fun addBooleanAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: kotlin.Boolean,
    ) {
        builder.answer.add(
            QuestionnaireResponse.Item.Answer.Builder().apply {
                value =
                    QuestionnaireResponse.Item.Answer.Value.Boolean(
                        Boolean.Builder().apply { value = answerValue }.build(),
                    )
            },
        )
    }

    /**
     * Helper function for processing questionnaire answers.
     * @param builder The builder.
     * @param answerValue The answerValue.
     */
    private fun addDecimalAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: Any,
    ) {
        val fl = (answerValue as? Number)?.toFloat() ?: (answerValue as? kotlin.String)?.toFloatOrNull()
        if (fl != null && !fl.isNaN() && !fl.isInfinite()) {
            val decimalValue = BigDecimal.parseString(fl.toString())
            builder.answer.add(
                QuestionnaireResponse.Item.Answer.Builder().apply {
                    value =
                        QuestionnaireResponse.Item.Answer.Value.Decimal(
                            Decimal.Builder().apply { value = decimalValue }.build(),
                        )
                },
            )
        }
    }

    /**
     * Helper function for processing questionnaire answers.
     * @param builder The builder.
     * @param answerValue The answerValue.
     */
    private fun addIntegerAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: Any,
    ) {
        val intVal = (answerValue as? Number)?.toInt() ?: (answerValue as? kotlin.String)?.toIntOrNull()
        if (intVal != null) {
            builder.answer.add(
                QuestionnaireResponse.Item.Answer.Builder().apply {
                    value =
                        QuestionnaireResponse.Item.Answer.Value.Integer(
                            Integer.Builder().apply { value = intVal }.build(),
                        )
                },
            )
        }
    }

    /**
     * Helper function for processing questionnaire answers.
     * @param builder The builder.
     * @param answerValue The answerValue.
     */
    private fun addDateAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: kotlin.String,
    ) {
        val fhirDate = FhirDate.fromString(answerValue)
        builder.answer.add(
            QuestionnaireResponse.Item.Answer.Builder().apply {
                value =
                    QuestionnaireResponse.Item.Answer.Value.Date(
                        Date.Builder().apply { value = fhirDate }.build(),
                    )
            },
        )
    }

    /**
     * Helper function for processing questionnaire answers.
     * @param builder The builder.
     * @param answerValue The answerValue.
     */
    private fun addDateTimeAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: kotlin.String,
    ) {
        val fhirDateTime = FhirDateTime.fromString(answerValue)
        builder.answer.add(
            QuestionnaireResponse.Item.Answer.Builder().apply {
                value =
                    QuestionnaireResponse.Item.Answer.Value.DateTime(
                        DateTime.Builder().apply { value = fhirDateTime }.build(),
                    )
            },
        )
    }

    /**
     * Helper function for processing questionnaire answers.
     * @param builder The builder.
     * @param answerValue The answerValue.
     */
    private fun addChoiceAnswer(
        builder: QuestionnaireResponse.Item.Builder,
        answerValue: Any,
    ) {
        if (answerValue is List<*>) {
            answerValue.filterIsInstance<kotlin.String>().forEach { opt ->
                addStringAnswer(builder, opt)
            }
        } else if (answerValue is kotlin.String) {
            addStringAnswer(builder, answerValue)
        }
    }
}
