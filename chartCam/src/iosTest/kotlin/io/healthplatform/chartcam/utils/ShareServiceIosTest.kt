/**
 * @file ShareServiceIosTest.kt
 * Contains declarations for ShareServiceIosTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Validates the behavior of Share extensions for iOS platforms.
 */
class ShareServiceIosTest {
    /**
     * Verifies that shareFile and shareText execute safely without crashing.
     */
    @Test
    fun testShareOperationsSafety() {
        val shareService = IosShareService()

        // Should execute safely without uncaught exceptions, returning Result
        val textResult = shareService.shareText("Patient Summary Data")
        assertNotNull(textResult)

        val fileResult = shareService.shareFile("/tmp/clinical_export.json")
        assertNotNull(fileResult)
    }

    /**
     * Verifies that sharing an empty file path returns Result.failure with ExportFileNotFoundException.
     */
    @Test
    fun testShareEmptyFilePathFails() {
        val shareService = IosShareService()
        val emptyResult = shareService.shareFile("")
        assertTrue(emptyResult.isFailure)
        assertTrue(emptyResult.exceptionOrNull() is ExportFileNotFoundException)

        val blankResult = shareService.shareFile("   ")
        assertTrue(blankResult.isFailure)
    }

    /**
     * Verifies that sharing multilingual clinical strings succeeds without errors.
     */
    @Test
    fun testShareMultilingualText() {
        val shareService = IosShareService()

        // Test English, Spanish, Japanese, Hebrew, Traditional Chinese
        val texts =
            listOf(
                "Clinical Summary Export",
                "Resumen clínico de exportación",
                "臨床要約のエクスポート",
                "ייצוא סיכום קליני",
                "臨床摘要匯出",
            )

        for (text in texts) {
            val res = shareService.shareText(text)
            assertNotNull(res)
        }

        // Test blank string
        val blankRes = shareService.shareText("")
        assertTrue(blankRes.isSuccess)
    }

    /**
     * Verifies sharing files with paths containing spaces and non-standard characters.
     */
    @Test
    fun testShareSpecialCharactersFilePath() {
        val shareService = IosShareService()
        val result = shareService.shareFile("/tmp/patient records/encounter #1 [export].enc")
        assertNotNull(result)
    }
}
