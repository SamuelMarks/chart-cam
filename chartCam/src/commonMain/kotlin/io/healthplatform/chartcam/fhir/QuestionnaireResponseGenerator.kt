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

        if (answerValue == null && nestedItemBuilders.isEmpty()) {
            return null
        }

        val responseItemBuilder =
            QuestionnaireResponse.Item
                .Builder(
                    linkId = String.Builder().apply { value = linkId },
                ).apply {
                    text = String.Builder().apply { value = item.text?.value ?: "" }
                }

        if (answerValue != null) {
            when (item.type?.value) {
                Questionnaire.QuestionnaireItemType.String, Questionnaire.QuestionnaireItemType.Text -> {
                    responseItemBuilder.answer.add(
                        QuestionnaireResponse.Item.Answer.Builder().apply {
                            value =
                                QuestionnaireResponse.Item.Answer.Value.String(
                                    String.Builder().apply { value = answerValue as? kotlin.String ?: "" }.build(),
                                )
                        },
                    )
                }
                Questionnaire.QuestionnaireItemType.Boolean -> {
                    responseItemBuilder.answer.add(
                        QuestionnaireResponse.Item.Answer.Builder().apply {
                            value =
                                QuestionnaireResponse.Item.Answer.Value.Boolean(
                                    Boolean.Builder().apply { value = answerValue as? kotlin.Boolean ?: false }.build(),
                                )
                        },
                    )
                }
                Questionnaire.QuestionnaireItemType.Decimal -> {
                    val fl = (answerValue as? Float) ?: (answerValue as? kotlin.String)?.toFloatOrNull()
                    if (fl != null) {
                        responseItemBuilder.answer.add(
                            QuestionnaireResponse.Item.Answer.Builder().apply {
                                value =
                                    QuestionnaireResponse.Item.Answer.Value.Decimal(
                                        Decimal.Builder().apply { value = BigDecimal.parseString(fl.toString()) }.build(),
                                    )
                            },
                        )
                    }
                }
                Questionnaire.QuestionnaireItemType.Integer -> {
                    val intVal = (answerValue as? Float)?.toInt() ?: (answerValue as? kotlin.String)?.toIntOrNull()
                    if (intVal != null) {
                        responseItemBuilder.answer.add(
                            QuestionnaireResponse.Item.Answer.Builder().apply {
                                value =
                                    QuestionnaireResponse.Item.Answer.Value.Integer(
                                        Integer.Builder().apply { value = intVal }.build(),
                                    )
                            },
                        )
                    }
                }
                Questionnaire.QuestionnaireItemType.Date -> {
                    responseItemBuilder.answer.add(
                        QuestionnaireResponse.Item.Answer.Builder().apply {
                            value =
                                QuestionnaireResponse.Item.Answer.Value.Date(
                                    Date.Builder().apply { value = FhirDate.fromString(answerValue as? kotlin.String ?: "") }.build(),
                                )
                        },
                    )
                }
                Questionnaire.QuestionnaireItemType.DateTime -> {
                    responseItemBuilder.answer.add(
                        QuestionnaireResponse.Item.Answer.Builder().apply {
                            value =
                                QuestionnaireResponse.Item.Answer.Value.DateTime(
                                    DateTime
                                        .Builder()
                                        .apply {
                                            value =
                                                FhirDateTime.fromString(
                                                    answerValue as? kotlin.String ?: "",
                                                )
                                        }.build(),
                                )
                        },
                    )
                }
                Questionnaire.QuestionnaireItemType.Choice -> {
                    if (answerValue is List<*>) {
                        answerValue.filterIsInstance<kotlin.String>().forEach { opt ->
                            responseItemBuilder.answer.add(
                                QuestionnaireResponse.Item.Answer.Builder().apply {
                                    value =
                                        QuestionnaireResponse.Item.Answer.Value.String(
                                            String.Builder().apply { value = opt }.build(),
                                        )
                                },
                            )
                        }
                    } else if (answerValue is kotlin.String) {
                        responseItemBuilder.answer.add(
                            QuestionnaireResponse.Item.Answer.Builder().apply {
                                value =
                                    QuestionnaireResponse.Item.Answer.Value.String(
                                        String.Builder().apply { value = answerValue }.build(),
                                    )
                            },
                        )
                    }
                }
                else -> {}
            }
        }

        if (nestedItemBuilders.isNotEmpty()) {
            responseItemBuilder.item.addAll(nestedItemBuilders)
        }

        return responseItemBuilder
    }
}
