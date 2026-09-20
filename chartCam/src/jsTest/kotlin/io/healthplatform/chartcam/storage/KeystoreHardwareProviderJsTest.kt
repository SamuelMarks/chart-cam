/**
 * @file KeystoreHardwareProviderJsTest.kt
 * Contains declarations for KeystoreHardwareProviderJsTest.kt.
 */
package io.healthplatform.chartcam.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for KeystoreHardwareProvider on JS.
 */
class KeystoreHardwareProviderJsTest {
    /**
     * Tests hardware provider methods on JS.
     */
    @Test
    fun testKeystoreHardwareProvider() {
        val provider = createKeystoreHardwareProvider()
        assertNotNull(provider)
        val res = provider.checkHardwareBacked()
        assertTrue(res.isFailure)
        assertEquals(BiometricHardwareStatus.NO_HARDWARE, provider.getHardwareStatus())
    }
}
