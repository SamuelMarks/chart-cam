/**
 * @file TestInstant.kt
 * Contains declarations for TestInstant.kt.
 */
package io.healthplatform.chartcam
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A simple test function to verify that time utility conversion is functioning.
 *
 * @param millis The epoch time in milliseconds.
 */
fun test(millis: Long) {
    val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
    instant.toLocalDateTime(TimeZone.UTC).date
}
