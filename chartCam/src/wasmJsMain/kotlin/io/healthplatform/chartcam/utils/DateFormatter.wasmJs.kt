package io.healthplatform.chartcam.utils

actual fun formatLocalizedDate(fhirDate: String): String {
    // Basic fallback for WasmJs without JS Date bindings for now
    return fhirDate
}
