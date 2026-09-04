/**
 * @file QuestionnaireUtils.kt
 * Contains declarations for QuestionnaireUtils.kt.
 */
package io.healthplatform.chartcam.utils

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.QuestionnaireResponse

private const val MAX_RECURSION_DEPTH = 50

/**
 * Utility functions for Questionnaire processing.
 */
object QuestionnaireUtils {
    /**
     * Recursively searches for a Questionnaire.Item by its linkId.
     *
     * @param items The list of items to search.
     * @param linkId The linkId to search for.
     * @return The found item or null.
     */
    fun findItemRecursively(
        items: List<Questionnaire.Item>,
        linkId: String,
    ): Questionnaire.Item? = findItemRecursivelyInternal(items, linkId, mutableSetOf(), 0)

    /**
     * Internal recursive finder with cycle protection.
     *
     * @param items The list of items to search.
     * @param linkId The linkId to search for.
     * @param visitedLinkIds The set of already visited item linkIds to prevent infinite recursion.
     * @param depth The current recursion depth.
     * @return The found item or null.
     */
    private fun findItemRecursivelyInternal(
        items: List<Questionnaire.Item>,
        linkId: String,
        visitedLinkIds: MutableSet<String>,
        depth: Int,
    ): Questionnaire.Item? {
        if (linkId.isBlank() || items.isEmpty() || depth > MAX_RECURSION_DEPTH) {
            return null
        }
        var found: Questionnaire.Item? = null
        val iterator = items.iterator()
        while (iterator.hasNext() && found == null) {
            val item = iterator.next()
            val itemLinkId = item.linkId.value
            val alreadyVisited = itemLinkId != null && !visitedLinkIds.add(itemLinkId)
            if (!alreadyVisited) {
                if (itemLinkId == linkId) {
                    found = item
                } else {
                    found = findItemRecursivelyInternal(item.item, linkId, visitedLinkIds, depth + 1)
                }
            }
        }
        return found
    }

    /**
     * Resolves the label for a question by linkId with fallback for missing or blank values.
     *
     * @param items The list of Questionnaire.Item to search.
     * @param linkId The linkId to resolve label for.
     * @param fallback The fallback label if text or item is missing or blank.
     * @return The resolved label string.
     */
    fun resolveLabel(
        items: List<Questionnaire.Item>,
        linkId: String,
        fallback: String = linkId,
    ): String {
        if (items.isEmpty()) return if (linkId.isNotBlank()) linkId else fallback
        val item = findItemRecursively(items, linkId)
        val textValue = item?.text?.value
        return when {
            !textValue.isNullOrBlank() -> textValue
            linkId.isNotBlank() -> linkId
            else -> fallback
        }
    }

    /**
     * Recursively builds dummy Questionnaire.Item.Builders from a QuestionnaireResponse.Item tree.
     *
     * @param qrItems The items from a QuestionnaireResponse.
     * @return A list of generated Questionnaire.Item.Builder instances.
     */
    fun buildDummyItemsRecursively(qrItems: List<QuestionnaireResponse.Item>): List<Questionnaire.Item.Builder> {
        val dummyItems = mutableListOf<Questionnaire.Item.Builder>()
        qrItems.forEach { qrItem ->
            val linkId = qrItem.linkId.value ?: return@forEach
            val answer = qrItem.answer.firstOrNull()?.value
            val qItemType = determineDummyItemType(answer, qrItem)

            val builder =
                Questionnaire.Item
                    .Builder(
                        com.google.fhir.model.r4.String
                            .Builder()
                            .apply { value = linkId },
                        Enumeration(value = qItemType),
                    ).apply {
                        this.text =
                            com.google.fhir.model.r4.String.Builder().apply {
                                value = linkId.replaceFirstChar { it.uppercase() }
                            }
                        if (qrItem.item.isNotEmpty()) {
                            this.item.addAll(buildDummyItemsRecursively(qrItem.item))
                        }
                    }
            dummyItems.add(builder)
        }
        return dummyItems
    }

    /**
     * Determines dummy item type.
     * @param answer The answer.
     * @param qrItem The qrItem.
     * @return The result.
     */
    private fun determineDummyItemType(
        answer: QuestionnaireResponse.Item.Answer.Value?,
        qrItem: QuestionnaireResponse.Item,
    ): Questionnaire.QuestionnaireItemType =
        if (answer != null) {
            when (answer) {
                is QuestionnaireResponse.Item.Answer.Value.String -> Questionnaire.QuestionnaireItemType.String
                is QuestionnaireResponse.Item.Answer.Value.Boolean -> Questionnaire.QuestionnaireItemType.Boolean
                is QuestionnaireResponse.Item.Answer.Value.Attachment -> Questionnaire.QuestionnaireItemType.Attachment
                is QuestionnaireResponse.Item.Answer.Value.Decimal -> Questionnaire.QuestionnaireItemType.Decimal
                is QuestionnaireResponse.Item.Answer.Value.Integer -> Questionnaire.QuestionnaireItemType.Integer
                is QuestionnaireResponse.Item.Answer.Value.Date -> Questionnaire.QuestionnaireItemType.Date
                is QuestionnaireResponse.Item.Answer.Value.DateTime -> Questionnaire.QuestionnaireItemType.DateTime
                else -> Questionnaire.QuestionnaireItemType.String
            }
        } else if (qrItem.item.isNotEmpty()) {
            Questionnaire.QuestionnaireItemType.Group
        } else {
            Questionnaire.QuestionnaireItemType.String
        }

