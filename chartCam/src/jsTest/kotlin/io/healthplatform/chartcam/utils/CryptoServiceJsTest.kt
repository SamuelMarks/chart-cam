/**
 * @file CryptoServiceJsTest.kt
 * Contains declarations for CryptoServiceJsTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for CryptoService on JS.
 */
class CryptoServiceJsTest {
    /**
     * Test crypto service creation on JS.
     */
    @Test
    fun testCryptoServiceJs() {
        val crypto = CryptoService()
        assertNotNull(crypto)
    }
}
