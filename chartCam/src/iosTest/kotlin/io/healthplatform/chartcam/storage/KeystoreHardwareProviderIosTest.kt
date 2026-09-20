/**
 * @file KeystoreHardwareProviderIosTest.kt
 * Contains declarations for KeystoreHardwareProviderIosTest.kt.
 */
package io.healthplatform.chartcam.storage

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for KeystoreHardwareProvider on iOS.
 */
class KeystoreHardwareProviderIosTest {
    /**
     * Tests hardware provider methods on iOS.
     */
    @Test
    fun testKeystoreHardwareProvider() {
        val provider = createKeystoreHardwareProvider()
        assertNotNull(provider)
        val status = provider.getHardwareStatus()
        assertNotNull(status)
        val res = provider.checkHardwareBacked()
        assertNotNull(res)
    }
}
