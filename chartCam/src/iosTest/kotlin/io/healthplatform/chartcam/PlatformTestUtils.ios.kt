/**
 * @file PlatformTestUtils.ios.kt
 * Contains declarations for PlatformTestUtils.ios.kt.
 */
package io.healthplatform.chartcam

/**
 * Cleans up the test environment for the iOS target.
 */
actual fun cleanupTestEnv() {
    // No-op
}

/**
 * Creates a test CalendarLocale for the specified language tag without calling deprecated constructors.
 *
 * @param languageTag The BCP 47 language tag (e.g. "en").
 * @return A [androidx.compose.material3.CalendarLocale] instance for the platform.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
actual fun createTestCalendarLocale(languageTag: String): androidx.compose.material3.CalendarLocale =
    androidx.compose.material3.CalendarLocale(languageTag)
