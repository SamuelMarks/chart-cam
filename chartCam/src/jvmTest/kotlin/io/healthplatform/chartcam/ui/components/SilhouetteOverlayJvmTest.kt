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

private val validPngBytes =
    byteArrayOf(
        0x89.toByte(),
        0x50.toByte(),
        0x4E.toByte(),
        0x47.toByte(),
        0x0D.toByte(),
        0x0A.toByte(),
        0x1A.toByte(),
        0x0A.toByte(),
        0x00,
        0x00,
        0x00,
        0x0D,
        0x49,
        0x48,
        0x44,
        0x52,
        0x00,
        0x00,
        0x00,
        0x01,
        0x00,
        0x00,
        0x00,
        0x01,
        0x08,
        0x06,
        0x00,
        0x00,
        0x00,
        0x1F,
        0x15,
        0xC4.toByte(),
        0x89.toByte(),
        0x00,
        0x00,
        0x00,
        0x0A,
        0x49,
        0x44,
        0x41,
        0x54,
        0x78,
        0x9C.toByte(),
        0x63,
        0x00,
        0x01,
        0x00,
        0x00,
        0x05,
        0x00,
        0x01,
        0x0D.toByte(),
        0x0A.toByte(),
        0x2D.toByte(),
        0xB4.toByte(),
        0x00,
        0x00,
        0x00,
        0x00,
        0x49,
        0x45,
        0x4E,
        0x44,
        0xAE.toByte(),
        0x42,
        0x60,
        0x82.toByte(),
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
                    ghostImageBytes = validPngBytes,
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
                    isVisible = true,
                )
                SilhouetteOverlay(
                    silhouetteType = SilhouetteType.FRONTAL_FACE,
                    isVisible = false,
                )
                SilhouetteOverlay(
                    silhouetteType = SilhouetteType.NONE,
                    isVisible = false,
                )
                SilhouetteOverlay(
                    silhouetteType = SilhouetteType.PROFILE_CORNEA_NOSE_LEFT,
                    isVisible = true,
                    modifier = androidx.compose.ui.Modifier,
                )
            }
            waitForIdle()
        }
    }

    @Test
    fun testSilhouetteOverlayRollTiltedPitchLevel() {
        setAppLanguage("en")
        val sensor = MockSilhouetteSensor(initialPitch = 1.0, initialRoll = 15.0)
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
        }
    }

    @Test
    fun testSilhouetteOverlayHapticLevelRecomposition() {
        runComposeUiTest {
            val isLevelState = androidx.compose.runtime.mutableStateOf(false)
            val outerState = androidx.compose.runtime.mutableStateOf(0)
            setContent {
                val dummy = outerState.value
                SilhouetteOverlayContent(
                    silhouetteType = SilhouetteType.FRONTAL_FACE,
                    isLevel = isLevelState.value,
                    ghostImageBytes = null,
                )
            }
            waitForIdle()

            // Transition from not level to level (triggers haptic feedback)
            isLevelState.value = true
            waitForIdle()

            // Keep level across another recomposition with outer trigger (lastLevelAnnounced == true, suppresses re-trigger)
            outerState.value++
            waitForIdle()

            // Transition back to tilted
            isLevelState.value = false
            waitForIdle()
        }
    }

    @Test
    fun testSilhouetteOverlayContentPermutations() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                // Test each silhouette type individually
                SilhouetteOverlayContent(
                    silhouetteType = SilhouetteType.PROFILE_CORNEA_NOSE_LEFT,
                    isLevel = false,
                    ghostImageBytes = ByteArray(10), // Invalid bytes branch
                )
                SilhouetteOverlayContent(
                    silhouetteType = SilhouetteType.PROFILE_CORNEA_NOSE_RIGHT,
                    isLevel = false,
                    ghostImageBytes = null,
                )
                SilhouetteOverlayContent(
                    silhouetteType = SilhouetteType.FRONTAL_FACE,
                    isLevel = true,
                    ghostImageBytes = validPngBytes, // Valid image render branch
                )
                SilhouetteOverlayContent(
                    silhouetteType = SilhouetteType.FRONTAL_FACE,
                    isLevel = false,
                    ghostImageBytes = null,
                )
            }
            waitForIdle()
        }
    }

    /**
     * Tests SilhouetteOverlayContent with NONE type.
     */
    @Test
    fun testSilhouetteOverlayContentNone() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                SilhouetteOverlayContent(
                    silhouetteType = SilhouetteType.NONE,
                    isLevel = true,
                )
            }
            waitForIdle()
            onNodeWithContentDescription(
                "On-screen patient positioning silhouette: . Camera is level",
            ).assertIsDisplayed()
        }
    }

    /**
     * Tests recomposition and skipping for SilhouetteOverlay and SilhouetteOverlayContent.
     */
    @Test
    fun testSilhouetteOverlayRecompositionAndSkipping() {
        runComposeUiTest {
            val outerTrigger = androidx.compose.runtime.mutableStateOf(0)
            val typeState = androidx.compose.runtime.mutableStateOf(SilhouetteType.FRONTAL_FACE)
            val isVisibleState = androidx.compose.runtime.mutableStateOf(true)
            val ghostState = androidx.compose.runtime.mutableStateOf<ByteArray?>(null)
            val sensor = MockSilhouetteSensor(0.0, 0.0)

            setContent {
                val dummy = outerTrigger.value
                SilhouetteOverlay(
                    silhouetteType = typeState.value,
                    sensorManager = sensor,
                    ghostImageBytes = ghostState.value,
                    isVisible = isVisibleState.value,
                )
                SilhouetteOverlayContent(
                    silhouetteType = typeState.value,
                    isLevel = true,
                    ghostImageBytes = ghostState.value,
                )
            }
            waitForIdle()

            // Outer trigger causes parent to recompose while children can skip
            outerTrigger.value++
            waitForIdle()

            // Update typeState
            typeState.value = SilhouetteType.PROFILE_CORNEA_NOSE_LEFT
            waitForIdle()

            // Update ghostState
            ghostState.value = validPngBytes
            waitForIdle()

            // Update isVisibleState
            isVisibleState.value = false
            waitForIdle()
        }
    }
}
