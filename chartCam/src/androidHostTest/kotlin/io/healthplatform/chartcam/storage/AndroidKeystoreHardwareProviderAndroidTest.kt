package io.healthplatform.chartcam.storage

import android.os.Build
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android host tests evaluating [AndroidKeystoreHardwareProvider].
 */
@Config(manifest = Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class AndroidKeystoreHardwareProviderAndroidTest {
    @Test
    fun testLegacyAndroidHardwareStatus() =
        runTest {
            val provider = AndroidKeystoreHardwareProvider(sdkInt = Build.VERSION_CODES.LOLLIPOP)
            val checkRes = provider.checkHardwareBacked()
            assertFalse(checkRes.isSuccess)
        }

    @Test
    fun testCreateKeystoreHardwareProviderFactory() {
        val provider = createKeystoreHardwareProvider()
        assertNotNull(provider)
        assertTrue(provider is AndroidKeystoreHardwareProvider)
    }
}