    /**
     * Recursively builds QuestionnaireResponse.Item from Questionnaire.Item based on current answers.
     *
     * @param qItems The list of Questionnaire.Item to traverse.
     * @param answers The map of current answers.
     * @return A list of populated QuestionnaireResponse.Item.Builder instances.
     */
    fun buildResponseItemsRecursively(
        qItems: List<Questionnaire.Item>,
        answers: Map<String, Any>,
    ): List<QuestionnaireResponse.Item.Builder> {
        val responseItems = mutableListOf<QuestionnaireResponse.Item.Builder>()
        for (qItem in qItems) {
            val linkId = qItem.linkId.value ?: continue
            val qType = qItem.type.value ?: Questionnaire.QuestionnaireItemType.String
            val answer = answers[linkId]

            val itemBuilder =
                QuestionnaireResponse.Item
                    .Builder(
                        com.google.fhir.model.r4.String
                            .Builder()
                            .apply { value = linkId },
                    ).apply {
                        this.text = qItem.text?.toBuilder()
                    }

            var hasAnswer = false

            if (answer != null) {
                hasAnswer = applyAnswerToItem(itemBuilder, answer, qType)
            }

            if (qItem.item.isNotEmpty()) {
                val nestedItems = buildResponseItemsRecursively(qItem.item, answers)
                if (nestedItems.isNotEmpty()) {
                    itemBuilder.item.addAll(nestedItems)
                    hasAnswer = true
                }
            }

            if (hasAnswer || qType == Questionnaire.QuestionnaireItemType.Group) {
                responseItems.add(itemBuilder)
            }
        }
        return responseItems
    }

    /**
     * Applies answer to item.
     * @param itemBuilder The itemBuilder.
     * @param answer The answer.
     * @param qType The qType.
     * @return The result.
     */
    private fun applyAnswerToItem(
        itemBuilder: QuestionnaireResponse.Item.Builder,
        answer: Any,
        qType: Questionnaire.QuestionnaireItemType,
    ): Boolean {
        var hasAnswer = false
        when (answer) {
            is String -> {
                if (answer.isNotBlank()) {
                    itemBuilder.answer.add(
                        QuestionnaireResponse.Item.Answer
                            .Builder()
                            .apply { value = mapStringAnswer(answer, qType) },
                    )
                    hasAnswer = true
                }
            }
            is List<*> -> {
                val strList = answer.filterIsInstance<String>().filter { it.isNotBlank() }
                if (strList.isNotEmpty()) {
                    strList.forEach { strVal ->
                        itemBuilder.answer.add(
                            QuestionnaireResponse.Item.Answer.Builder().apply {
                                value =
                                    com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String(
                                        com.google.fhir.model.r4.String
                                            .Builder()
                                            .apply { value = strVal }
                                            .build(),
                                    )
                            },
                        )
                    }
                    hasAnswer = true
                }
            }
            is Boolean -> {
                itemBuilder.answer.add(
                    QuestionnaireResponse.Item.Answer.Builder().apply {
                        value =
                            com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Boolean(
                                com.google.fhir.model.r4.Boolean
                                    .Builder()
                                    .apply { value = answer }
                                    .build(),
                            )
                    },
                )
                hasAnswer = true
            }
            is Float -> {
                itemBuilder.answer.add(
                    QuestionnaireResponse.Item.Answer
                        .Builder()
                        .apply { value = mapFloatAnswer(answer, qType) },
                )
                hasAnswer = true
            }
        }
        return hasAnswer
    }

