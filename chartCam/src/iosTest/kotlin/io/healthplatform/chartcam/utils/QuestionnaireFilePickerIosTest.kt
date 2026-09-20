/**
 * @file QuestionnaireFilePickerIosTest.kt
 * Contains declarations for QuestionnaireFilePickerIosTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for QuestionnaireFilePicker on iOS.
 */
class QuestionnaireFilePickerIosTest {
    /**
     * Tests questionnaire file picker invocation on iOS.
     */
    @Test
    fun testFilePickerInvocation() =
        runTest {
            val picker = createQuestionnaireFilePicker()
            assertNotNull(picker)
            val res = picker.pickQuestionnaireFile()
            assertNotNull(res)
        }
}
