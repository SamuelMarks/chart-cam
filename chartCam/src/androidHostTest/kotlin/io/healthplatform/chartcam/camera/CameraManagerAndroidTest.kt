/**
 * @file CameraManagerAndroidTest.kt
 * Contains Android host unit tests for [AndroidCameraManager].
 */
package io.healthplatform.chartcam.camera

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.healthplatform.chartcam.AndroidAppInit
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Robolectric host tests for [AndroidCameraManager] verifying initialization, flash control,
 * lens toggling, video recording guard states, and image capture behavior.
 */
@Config(manifest = "src/androidHostTest/AndroidManifest.xml", packageName = "io.healthplatform.chartcam")
@RunWith(RobolectricTestRunner::class)
class CameraManagerAndroidTest {
    /**
     * Verifies [AndroidCameraManager] lifecycle, lens toggling, flash control, and unbound capture guards.
     */
    @Test
    fun testCameraManagerAndroid() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            AndroidAppInit.init(context)
            val manager = AndroidCameraManager(context)
            assertNotNull(manager)
            assertFalse(manager.isRecordingVideo)
            assertTrue(manager.hasMultipleCameras)

            assertTrue(manager.setFlash(true).isSuccess)
            assertTrue(manager.setFlash(false).isSuccess)
            assertTrue(manager.toggleLens().isSuccess)
            assertTrue(manager.toggleLens().isSuccess)

            assertNull(manager.captureImage())
            assertTrue(manager.captureImageCatching().isFailure)

            assertTrue(manager.startVideoRecording().isFailure)
            assertTrue(manager.stopVideoRecording().isFailure)
            assertTrue(manager.cancelVideoRecording().isSuccess)

            manager.release()
        }
}
