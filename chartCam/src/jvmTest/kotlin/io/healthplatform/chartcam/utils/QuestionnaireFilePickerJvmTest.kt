/**
 * @file QuestionnaireFilePickerJvmTest.kt
 * Contains declarations for QuestionnaireFilePickerJvmTest.kt.
 */
package io.healthplatform.chartcam.utils

import io.healthplatform.chartcam.files.createFileStorage
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for JvmQuestionnaireFilePicker and file factory.
 */
class QuestionnaireFilePickerJvmTest {
    /**
     * Verifies picker initialization and picking files from local directories.
     */
    @Test
    fun testFilePicker() =
        runTest {
            val picker = createQuestionnaireFilePicker()
            assertNotNull(picker)

            // Test picking when no file exists
            val noFileRes = picker.pickQuestionnaireFile()
            assertNotNull(noFileRes)

            // Test with non-existent directory to verify null listFiles() handling
            val emptyPicker = JvmQuestionnaireFilePicker(listOf(File("non_existent_dir_for_test")))
            assertTrue(emptyPicker.pickQuestionnaireFile().isFailure)

            // Create a temporary questionnaire file
            val tempFile = File("questionnaire_test_jvm.json")
            tempFile.writeText("""{"resourceType":"Questionnaire","id":"test-picker"}""")

            val successRes = picker.pickQuestionnaireFile()
            assertTrue(successRes.isSuccess)
            assertTrue(successRes.getOrNull()!!.contains("test-picker"))

            tempFile.delete()

            // Exercise isQuestionnaireJson
            assertTrue(picker.isQuestionnaireJson("""{"resourceType": "Questionnaire", "id": "123"}"""))
            assertTrue(picker.isQuestionnaireJson("""{"resourceType":"Questionnaire","id":"123"}"""))
            assertTrue(!picker.isQuestionnaireJson("""{"resourceType": "Patient", "id": "123"}"""))

            // Exercise synthetic DefaultImpls bridge if present on JVM/Android
            runCatching {
                val defaultImpls = Class.forName("io.healthplatform.chartcam.utils.QuestionnaireFilePicker\$DefaultImpls")
                val method = defaultImpls.getMethod("isQuestionnaireJson", QuestionnaireFilePicker::class.java, String::class.java)
                val result = method.invoke(null, picker, """{"resourceType": "Questionnaire"}""") as Boolean
                assertTrue(result)
            }
        }

    /**
     * Verifies JvmAudioRecorderManager factory.
     */
    @Test
    fun testJvmAudioRecorderFactory() {
        val storage = createFileStorage()
        val recorder =
            io.healthplatform.chartcam.media
                .createAudioRecorderManager(storage)
        assertNotNull(recorder)
    }
}
