/**
 * @file CryptoServiceWasmJsTest.kt
 * Contains declarations for CryptoServiceWasmJsTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for CryptoService on WasmJS.
 */
class CryptoServiceWasmJsTest {
    /**
     * Test crypto service creation on WasmJS.
     */
    @Test
    fun testCryptoServiceWasmJs() {
        val crypto = CryptoService()
        assertNotNull(crypto)
    }
}
