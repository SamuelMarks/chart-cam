/**
 * @file AndroidKeystoreHardwareProviderAndroidTest.kt
 * Contains declarations for AndroidKeystoreHardwareProviderAndroidTest.kt.
 */
package io.healthplatform.chartcam.storage

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.healthplatform.chartcam.AndroidAppInit
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android host tests evaluating [AndroidKeystoreHardwareProvider].
 */
@Config(manifest = Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class AndroidKeystoreHardwareProviderAndroidTest {
    /**
     * Verifies hardware backing check and status for modern Android API levels.
     */
    @Test
    fun testModernAndroidHardwareStatus() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            AndroidAppInit.init(context)
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            shadowOf(km).setIsDeviceSecure(true)

            val provider = AndroidKeystoreHardwareProvider(sdkInt = Build.VERSION_CODES.M)
            val checkRes = provider.checkHardwareBacked()
            assertTrue(checkRes.isSuccess)
            assertEquals(BiometricHardwareStatus.AVAILABLE, provider.getHardwareStatus())

            val promptRes = provider.promptBiometrics("Title", "Subtitle")
            assertTrue(promptRes.isSuccess)

            shadowOf(km).setIsDeviceSecure(false)
            val promptInsecureRes = provider.promptBiometrics("Title", "Subtitle")
            assertFalse(promptInsecureRes.isSuccess)
        }

    /**
     * Verifies legacy Android API levels report no hardware support.
     */
    @Test
    fun testLegacyAndroidHardwareStatus() =
        runTest {
            val provider = AndroidKeystoreHardwareProvider(sdkInt = Build.VERSION_CODES.LOLLIPOP)
            val checkRes = provider.checkHardwareBacked()
            assertFalse(checkRes.isSuccess)
            assertEquals(BiometricHardwareStatus.NO_HARDWARE, provider.getHardwareStatus())

            val promptRes = provider.promptBiometrics("Title", "Subtitle")
            assertFalse(promptRes.isSuccess)
        }

    /**
     * Verifies actual factory function instantiation.
     */
    @Test
    fun testCreateKeystoreHardwareProviderFactory() {
        val provider = createKeystoreHardwareProvider()
        assertNotNull(provider)
        assertTrue(provider is AndroidKeystoreHardwareProvider)
    }

    /**
     * Verifies prompt biometrics branches when context or keyguard service are unavailable.
     */
    @Test
    fun testPromptBiometricsContextBranches() =
        runTest {
            val nullCtxProvider =
                AndroidKeystoreHardwareProvider(
                    sdkInt = Build.VERSION_CODES.M,
                    contextProvider = { null },
                )
            assertTrue(nullCtxProvider.promptBiometrics("T", "S").isSuccess)

            val baseContext = ApplicationProvider.getApplicationContext<Context>()
            val fakeCtx =
                object : android.content.ContextWrapper(baseContext) {
                    override fun getSystemService(name: String): Any? = null
                }
            val noKmProvider =
                AndroidKeystoreHardwareProvider(
                    sdkInt = Build.VERSION_CODES.M,
                    contextProvider = { fakeCtx },
                )
            assertTrue(noKmProvider.promptBiometrics("T", "S").isSuccess)
        }
}
