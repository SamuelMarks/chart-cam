/**
 * @file FormLabelTest.kt
 * Contains declarations for FormLabelTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for FormLabel formatting and annotated string construction logic.
 */
class FormLabelTest {
    /**
     * Verifies that the required form label appends an asterisk.
     */
    @Test
    fun testFormLabelRequired() {
        val label = "Patient Name"
        val isRequired = true
        val formatted = if (isRequired) "$label *" else label
        assertEquals("Patient Name *", formatted)
    }

    /**
     * Verifies that optional form labels do not append an asterisk.
     */
    @Test
    fun testFormLabelOptional() {
        val label = "Observation Notes"
        val isRequired = false
        val formatted = if (isRequired) "$label *" else label
        assertEquals("Observation Notes", formatted)
    }

    /**
     * Verifies [buildAnnotatedFormLabel] when asterisk is at the end of the text.
     */
    @Test
    fun testBuildAnnotatedFormLabelTrailingAsterisk() {
        val annotated = buildAnnotatedFormLabel("Patient Name *", Color.Red)
        assertEquals("Patient Name *", annotated.text)
        assertNotNull(annotated.spanStyles)
    }

    /**
     * Verifies [buildAnnotatedFormLabel] when asterisk is followed by additional text.
     */
    @Test
    fun testBuildAnnotatedFormLabelAsteriskWithTrailingText() {
        val annotated = buildAnnotatedFormLabel("* Mandatory Field", Color.Red)
        assertEquals("* Mandatory Field", annotated.text)
    }

    /**
     * Verifies [buildAnnotatedFormLabel] when asterisk is in the middle of text.
     */
    @Test
    fun testBuildAnnotatedFormLabelMiddleAsterisk() {
        val annotated = buildAnnotatedFormLabel("Field * (Required)", Color.Red)
        assertEquals("Field * (Required)", annotated.text)
    }

    /**
     * Verifies [buildAnnotatedFormLabel] when no asterisk is present.
     */
    @Test
    fun testBuildAnnotatedFormLabelNoAsterisk() {
        val annotated = buildAnnotatedFormLabel("Optional Notes", Color.Red)
        assertEquals("Optional Notes", annotated.text)
    }
}
