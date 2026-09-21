/**
 * @file SilhouetteOverlayJvmTest.kt
 * Contains UI tests for SilhouetteOverlay on JVM.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.camera.SilhouetteType
import io.healthplatform.chartcam.sensors.OrientationData
import io.healthplatform.chartcam.sensors.SensorManager
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

/** Mock sensor manager for testing silhouette overlay. */
private class MockSilhouetteSensor(
    initialPitch: Double = 0.0,
    initialRoll: Double = 0.0,
) : SensorManager {
    private val _orientation = MutableStateFlow(OrientationData(initialPitch, initialRoll))
    override val orientation = _orientation.asStateFlow()

    override fun startListening() {}

    override fun stopListening() {}
}

/**
 * UI tests for SilhouetteOverlay.
 */
@OptIn(ExperimentalTestApi::class)
class SilhouetteOverlayJvmTest {
    @Test
    fun testSilhouetteOverlayProfileLeftWhenLevel() {
        setAppLanguage("en")
        val sensor = MockSilhouetteSensor(0.0, 0.0)
        runComposeUiTest {
            setContent {
                SilhouetteOverlay(
                    silhouetteType = SilhouetteType.PROFILE_CORNEA_NOSE_LEFT,
                    sensorManager = sensor,
                )
            }
            waitForIdle()
            onNodeWithContentDescription(
                "On-screen patient positioning silhouette: Position side profile: align corneal apex in the arc and nasal tip in the box. Camera is level",
            ).assertIsDisplayed()
        }
    }

    @Test
    fun testSilhouetteOverlayFrontalWhenTilted() {
        setAppLanguage("en")
        val sensor = MockSilhouetteSensor(15.0, 10.0)
        runComposeUiTest {
            setContent {
                SilhouetteOverlay(
                    silhouetteType = SilhouetteType.FRONTAL_FACE,
                    sensorManager = sensor,
                )
            }
            waitForIdle()
            onNodeWithContentDescription(
                "On-screen patient positioning silhouette: Position front view: align eyes on the horizontal line and nose on the center axis. Camera is tilted",
            ).assertIsDisplayed()
        }
    }

    @Test
    fun testSilhouetteOverlayContralateralWithGhost() {
        setAppLanguage("en")
        val sensor = MockSilhouetteSensor(0.0, 0.0)
        runComposeUiTest {
            setContent {
                SilhouetteOverlay(
                    silhouetteType = SilhouetteType.PROFILE_CORNEA_NOSE_RIGHT,
                    sensorManager = sensor,
                    ghostImageBytes = ByteArray(10),
                )
            }
            waitForIdle()
            onNodeWithContentDescription(
                "On-screen patient positioning silhouette: Position side profile: align corneal apex in the arc and nasal tip in the box. Camera is level",
            ).assertIsDisplayed()
        }
    }

    @Test
    fun testSilhouetteOverlayInvisibleOrNone() {
        runComposeUiTest {
            setContent {
                SilhouetteOverlay(
                    silhouetteType = SilhouetteType.NONE,
                )
                SilhouetteOverlay(
                    silhouetteType = SilhouetteType.PROFILE_CORNEA_NOSE_LEFT,
                    isVisible = false,
                )
            }
            waitForIdle()
        }
    }
}
