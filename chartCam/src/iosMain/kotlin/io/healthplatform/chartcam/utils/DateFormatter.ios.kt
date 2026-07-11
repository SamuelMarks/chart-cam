package io.healthplatform.chartcam.utils

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun formatLocalizedDate(fhirDate: String): String {
    if (fhirDate.isBlank()) return fhirDate
    return try {
        val isoFormatter = NSISO8601DateFormatter()
        val date =
            isoFormatter.dateFromString(fhirDate)
                ?: NSISO8601DateFormatter().apply { formatOptions = 2048UL }.dateFromString(fhirDate)

        if (date != null) {
            val formatter =
                NSDateFormatter().apply {
                    locale = NSLocale.currentLocale
                    dateStyle = NSDateFormatterMediumStyle
                    timeStyle = if (fhirDate.contains("T")) NSDateFormatterMediumStyle else 0UL
                }
            formatter.stringFromDate(date)
        } else {
            fhirDate
        }
    } catch (e: Exception) {
        fhirDate
    }
}
