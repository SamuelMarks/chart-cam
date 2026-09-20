/**
 * @file SdcObservationExtractor.kt
 * Extracts structured FHIR Observation resources from completed SDC QuestionnaireResponses.
 */
package io.healthplatform.chartcam.sdc

import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.DateTime
import dev.ohs.fhir.model.r4.Decimal
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Quantity
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Uri
import io.healthplatform.chartcam.utils.UUID
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Context holder for questionnaire observation extraction passes.
 *
 * @property questionMap Map of question definitions indexed by linkId.
 * @property cleanPatientId Sanitized patient resource identifier.
 * @property cleanEncounterId Sanitized encounter identifier, or null.
 * @property nowIso Timestamp for the extraction event in ISO-8601 format.
 * @property observations Accumulator list of generated Observation resources.
 */
private data class ExtractionContext(
    val questionMap: Map<String, Questionnaire.Item>,
    val cleanPatientId: String,
    val cleanEncounterId: String?,
    val nowIso: String,
    val observations: MutableList<Observation>,
)

/**
 * Offline extractor engine transforming completed SDC forms into standard FHIR Observation resources.
 */
object SdcObservationExtractor {
    /** LOINC standard code for Pain Severity score. */
    const val LOINC_PAIN_SCORE = "72514-3"

    /** LOINC standard code for Fitzpatrick skin classification. */
    const val LOINC_FITZPATRICK = "87474-3"

    /**
     * Extracts FHIR [Observation] resources from a completed [QuestionnaireResponse] and its template [Questionnaire].
     *
     * @param questionnaire The template [Questionnaire] defining questions and codings.
     * @param response The completed [QuestionnaireResponse].
     * @param patientId The target patient identifier.
     * @param encounterId Optional linked encounter identifier.
     * @return A [Result] enclosing the list of generated [Observation] resources.
     */
    fun extractObservations(
        questionnaire: Questionnaire,
        response: QuestionnaireResponse,
        patientId: String,
        encounterId: String? = null,
    ): Result<List<Observation>> =
        runCatching {
            val observations = mutableListOf<Observation>()
            val questionMap = mutableMapOf<String, Questionnaire.Item>()
            indexQuestionnaireItems(questionnaire.item, questionMap)

            val ctx =
                ExtractionContext(
                    questionMap = questionMap,
                    cleanPatientId = patientId.removePrefix("Patient/"),
                    cleanEncounterId = encounterId?.removePrefix("Encounter/"),
                    nowIso =
                        kotlin.time.Clock.System
                            .now()
                            .toString(),
                    observations = observations,
                )

            processResponseItems(response.item, ctx)
            observations
        }

    /**
     * Recursively indexes template questionnaire items by their linkId.
     *
     * @param items The list of questionnaire items.
     * @param map The destination map.
     */
    private fun indexQuestionnaireItems(
        items: List<Questionnaire.Item>,
        map: MutableMap<String, Questionnaire.Item>,
    ) {
        for (item in items) {
            val linkId = item.linkId.value ?: item.linkId.id
            if (!linkId.isNullOrBlank()) {
                map[linkId] = item
            }
            indexQuestionnaireItems(item.item, map)
        }
    }

    /**
     * Traverses questionnaire response items and constructs corresponding Observation resources.
     *
     * @param responseItems The list of response items.
     * @param ctx The shared extraction context.
     */
    private fun processResponseItems(
        responseItems: List<QuestionnaireResponse.Item>,
        ctx: ExtractionContext,
    ) {
        for (rItem in responseItems) {
            val linkId = rItem.linkId.value ?: rItem.linkId.id ?: ""
            val qItem = ctx.questionMap[linkId]
            processAnswers(rItem, linkId, qItem, ctx)
            processResponseItems(rItem.item, ctx)
        }
    }

    /**
     * Processes answers for an item and adds generated Observation records to the context.
     *
     * @param rItem The response item.
     * @param linkId The linkId.
     * @param qItem The template item, or null.
     * @param ctx The extraction context.
     */
    private fun processAnswers(
        rItem: QuestionnaireResponse.Item,
        linkId: String,
        qItem: Questionnaire.Item?,
        ctx: ExtractionContext,
    ) {
        if (qItem != null && !shouldExtractObservation(qItem, linkId)) return
        for (answer in rItem.answer) {
            val obsValue = mapAnswerToObservationValue(answer.value) ?: continue
            val obsCode = resolveObservationCode(linkId, qItem)
            val encRef =
                ctx.cleanEncounterId?.let {
                    Reference(reference = FhirString(value = "Encounter/$it"))
                }
            val obs =
                Observation(
                    id = UUID.randomUUID(),
                    status = Enumeration(value = Observation.ObservationStatus.Final),
                    code = obsCode,
                    subject = Reference(reference = FhirString(value = "Patient/${ctx.cleanPatientId}")),
                    encounter = encRef,
                    effective =
                        Observation.Effective.DateTime(
                            DateTime(value = FhirDateTime.fromString(ctx.nowIso)),
                        ),
                    value = obsValue,
                )
            ctx.observations.add(obs)
        }
    }

