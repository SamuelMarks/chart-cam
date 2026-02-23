package io.healthplatform.chartcam.camera

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertTrue

class JvmCameraManagerTest {

    @Test
    fun testCaptureImage() = runBlocking {
        val manager = JvmCameraManager()
        
        // Ensure no exception when toggling features that are unsupported
        manager.setFlash(true)
        manager.setFlash(false)
        manager.toggleLens()

        // Capture image could return null on CI environments without camera
        // We just ensure it doesn't crash.
        val result = manager.captureImage()
        if (result != null) {
            assertTrue(result.isNotEmpty())
        }

        // Release should also not throw
        manager.release()
    }
}
