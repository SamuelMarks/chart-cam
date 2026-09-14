/**
 * @file ImportFilter.kt
 * Models and configuration options for filtering resources during FHIR bundle import.
 */
package io.healthplatform.chartcam.models

/**
 * Categories of FHIR resources that can be selectively imported.
 */
enum class ImportCategory {
    /** Practitioner clinical identities and accounts. */
    PRACTITIONERS,

    /** Patient demographics and core identities. */
    PATIENTS,

    /** Clinical visits, appointments, and encounters. */
    ENCOUNTERS,

    /** Questionnaire responses and practitioner clinical notes. */
    CLINICAL_NOTES,

    /** Binary photo captures and document attachments. */
    BINARY_PHOTOS,

    /** Custom clinical forms and questionnaire schemas. */
    QUESTIONNAIRES,
}

/**
 * Filter configuration specifying which categories and practitioner scopes should be ingested.
 *
 * @property enabledCategories The set of active categories permitted to be imported.
 * @property targetPractitionerId Optional practitioner ID to scope patient and encounter ingestion.
 */
data class ImportFilterOptions(
    val enabledCategories: Set<ImportCategory> = ImportCategory.entries.toSet(),
    val targetPractitionerId: String? = null,
) {
    /**
     * Checks if a particular category is enabled in this filter.
     *
     * @param category The category to verify.
     * @return True if the category is enabled, false otherwise.
     */
    fun isCategoryEnabled(category: ImportCategory): Boolean = enabledCategories.contains(category)

    /**
     * Companion object providing preset filter configurations.
     */
    companion object {
        /**
         * Creates an options instance with all categories enabled.
         *
         * @return An [ImportFilterOptions] with all categories active.
         */
        fun all(): ImportFilterOptions = ImportFilterOptions(ImportCategory.entries.toSet())
    }
}
