package io.healthplatform.chartcam.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.camera.CameraManager
import org.mockito.Mockito
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CameraPreviewJvmTest {
    @Test
    fun testCameraPreview() =
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
