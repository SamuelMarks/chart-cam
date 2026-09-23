/**
 * @file PlatformTestUtils.kt
 * Contains declarations for PlatformTestUtils.kt.
 */
package io.healthplatform.chartcam

/**
 * Cleans up the test environment.
 * Implementation is expected to be provided by each target platform.
 */
expect fun cleanupTestEnv()

/**
 * Creates a test CalendarLocale for the specified language tag without calling deprecated constructors.
 *
 * @param languageTag The BCP 47 language tag (e.g. "en").
 * @return A [androidx.compose.material3.CalendarLocale] instance for the platform.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
expect fun createTestCalendarLocale(languageTag: String): androidx.compose.material3.CalendarLocale
