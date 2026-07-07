package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import io.healthplatform.chartcam.sensors.OrientationData
import io.healthplatform.chartcam.sensors.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.Mockito
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LevelerOverlayTest {
    @Test
    fun testLevelerOverlay() =
        runComposeUiTest {
            val mockSensorManager = Mockito.mock(SensorManager::class.java)
            Mockito.`when`(mockSensorManager.orientation).thenReturn(MutableStateFlow(OrientationData(0.0, 0.0)))

            setContent {
                LevelerOverlay(
                    sensorManager = mockSensorManager,
                )
            }

            onRoot().assertExists()
        }
}
