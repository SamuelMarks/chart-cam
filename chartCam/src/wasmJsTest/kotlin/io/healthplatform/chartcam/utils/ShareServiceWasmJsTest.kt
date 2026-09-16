/**
 * @file ShareServiceWasmJsTest.kt
 * Contains declarations for ShareServiceWasmJsTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for [WasmJsShareService] on WebAssembly runtime.
 */
class ShareServiceWasmJsTest {
    /**
     * Test share service initialization on WasmJS.
     */
    @Test
    fun testShareServiceWasmJs() {
        val service = createShareService()
        assertNotNull(service)
        assertTrue(service is WasmJsShareService)
    }

    /**
     * Test sharing text on WasmJS runtime.
     */
    @Test
    fun testShareTextWasmJs() {
        val service = createShareService()
        val result = service.shareText("Wasm Encrypted Password")
        assertNotNull(result)
    }

    /**
     * Test sharing a valid file with simulated download on WasmJS runtime.
     */
    @Test
    fun testShareFileWasmJs() {
        val service = createShareService()
        val result = service.shareFile("questionnaire_wasm.json")
        assertNotNull(result)
    }

    /**
     * Test sharing empty file path returns Result.failure on WasmJS runtime.
     */
    @Test
    fun testShareEmptyFilePathWasmJs() {
        val service = createShareService()
        val result = service.shareFile("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ExportFileNotFoundException)
    }
}
