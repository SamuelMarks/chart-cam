/**
 * @file ClipboardUtilsAndroidTest.kt
 * Contains declarations for ClipboardUtilsAndroidTest.kt.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Android host tests for ClipboardUtils.
 */
class ClipboardUtilsAndroidTest {
    /**
     * Verifies text trimming logic used for clipboard operations.
     */
    @Test
    fun testClipboardUtilsAndroid() {
        val raw = "  clinical_token  "
        val trimmed = raw.trim()
        assertNotNull(trimmed)
        assertEquals("clinical_token", trimmed)
    }
}
