/**
 * @file FormValidator.kt
 * Contains validation logic for form inputs.
 */
package io.healthplatform.chartcam.validation

import com.google.fhir.model.r4.Questionnaire

/**
 * Validates form answers against [Questionnaire.Item] constraints.
 */
object FormValidator {
    /**
     * Validates a string answer against the required constraints.
     *
     * @param answer The user's answer.
     * @param required Whether the field is mandatory.
     * @param regex Optional regular expression the answer must match.
     * @return True if valid, false otherwise.
     */
    fun validateText(
        answer: String?,
        required: Boolean = false,
        regex: String? = null,
    ): Boolean =
        if (answer.isNullOrBlank()) {
            !required
        } else if (regex != null) {
            Regex(regex).matches(answer)
        } else {
            true
        }

    /**
     * Validates a numeric answer against range constraints.
     *
     * @param answer The numeric answer.
     * @param required Whether the field is mandatory.
     * @param min The minimum acceptable value (inclusive).
     * @param max The maximum acceptable value (inclusive).
     * @return True if valid, false otherwise.
     */
    fun validateNumber(
        answer: Double?,
        required: Boolean = false,
        min: Double? = null,
        max: Double? = null,
    ): Boolean =
        if (answer == null) {
            !required
        } else {
            val passesMin = min == null || answer >= min
            val passesMax = max == null || answer <= max
            passesMin && passesMax
        }
}
