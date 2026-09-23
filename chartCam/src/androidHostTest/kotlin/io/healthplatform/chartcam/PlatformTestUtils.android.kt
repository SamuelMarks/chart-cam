/**
 * @file PlatformTestUtils.android.kt
 * Contains declarations for PlatformTestUtils.android.kt.
 */
package io.healthplatform.chartcam

/**
 * Cleans up the test environment for Android host tests.
 */
actual fun cleanupTestEnv() {
    // No-op for now
}

/**
 * Creates a test CalendarLocale for the specified language tag without calling deprecated constructors.
 *
 * @param languageTag The BCP 47 language tag (e.g. "en").
 * @return A [androidx.compose.material3.CalendarLocale] instance for the platform.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
actual fun createTestCalendarLocale(languageTag: String): androidx.compose.material3.CalendarLocale =
    java.util.Locale.forLanguageTag(languageTag)
