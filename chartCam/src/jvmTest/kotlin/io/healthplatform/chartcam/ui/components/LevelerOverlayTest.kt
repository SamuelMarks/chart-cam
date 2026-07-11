package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.healthplatform.chartcam.sensors.OrientationData
import io.healthplatform.chartcam.sensors.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import kotlin.test.Test

class MockSensorManager : SensorManager {
    val mutableOrientation = MutableStateFlow(OrientationData(0.0, 0.0))
    override val orientation: StateFlow<OrientationData> = mutableOrientation

    override fun startListening() {}

    override fun stopListening() {}
}

@OptIn(ExperimentalTestApi::class)
class LevelerOverlayTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testLevelerSemanticsLevel() {
        val sensorManager = MockSensorManager()
        rule.setContent {
            LevelerOverlay(sensorManager = sensorManager)
        }
        rule.onNodeWithContentDescription("Camera Leveler: Camera is level").assertExists()
    }

    @Test
    fun testLevelerSemanticsTilted() {
        val sensorManager = MockSensorManager()
        sensorManager.mutableOrientation.value = OrientationData(4.0, 4.0)
        rule.setContent {
            LevelerOverlay(sensorManager = sensorManager)
        }
        rule.onNodeWithContentDescription("Camera Leveler: Camera is tilted").assertExists()
    }
}
