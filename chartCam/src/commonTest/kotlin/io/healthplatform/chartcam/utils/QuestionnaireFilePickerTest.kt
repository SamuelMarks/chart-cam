package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestionnaireFilePickerTest {
    @Test
    fun testFilePickerErrors() {
        val err1 = FilePickerError.UserCancelled
        assertEquals("User cancelled file selection", err1.message)

        val err2 = FilePickerError.ReadFailed
        assertEquals("Failed to read the selected file", err2.message)

        val err3 = FilePickerError.Unsupported
        assertEquals("File picking is not supported on this platform", err3.message)
    }

    @Test
    fun testIsQuestionnaireJson() {
        val picker =
            object : QuestionnaireFilePicker {
                override suspend fun pickQuestionnaireFile(): Result<String> = Result.success("{}")
            }

        assertTrue(picker.isQuestionnaireJson("{\"resourceType\": \"Questionnaire\"}"))
        assertTrue(picker.isQuestionnaireJson("{\"resourceType\":\"Questionnaire\"}"))
        assertFalse(picker.isQuestionnaireJson("{\"resourceType\": \"Patient\"}"))
        assertFalse(picker.isQuestionnaireJson("invalid json"))
    }
}
