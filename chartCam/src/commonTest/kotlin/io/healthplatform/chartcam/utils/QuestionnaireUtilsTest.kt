/**
 * @file QuestionnaireUtilsTest.kt
 * Contains unit tests for QuestionnaireUtils helper functions.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests verifying QuestionnaireUtils behavior.
 */
class QuestionnaireUtilsTest {
    /**
     * Tests stripNarrativeDiv with null and empty inputs.
     */
    @Test
    fun testStripNarrativeDiv_nullAndEmpty() {
        assertNull(QuestionnaireUtils.stripNarrativeDiv(null))
        assertEquals("", QuestionnaireUtils.stripNarrativeDiv(""))
        assertEquals("", QuestionnaireUtils.stripNarrativeDiv("   "))
    }

    /**
     * Tests stripNarrativeDiv with standard div tags.
     */
    @Test
    fun testStripNarrativeDiv_standardDiv() {
        val input = "<div>Patient examination completed.</div>"
        assertEquals("Patient examination completed.", QuestionnaireUtils.stripNarrativeDiv(input))
    }

    /**
     * Tests stripNarrativeDiv with FHIR XHTML namespaced div.
     */
    @Test
    fun testStripNarrativeDiv_namespacedDiv() {
        val input = """<div xmlns="http://www.w3.org/1999/xhtml">Follow-up in 2 weeks.</div>"""
        assertEquals("Follow-up in 2 weeks.", QuestionnaireUtils.stripNarrativeDiv(input))
    }

    /**
     * Tests stripNarrativeDiv with nested tags.
     */
    @Test
    fun testStripNarrativeDiv_nestedContent() {
        val input = """<div xmlns="http://www.w3.org/1999/xhtml"><p>Vitals normal.</p></div>"""
        assertEquals("<p>Vitals normal.</p>", QuestionnaireUtils.stripNarrativeDiv(input))
    }

    /**
     * Tests stripNarrativeDiv when no outer div is present.
     */
    @Test
    fun testStripNarrativeDiv_plainText() {
        val input = "Just plain clinical notes."
        assertEquals("Just plain clinical notes.", QuestionnaireUtils.stripNarrativeDiv(input))
    }
}
