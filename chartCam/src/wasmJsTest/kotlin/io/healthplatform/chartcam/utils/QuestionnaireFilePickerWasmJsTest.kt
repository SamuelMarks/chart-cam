/**
 * @file QuestionnaireFilePickerWasmJsTest.kt
 * Contains declarations for QuestionnaireFilePickerWasmJsTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.browser.localStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@JsFun("() => { window._chartcam_is_test = true; }")
private external fun setChartCamTest()

/**
 * Test class for QuestionnaireFilePicker on WasmJS.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
class QuestionnaireFilePickerWasmJsTest {
    /**
     * Tests picking questionnaire file from localStorage.
     */
    @Test
    fun testQuestionnaireFilePicker() =
        runTest {
            setChartCamTest()
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
