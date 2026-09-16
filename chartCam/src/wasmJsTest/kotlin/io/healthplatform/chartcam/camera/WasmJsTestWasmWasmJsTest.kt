/**
 * @file WasmJsTestWasmWasmJsTest.kt
 * Contains declarations for WasmJsTestWasmWasmJsTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for WasmJS camera testing utilities.
 */
class WasmJsTestWasmWasmJsTest {
    /**
     * Test string trimming behavior in WasmJS.
     */
    @Test
    fun testWasm() {
        val testString = "  wasm_test  "
        assertEquals("wasm_test", testString.trim())
    }
}