    /**
     * Maps string answer.
     * @param answer The answer.
     * @param qType The qType.
     * @return The result.
     */
    private fun mapStringAnswer(
        answer: String,
        qType: Questionnaire.QuestionnaireItemType,
    ): com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value =
        when (qType) {
            Questionnaire.QuestionnaireItemType.Date ->
                com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Date(
                    com.google.fhir.model.r4.Date
                        .Builder()
                        .apply {
                            value =
                                com.google.fhir.model.r4.FhirDate
                                    .fromString(answer)
                        }.build(),
                )
            Questionnaire.QuestionnaireItemType.DateTime ->
                com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.DateTime(
                    com.google.fhir.model.r4.DateTime
                        .Builder()
                        .apply {
                            value =
                                com.google.fhir.model.r4.FhirDateTime
                                    .fromString(answer)
                        }.build(),
                )
            Questionnaire.QuestionnaireItemType.Decimal ->
                com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Decimal(
                    com.google.fhir.model.r4.Decimal
                        .Builder()
                        .apply {
                            value =
                                com.ionspin.kotlin.bignum.decimal.BigDecimal
                                    .parseString(answer)
                        }.build(),
                )
            Questionnaire.QuestionnaireItemType.Integer ->
                com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Integer(
                    com.google.fhir.model.r4.Integer
                        .Builder()
                        .apply {
                            value = answer.toIntOrNull() ?: 0
                        }.build(),
                )
            else ->
                com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String(
                    com.google.fhir.model.r4.String
                        .Builder()
                        .apply { value = answer }
                        .build(),
                )
        }

    /**
     * Maps float answer.
     * @param answer The answer.
     * @param qType The qType.
     * @return The result.
     */
    private fun mapFloatAnswer(
        answer: Float,
        qType: Questionnaire.QuestionnaireItemType,
    ): com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value =
        when (qType) {
            Questionnaire.QuestionnaireItemType.Integer ->
                com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Integer(
                    com.google.fhir.model.r4.Integer
                        .Builder()
                        .apply { value = answer.toInt() }
                        .build(),
                )
            else ->
                com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Decimal(
                    com.google.fhir.model.r4.Decimal
                        .Builder()
                        .apply {
                            value =
                                com.ionspin.kotlin.bignum.decimal.BigDecimal
                                    .parseString(answer.toString())
                        }.build(),
                )
        }

    /**
     * Extracts answers recursively.
     * @param items The items.
     * @param existingAnswers The existingAnswers.
     */
    fun extractAnswersRecursively(
        items: List<QuestionnaireResponse.Item>,
        existingAnswers: MutableMap<String, Any>,
    ) {
        items.forEach { item ->
            val linkId = item.linkId.value ?: return@forEach
            val answers = item.answer
            if (answers.size > 1) {
                existingAnswers[linkId] = extractListAnswer(answers)
            } else if (answers.size == 1) {
                answers.first().value?.let { extractSingleAnswer(linkId, it, existingAnswers) }
            }
            if (item.item.isNotEmpty()) {
                extractAnswersRecursively(item.item, existingAnswers)
            }
        }
    }

    /**
     * Extracts list answer.
     * @param answers The answers.
     * @return The result.
     */
    private fun extractListAnswer(answers: List<QuestionnaireResponse.Item.Answer>): List<String> =
        answers.mapNotNull { ans ->
            val strValue = ans.value as? com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String
            strValue?.value?.value
        }

    /**
     * Extracts single answer.
     * @param linkId The linkId.
     * @param answer The answer.
     * @param existingAnswers The existingAnswers.
     */
    private fun extractSingleAnswer(
        linkId: String,
        answer: com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value,
        existingAnswers: MutableMap<String, Any>,
    ) {
        extractStringOrBoolean(linkId, answer, existingAnswers)
        extractNumber(linkId, answer, existingAnswers)
        extractDate(linkId, answer, existingAnswers)
    }

    /**
     * Extracts string or boolean.
     * @param linkId The linkId.
     * @param answer The answer.
     * @param existingAnswers The existingAnswers.
     */
    private fun extractStringOrBoolean(
        linkId: String,
        answer: com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value,
        existingAnswers: MutableMap<String, Any>,
    ) {
        if (answer is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String) {
            answer.value.value?.let { existingAnswers[linkId] = it }
        } else if (answer is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Boolean) {
            answer.value.value?.let { existingAnswers[linkId] = it }
        }
    }

    /**
     * Extracts number.
     * @param linkId The linkId.
     * @param answer The answer.
     * @param existingAnswers The existingAnswers.
     */
    private fun extractNumber(
        linkId: String,
        answer: com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value,
        existingAnswers: MutableMap<String, Any>,
    ) {
        if (answer is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Decimal) {
            val decVal = answer.value.value
            if (decVal != null) {
                existingAnswers[linkId] = decVal.toStringExpanded().toFloatOrNull() ?: 0f
            }
        } else if (answer is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Integer) {
            existingAnswers[linkId] = answer.value.value?.toFloat() ?: 0f
        }
    }

    /**
     * Extracts date.
     * @param linkId The linkId.
     * @param answer The answer.
     * @param existingAnswers The existingAnswers.
     */
    private fun extractDate(
        linkId: String,
        answer: com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value,
        existingAnswers: MutableMap<String, Any>,
    ) {
        if (answer is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Date) {
            answer.value.value?.let { existingAnswers[linkId] = it.toString() }
        } else if (answer is com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.DateTime) {
            answer.value.value?.let { existingAnswers[linkId] = it.toString() }
        }
    }
}
