package io.healthplatform.chartcam.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import io.healthplatform.chartcam.camera.CameraManager
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class CameraPreviewAndroidTest {
    @Test
    fun testCameraPreviewAndroid() =
        runComposeUiTest {
            val mockCameraManager = Mockito.mock(CameraManager::class.java)

            setContent {
                CameraPreview(
                    modifier = Modifier,
                    cameraManager = mockCameraManager,
                )
            }

            onRoot().assertExists()
        }
}
