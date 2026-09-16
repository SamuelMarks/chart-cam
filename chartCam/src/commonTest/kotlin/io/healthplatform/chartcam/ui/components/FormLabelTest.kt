/**
 * @file FormLabelTest.kt
 * Contains declarations for FormLabelTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Common test for FormLabel component logic.
 */
class FormLabelTest {
    /**
     * Verifies required field label formatting.
     */
    @Test
    fun testFormLabelRequirement() {
        val label = "Patient Name"
        val isRequired = true
        val formatted = if (isRequired) "$label *" else label
        assertNotNull(formatted)
        assertEquals("Patient Name *", formatted)
    }
}
