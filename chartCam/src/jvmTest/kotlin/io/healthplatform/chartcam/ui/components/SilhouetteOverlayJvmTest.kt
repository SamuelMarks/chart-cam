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

private val minimalBmp =
    byteArrayOf(
        0x42,
        0x4D,
        0x1E,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x1A,
        0x00,
        0x00,
        0x00,
        0x0C,
        0x00,
        0x00,
        0x00,
        0x01,
        0x00,
        0x01,
        0x00,
        0x01,
        0x00,
        0x18,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
    )

/** Mock sensor manager for testing silhouette overlay. */
private class MockSilhouetteSensor(
    initialPitch: Double = 0.0,
    initialRoll: Double = 0.0,
) : SensorManager {
    private val _orientation = MutableStateFlow(OrientationData(initialPitch, initialRoll))
    override val orientation = _orientation.asStateFlow()

    fun update(pitch: Double, roll: Double) {
        _orientation.value = OrientationData(pitch, roll)
    }

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
                    ghostImageBytes = minimalBmp,
                )
            }
            waitForIdle()
            onNodeWithContentDescription(
                "On-screen patient positioning silhouette: Position side profile: align corneal apex in the arc and nasal tip in the box. Camera is level",
            ).assertIsDisplayed()
        }
    }

    @Test
    fun testSilhouetteOverlayWithNullSensor() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                SilhouetteOverlay(
                    silhouetteType = SilhouetteType.PROFILE_CORNEA_NOSE_LEFT,
                    sensorManager = null,
                )
            }
            waitForIdle()
            onNodeWithContentDescription(
                "On-screen patient positioning silhouette: Position side profile: align corneal apex in the arc and nasal tip in the box. Camera is level",
            ).assertIsDisplayed()
        }
    }

    @Test
    fun testSilhouetteOverlaySensorTransition() {
        setAppLanguage("en")
        val sensor = MockSilhouetteSensor(10.0, 10.0)
        runComposeUiTest {
            setContent {
                SilhouetteOverlay(
                    silhouetteType = SilhouetteType.PROFILE_CORNEA_NOSE_LEFT,
                    sensorManager = sensor,
                )
            }
            waitForIdle()
            onNodeWithContentDescription(
                "On-screen patient positioning silhouette: Position side profile: align corneal apex in the arc and nasal tip in the box. Camera is tilted",
            ).assertIsDisplayed()

            // Transition sensor orientation to level
            sensor.update(0.0, 0.0)
            waitForIdle()
            onNodeWithContentDescription(
                "On-screen patient positioning silhouette: Position side profile: align corneal apex in the arc and nasal tip in the box. Camera is level",
            ).assertIsDisplayed()

            // Keep sensor level (verifies lastLevelAnnounced idempotence)
            sensor.update(1.0, 1.0)
            waitForIdle()
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

    @Test
    fun testSilhouetteOverlayContentPermutations() {
        runComposeUiTest {
            setContent {
                // Test all direct content combinations
                SilhouetteOverlayContent(
                    silhouetteType = SilhouetteType.PROFILE_CORNEA_NOSE_LEFT,
                    isLevel = false,
                    ghostImageBytes = ByteArray(10), // Invalid bytes branch
                )
                SilhouetteOverlayContent(
                    silhouetteType = SilhouetteType.PROFILE_CORNEA_NOSE_RIGHT,
                    isLevel = false,
                )
                SilhouetteOverlayContent(
                    silhouetteType = SilhouetteType.FRONTAL_FACE,
                    isLevel = true,
                    ghostImageBytes = minimalBmp, // Valid image render branch
                )
                SilhouetteOverlayContent(
                    silhouetteType = SilhouetteType.NONE,
                    isLevel = true,
                )
            }
            waitForIdle()
        }
    }
}
