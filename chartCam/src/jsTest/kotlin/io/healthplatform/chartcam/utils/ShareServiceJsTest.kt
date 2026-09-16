/**
 * @file ShareServiceJsTest.kt
 * Contains declarations for ShareServiceJsTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.browser.window
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for [JsShareService] on JavaScript runtime.
 */
class ShareServiceJsTest {
    /**
     * Test share service initialization on JS.
     */
    @Test
    fun testShareServiceJs() {
        val service = createShareService()
        assertNotNull(service)
        assertTrue(service is JsShareService)
    }

    /**
     * Test sharing text on JS runtime.
     */
    @Test
    fun testShareTextJs() {
        val service = createShareService()
        val result = service.shareText("Decentralized Export Password")
        assertNotNull(result)
    }

    /**
     * Test sharing a valid file with simulated download on JS runtime.
     */
    @Test
    fun testShareFileJs() {
        val service = createShareService()

        // Test without localStorage item (falls back to data URI)
        val resFallback = service.shareFile("questionnaire_test.json")
        assertNotNull(resFallback)

        // Test with pre-existing localStorage item
        window.localStorage.setItem("clinical_export.enc", "U2FtcGxlIERhdGE=")
        val resStored = service.shareFile("clinical_export.enc")
        assertNotNull(resStored)
        window.localStorage.removeItem("clinical_export.enc")
    }

    /**
     * Test sharing empty file path returns Result.failure.
     */
    @Test
    fun testShareEmptyFilePathJs() {
        val service = createShareService()
        val result = service.shareFile("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ExportFileNotFoundException)
    }
}
