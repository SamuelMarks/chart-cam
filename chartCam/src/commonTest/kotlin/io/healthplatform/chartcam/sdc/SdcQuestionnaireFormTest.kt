/**
 * @file SdcQuestionnaireFormTest.kt
 * Contains declarations for SdcQuestionnaireFormTest.kt.
 */
package io.healthplatform.chartcam.sdc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Common tests for the SDC Questionnaire form functionality.
 */
class SdcQuestionnaireFormTest {
    /**
     * Verifies SdcFormConfig defaults and SdcFormState initialization.
     */
    @Test
    fun testFormStateAndConfig() {
        val config = SdcFormConfig(readOnly = false, showValidationErrors = true)
        assertFalse(config.readOnly)
        assertTrue(config.showValidationErrors)

        val answers = mapOf("q1" to "answer1")
        val touched = setOf("q1")
        val state = SdcFormState(answers = answers, touchedFields = touched, config = config)

        assertEquals("answer1", state.answers["q1"])
        assertTrue(state.touchedFields.contains("q1"))
    }
}
