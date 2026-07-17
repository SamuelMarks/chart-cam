/**
 * @file CameraPreviewAndroidTest.kt
 * Contains declarations for CameraPreviewAndroidTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.camera.CameraManager
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalTestApi::class)
@org.robolectric.annotation.Config(manifest = "src/androidHostTest/AndroidManifest.xml", packageName = "io.healthplatform.chartcam")
@RunWith(RobolectricTestRunner::class)
class CameraPreviewAndroidTest {
    @org.junit.Ignore("Robolectric doesn't support this")
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