    /**
     * Determines whether an observation should be extracted for this item based on SDC extensions or definitions.
     *
     * @param qItem The template questionnaire item.
     * @param linkId The linkId of the question.
     * @return True if observation extraction is indicated.
     */
    private fun shouldExtractObservation(qItem: Questionnaire.Item, linkId: String): Boolean {
        val hasExtractExtension =
            qItem.extension.any { ext ->
                if (ext.url != io.healthplatform.chartcam.fhir.SdcExtensions.OBSERVATION_EXTRACT) {
                    false
                } else {
                    val extVal = ext.value
                    if (extVal is dev.ohs.fhir.model.r4.Extension.Value.Boolean) {
                        extVal.value.value != false
                    } else {
                        true
                    }
                }
            }
        val hasDefinition = !qItem.definition?.value.isNullOrBlank()
        val hasCodes = qItem.code.isNotEmpty()
        val isStandardMetric =
            linkId.lowercase().let { lower ->
                lower.contains("pain") || lower.contains("fitzpatrick") || lower.contains("skin")
            }
        return hasExtractExtension || hasDefinition || hasCodes || isStandardMetric
    }

    /**
     * Resolves the observation code from declared item coding or definition URI.
     *
     * @param qItem The questionnaire item template.
     * @return The populated [CodeableConcept], or null if neither code nor definition are present.
     */
    private fun resolveItemCodeOrDefinition(qItem: Questionnaire.Item): CodeableConcept? {
        if (qItem.code.isNotEmpty()) {
            return CodeableConcept(coding = qItem.code, text = qItem.text)
        }
        val defUri = qItem.definition?.value
        return if (!defUri.isNullOrBlank()) {
            val codeVal = defUri.substringAfterLast('#').substringAfterLast('/')
            val sys = if (defUri.contains("loinc.org")) "http://loinc.org" else defUri.substringBeforeLast('/')
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            system = Uri(value = sys),
                            code =
                                dev.ohs.fhir.model.r4
                                    .Code(value = codeVal),
                            display = qItem.text,
                        ),
                    ),
                text = qItem.text,
            )
        } else {
            null
        }
    }

    /**
     * Resolves the appropriate CodeableConcept for the extracted observation.
     *
     * @param linkId The question linkId.
     * @param qItem The corresponding question template item.
     * @return The populated [CodeableConcept].
     */
    private fun resolveObservationCode(
        linkId: String,
        qItem: Questionnaire.Item?,
    ): CodeableConcept =
        qItem?.let { resolveItemCodeOrDefinition(it) } ?: resolveFallbackObservationCode(linkId, qItem)

    /**
     * Resolves fallback observation code based on standard linkId patterns.
     *
     * @param linkId The question linkId.
     * @param qItem The corresponding question template item.
     * @return The fallback [CodeableConcept].
     */
    private fun resolveFallbackObservationCode(
        linkId: String,
        qItem: Questionnaire.Item?,
    ): CodeableConcept {
        val lower = linkId.lowercase()
        return when {
            lower.contains("pain") ->
                CodeableConcept(
                    coding =
                        listOf(
                            Coding(
                                system = Uri(value = "http://loinc.org"),
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code(value = LOINC_PAIN_SCORE),
                                display = FhirString(value = "Pain severity - 0-10 verbal numeric rating score"),
                            ),
                        ),
                    text = FhirString(value = "Pain Score"),
                )
            lower.contains("fitzpatrick") || lower.contains("skin") ->
                CodeableConcept(
                    coding =
                        listOf(
                            Coding(
                                system = Uri(value = "http://loinc.org"),
                                code =
                                    dev.ohs.fhir.model.r4
                                        .Code(value = LOINC_FITZPATRICK),
                                display = FhirString(value = "Fitzpatrick skin type"),
                            ),
                        ),
                    text = FhirString(value = "Fitzpatrick Skin Type"),
                )
            else ->
                CodeableConcept(
                    text = qItem?.text ?: FhirString(value = linkId),
                )
        }
    }

    /**
     * Maps a QuestionnaireResponse answer value to an Observation.Value.
     *
     * @param value The response item answer value.
     * @return The mapped [Observation.Value], or null if unsupported.
     */
    private fun mapAnswerToObservationValue(value: QuestionnaireResponse.Item.Answer.Value?): Observation.Value? =
        when (value) {
            is QuestionnaireResponse.Item.Answer.Value.Decimal ->
                Observation.Value.Quantity(
                    Quantity(
                        value = Decimal(value = value.value.value),
                    ),
                )
            is QuestionnaireResponse.Item.Answer.Value.Integer ->
                Observation.Value.Integer(value = value.value)
            is QuestionnaireResponse.Item.Answer.Value.String ->
                Observation.Value.String(value = value.value)
            is QuestionnaireResponse.Item.Answer.Value.Boolean ->
                Observation.Value.Boolean(value = value.value)
            is QuestionnaireResponse.Item.Answer.Value.Coding ->
                Observation.Value.CodeableConcept(
                    value = CodeableConcept(coding = listOf(value.value)),
                )
            is QuestionnaireResponse.Item.Answer.Value.Date ->
                runCatching {
                    Observation.Value.DateTime(
                        value = DateTime(value = FhirDateTime.fromString(value.value.value.toString())),
                    )
                }.getOrNull()
            is QuestionnaireResponse.Item.Answer.Value.DateTime ->
                Observation.Value.DateTime(value = value.value)
            is QuestionnaireResponse.Item.Answer.Value.Quantity ->
                Observation.Value.Quantity(value = value.value)
            else -> null
        }
}
