package io.healthplatform.chartcam.utils

/**
 * Formats a FHIR Date or DateTime string according to the user's current locale.
 *
 * @param fhirDate The raw FHIR date string (e.g., "1990-01-01" or "1990-01-01T10:00:00Z").
 * @return The locale-formatted date string.
 */
expect fun formatLocalizedDate(fhirDate: String): String
