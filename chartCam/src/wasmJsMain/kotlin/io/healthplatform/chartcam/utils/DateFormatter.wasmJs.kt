/**
 * @file DateFormatter.wasmJs.kt
 * Contains declarations for DateFormatter.wasmJs.kt.
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.healthplatform.chartcam.utils

import kotlin.js.toJsString

private const val FORMAT_DATE_JS =
    "(fhirDate, language) => { " +
        "try { " +
        "  const d = new Date(fhirDate); " +
        "  if (isNaN(d.getTime())) return fhirDate; " +
        "  if (fhirDate.indexOf('T') !== -1) { " +
        "    return d.toLocaleString(language, { dateStyle: 'medium', timeStyle: 'short' }); " +
        "  } else { " +
        "    return d.toLocaleDateString(language, { dateStyle: 'medium' }); " +
        "  } " +
        "} catch (e) { " +
        "  return fhirDate; " +
        "} " +
        "}"

/**
 * Formats date using browser Intl API.
 *
 * @param fhirDate Date string in JS.
 * @param language BCP-47 tag in JS.
 * @return Localized date string in JS.
 */
@JsFun(FORMAT_DATE_JS)
private external fun formatLocalizedDateJs(
    fhirDate: JsAny,
    language: JsAny,
): JsAny

/**
 * Formats a FHIR date or datetime string into a localized, human-readable format on the WasmJS platform.
 *
 * @param fhirDate The date string in FHIR standard format (e.g., ISO 8601).
 * @param language The BCP-47 language tag to format the date with.
 * @return The localized date string, or the original [fhirDate] if parsing fails.
 */
actual fun formatLocalizedDate(
    fhirDate: String,
    language: String,
): String {
    if (fhirDate.isBlank()) return fhirDate
    val jsResult = formatLocalizedDateJs(fhirDate.toJsString(), language.toJsString())
    return jsResult.toString()
}
