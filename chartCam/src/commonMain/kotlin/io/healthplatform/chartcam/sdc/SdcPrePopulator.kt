/**
 * @file SdcPrePopulator.kt
 * Offline pre-population engine extracting patient demographics to initialize SDC questionnaire forms.
 */
package io.healthplatform.chartcam.sdc

import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire

/**
 * Pre-populator engine that initializes Questionnaire form answer maps using local Patient demographic data.
 * Operates purely offline without remote FHIR terminology or external REST dependencies.
 */
object SdcPrePopulator {
    /**
     * Pre-populates question answers from a local [Patient] demographic record wrapped in a [Result].
     *
     * Inspects question linkIds, declared initial values, and SDC extensions to auto-fill
     * common demographic fields like patient name, date of birth, and administrative gender.
     *
     * @param questionnaire The template [Questionnaire] being launched.
     * @param patient The local [Patient] clinical record.
     * @return A [Result] enclosing the map of pre-populated answers keyed by question linkId.
     */
    fun populateCatching(
        questionnaire: Questionnaire,
        patient: Patient,
    ): Result<Map<String, Any>> =
        runCatching {
            val answers = mutableMapOf<String, Any>()
            indexAndPopulateItems(questionnaire.item, patient, answers)
            answers
        }

    /**
     * Recursively traverses questionnaire items and pre-populates matching demographic answers.
     *
     * @param items The list of questionnaire items.
     * @param patient The local patient demographic record.
     * @param answers The output map accumulator.
     */
    private fun indexAndPopulateItems(
        items: List<Questionnaire.Item>,
        patient: Patient,
        answers: MutableMap<String, Any>,
    ) {
        for (item in items) {
            populateSingleItem(item, patient, answers)
            indexAndPopulateItems(item.item, patient, answers)
        }
    }

    /**
     * Pre-populates a single questionnaire item if demographic data or default initial values exist.
     *
     * @param item The questionnaire item.
     * @param patient The local patient demographic record.
     * @param answers The output map accumulator.
     */
    private fun populateSingleItem(
        item: Questionnaire.Item,
        patient: Patient,
        answers: MutableMap<String, Any>,
    ) {
        val linkId = item.linkId.value ?: item.linkId.id
        if (linkId.isNullOrBlank()) return

        val prePopulated = resolveDemographicAnswer(linkId, patient)
        val initialVal = item.initial.firstOrNull()?.let { extractInitialValue(it) }
        val resolved = prePopulated ?: initialVal
        if (resolved != null) {
            answers[linkId] = resolved
        }
    }

    /**
     * Matches a question linkId against patient demographics.
     *
     * @param linkId The question linkId.
     * @param patient The patient resource.
     * @return The demographic answer value, or null if no match.
     */
    private fun resolveDemographicAnswer(
        linkId: String,
        patient: Patient,
    ): Any? {
        val lower = linkId.lowercase()
        return when {
            lower.contains("birth") || lower.contains("dob") -> {
                patient.birthDate?.value?.toString()
            }
            lower.contains("gender") || lower.contains("sex") -> {
                patient.gender?.value?.name
            }
            lower.contains("patient_name") || lower == "name" || lower.contains("fullname") -> {
                val name = patient.name.firstOrNull() ?: return null
                val given = name.given.mapNotNull { it.value }.joinToString(" ")
                val family = name.family?.value.orEmpty()
                "$given $family".trim().ifEmpty { null }
            }
            else -> null
        }
    }

    /**
     * Extracts the raw value from a Questionnaire.Item.Initial element.
     *
     * @param initial The initial value element.
     * @return The raw unwrapped value, or null.
     */
    private fun extractInitialValue(initial: Questionnaire.Item.Initial): Any? =
        when (val v = initial.value) {
            is Questionnaire.Item.Initial.Value.Boolean -> v.value.value
            is Questionnaire.Item.Initial.Value.Decimal -> v.value.value
            is Questionnaire.Item.Initial.Value.Integer -> v.value.value
            is Questionnaire.Item.Initial.Value.Date -> v.value.value?.toString()
            is Questionnaire.Item.Initial.Value.DateTime -> v.value.value?.toString()
            is Questionnaire.Item.Initial.Value.String -> v.value.value
            is Questionnaire.Item.Initial.Value.Uri -> v.value.value
            is Questionnaire.Item.Initial.Value.Coding -> v.value.code?.value
            else -> null
        }
}
