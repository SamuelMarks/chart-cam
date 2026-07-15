/**
 * @file DateFormatter.wasmJs.kt
 * @file DateFormatter.wasmJs.kt
 * Contains declarations for DateFormatter.wasmJs.kt.
 */
package io.healthplatform.chartcam.utils

/**
 * Formats a FHIR date or datetime string into a localized, human-readable format on the WasmJS platform.
 *
 * @param fhirDate The date string in FHIR standard format (e.g., ISO 8601).
 * @return The localized date string, or the original [fhirDate] if parsing fails.
 */
actual fun formatLocalizedDate(fhirDate: String): String {
    // Basic fallback for WasmJs without JS Date bindings for now
    return fhirDate
}
