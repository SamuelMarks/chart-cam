/**
 * @file PlatformTestUtils.jvm.kt
 * Contains declarations for PlatformTestUtils.jvm.kt.
 */
package io.healthplatform.chartcam

import java.io.File
import java.util.prefs.Preferences

/**
 * Cleans up the test environment for JVM.
 */
actual fun cleanupTestEnv() {
    System.setProperty("chartcam.isTest", "true")
    File("chartcam_desktop.db").delete()
    val prefs = Preferences.userRoot().node("io.healthplatform.chartcam.secure")
    prefs.clear()
    prefs.flush()
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
