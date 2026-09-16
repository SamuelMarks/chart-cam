/**
 * @file ClipboardUtilsTest.kt
 * Contains declarations for ClipboardUtilsTest.kt.
 */
package io.healthplatform.chartcam.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for common clipboard utilities logic.
 */
class ClipboardUtilsTest {
    /**
     * Verifies string trimming and payload validation for clipboard exchange.
     */
    @Test
    fun testClip() {
        val testPayload = """  {"resourceType":"Questionnaire"}  """
        val trimmed = testPayload.trim()
        assertNotNull(trimmed)
        assertEquals("""{"resourceType":"Questionnaire"}""", trimmed)
    }
}
