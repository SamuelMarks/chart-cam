/**
 * @file PlatformTestUtils.jvm.kt
 * Contains declarations for PlatformTestUtils.jvm.kt.
 */
package io.healthplatform.chartcam

import java.io.File
import java.util.prefs.Preferences

actual fun cleanupTestEnv() {
    System.setProperty("chartcam.isTest", "true")
    File("chartcam_desktop.db").delete()
    val prefs = Preferences.userRoot().node("io.healthplatform.chartcam.secure")
    prefs.clear()
    prefs.flush()
}
