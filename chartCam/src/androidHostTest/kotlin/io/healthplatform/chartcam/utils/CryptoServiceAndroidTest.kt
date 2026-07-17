/**
 * @file CryptoServiceAndroidTest.kt
 * Contains declarations for CryptoServiceAndroidTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertNotNull

class CryptoServiceAndroidTest {
    @Test
    fun testCryptoServiceInitialization() {
        val service = CryptoService()
        assertNotNull(service)
    }
}
