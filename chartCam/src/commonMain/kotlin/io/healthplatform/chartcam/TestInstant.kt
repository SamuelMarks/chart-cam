package io.healthplatform.chartcam
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun test(millis: Long) {
    val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
    instant.toLocalDateTime(TimeZone.UTC).date
}
