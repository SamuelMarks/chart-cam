package io.healthplatform.chartcam.utils

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

actual fun formatLocalizedDate(fhirDate: String): String {
    if (fhirDate.isBlank()) return fhirDate
    return try {
        if (fhirDate.contains("T")) {
            val dateTime = ZonedDateTime.parse(fhirDate)
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
            dateTime.format(formatter)
        } else {
            val date = LocalDate.parse(fhirDate)
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
            date.format(formatter)
        }
    } catch (e: Exception) {
        fhirDate
    }
}
