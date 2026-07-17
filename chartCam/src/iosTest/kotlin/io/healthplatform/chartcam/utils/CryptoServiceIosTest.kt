/**
 * @file CryptoServiceIosTest.kt
 * Contains declarations for CryptoServiceIosTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertNotNull

class CryptoServiceIosTest {
    @Test
    fun testCryptoServiceInitialization() {
        val service = CryptoService()
        assertNotNull(service)
    }
}
