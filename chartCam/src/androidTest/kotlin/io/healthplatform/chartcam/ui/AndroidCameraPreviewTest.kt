package io.healthplatform.chartcam.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.healthplatform.chartcam.AndroidAppInit
import io.healthplatform.chartcam.camera.AndroidCameraManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [CameraPreview] on Android.
 */
@RunWith(AndroidJUnit4::class)
class AndroidCameraPreviewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        AndroidAppInit.init(ApplicationProvider.getApplicationContext())
    }

    /**
     * Test CameraPreview renders without crashing.
     */
    @Test
    fun testCameraPreviewRendering() {
        val manager = AndroidCameraManager(ApplicationProvider.getApplicationContext())
        composeTestRule.setContent {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                cameraManager = manager,
            )
        }
        // Since we cannot easily verify surface view internals via compose test rules alone,
        // we assert it does not crash.
    }
}
