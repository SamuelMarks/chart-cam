/**
 * @file QuestionnaireFilePickerJsTest.kt
 * Contains declarations for QuestionnaireFilePickerJsTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.browser.localStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for QuestionnaireFilePicker on JS.
 */
class QuestionnaireFilePickerJsTest {
    /**
     * Tests picking questionnaire file from localStorage on JS.
     */
    @Test
    fun testQuestionnaireFilePicker() =
        runTest {
            val picker = createQuestionnaireFilePicker()
            assertNotNull(picker)

            localStorage.removeItem("chartcam_imported_questionnaire")
            val failRes = picker.pickQuestionnaireFile()
            assertTrue(failRes.isFailure)

            localStorage.setItem("chartcam_imported_questionnaire", "{\"resourceType\":\"Questionnaire\"}")
            val successRes = picker.pickQuestionnaireFile()
            assertTrue(successRes.isSuccess)
            assertEquals("{\"resourceType\":\"Questionnaire\"}", successRes.getOrNull())
            localStorage.removeItem("chartcam_imported_questionnaire")
        }
}
