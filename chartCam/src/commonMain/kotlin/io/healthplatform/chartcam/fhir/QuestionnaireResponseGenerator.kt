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
            questionnaire.item.mapNotNull { item ->
                createResponseItemBuilder(item, answers)
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
     * Creates a builder for a [QuestionnaireResponse.Item] from a given [Questionnaire.Item] and answers map.
     *
     * @param item The questionnaire item.
     * @param answers The map of answers.
     * @return A builder for the questionnaire response item, or null if it cannot be built.
     */
    private fun createResponseItemBuilder(
        item: Questionnaire.Item,
        answers: Map<kotlin.String, Any>,
    ): QuestionnaireResponse.Item.Builder? {
        val linkId = item.linkId.value ?: return null
        val answerValue = answers[linkId]
        val nestedItemBuilders = item.item.mapNotNull { createResponseItemBuilder(it, answers) }

        return if (answerValue == null && nestedItemBuilders.isEmpty()) {
            null
        } else {
            QuestionnaireResponse.Item
                .Builder(
                    linkId = String.Builder().apply { value = linkId },
                ).apply {
                    text = String.Builder().apply { value = item.text?.value ?: "" }
                    if (answerValue != null) {
                        populateAnswers(item, answerValue, this)
                    }
                    if (nestedItemBuilders.isNotEmpty()) {
                        this.item.addAll(nestedItemBuilders)
                    }
                }
        }
    }

    /**
     * Helper function for processing questionnaire answers.
     * @param item The item.
     * @param answerValue The answerValue.
     * @param builder The builder.
     */
    private fun populateAnswers(
        item: Questionnaire.Item,
        answerValue: Any,
        builder: QuestionnaireResponse.Item.Builder,
    ) {
        when (item.type?.value) {
            Questionnaire.QuestionnaireItemType.String, Questionnaire.QuestionnaireItemType.Text -> {
                addStringAnswer(builder, answerValue as? kotlin.String ?: "")
            }
            Questionnaire.QuestionnaireItemType.Boolean -> {
                addBooleanAnswer(builder, answerValue as? kotlin.Boolean ?: false)
            }
            Questionnaire.QuestionnaireItemType.Decimal -> {
                addDecimalAnswer(builder, answerValue)
            }
            Questionnaire.QuestionnaireItemType.Integer -> {
                addIntegerAnswer(builder, answerValue)
            }
            Questionnaire.QuestionnaireItemType.Date -> {
                addDateAnswer(builder, answerValue as? kotlin.String ?: "")
            }
            Questionnaire.QuestionnaireItemType.DateTime -> {
                addDateTimeAnswer(builder, answerValue as? kotlin.String ?: "")
            }
            Questionnaire.QuestionnaireItemType.Choice -> {
                addChoiceAnswer(builder, answerValue)
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
        val fl = (answerValue as? Float) ?: (answerValue as? kotlin.String)?.toFloatOrNull()
        if (fl != null) {
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
        val intVal = (answerValue as? Float)?.toInt() ?: (answerValue as? kotlin.String)?.toIntOrNull()
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
