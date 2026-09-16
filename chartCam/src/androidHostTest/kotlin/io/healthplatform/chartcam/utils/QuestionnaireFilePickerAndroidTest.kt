/**
 * @file QuestionnaireFilePickerAndroidTest.kt
 * Contains declarations for QuestionnaireFilePickerAndroidTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android host tests for AndroidQuestionnaireFilePicker, AndroidKeystoreHardwareProvider, and AndroidAudioRecorderManager.
 */
@RunWith(RobolectricTestRunner::class)
class QuestionnaireFilePickerAndroidTest {
    /**
     * Verifies Android file picker initialization and file reading.
     */
    @Test
    fun testAndroidFilePicker() =
        runTest {
            val picker = createQuestionnaireFilePicker()
            assertNotNull(picker)

            val noFileRes = picker.pickQuestionnaireFile()
            assertNotNull(noFileRes)

            val emptyPicker = AndroidQuestionnaireFilePicker(listOf(File("/non_existent_folder_for_test")))
            assertTrue(emptyPicker.pickQuestionnaireFile().isFailure)

            val tempFile = File("questionnaire_test_android.json")
            tempFile.writeText("""{"resourceType":"Questionnaire","id":"test-android-picker"}""")

            val successRes = picker.pickQuestionnaireFile()
            assertTrue(successRes.isSuccess)
            assertTrue(successRes.getOrNull()!!.contains("test-android-picker"))

            tempFile.delete()
        }

    /**
     * Verifies AndroidKeystoreHardwareProvider creation and status.
     */
    @Test
    fun testAndroidKeystoreHardwareProvider() {
        val provider =
            io.healthplatform.chartcam.storage
                .createKeystoreHardwareProvider()
        assertNotNull(provider)
        val status = provider.getHardwareStatus()
        assertNotNull(status)
        val check = provider.checkHardwareBacked()
        assertNotNull(check)
        // Under default Robolectric SDK 21 (< M), provider returns failure and NO_HARDWARE
        assertTrue(check.isFailure)
        kotlin.test.assertEquals(io.healthplatform.chartcam.storage.BiometricHardwareStatus.NO_HARDWARE, status)

        val modernProvider =
            io.healthplatform.chartcam.storage
                .AndroidKeystoreHardwareProvider(sdkInt = 28)
        kotlin.test.assertEquals(io.healthplatform.chartcam.storage.BiometricHardwareStatus.AVAILABLE, modernProvider.getHardwareStatus())
        assertTrue(modernProvider.checkHardwareBacked().isSuccess)
    }

    /**
     * Verifies AndroidAudioRecorderManager factory.
     */
    @Test
    fun testAndroidAudioRecorderFactory() {
        val mockStorage =
            object : io.healthplatform.chartcam.files.FileStorage {
                override fun saveImage(
                    fileName: String,
                    bytes: ByteArray,
                ): String = fileName

                override fun readImage(path: String): ByteArray = ByteArray(0)

                override fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

                override fun clearCache() {}
            }
        val recorder =
            io.healthplatform.chartcam.media
                .createAudioRecorderManager(mockStorage)
        assertNotNull(recorder)
    }
}
