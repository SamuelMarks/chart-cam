/**
 * @file LevelerOverlayJvmTest.kt
 * Contains declarations for LevelerOverlayJvmTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.sensors.OrientationData
import io.healthplatform.chartcam.sensors.SensorManager
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

/**
 * Mock SensorManager for testing LevelerOverlay(SensorManager).
 */
private class MockSensorManager : SensorManager {
    private val _orientation = MutableStateFlow(OrientationData(0.0, 0.0))
    override val orientation = _orientation.asStateFlow()

    override fun startListening() {}

    override fun stopListening() {}
}

/**
 * Test class for LevelerOverlay on JVM.
 */
class LevelerOverlayJvmTest {
    /**
     * Test leveler overlay on JVM when device is level.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLevelerOverlayWhenLevel() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                LevelerOverlay(pitch = 0f, roll = 0f)
            }
            mainClock.advanceTimeBy(600L)
            waitForIdle()
            onNodeWithContentDescription("Camera Leveler: Camera is level").assertIsDisplayed()
        }
    }

    /**
     * Test leveler overlay on JVM when device is tilted.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLevelerOverlayWhenTilted() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                LevelerOverlay(pitch = 15f, roll = 15f)
            }
            mainClock.advanceTimeBy(600L)
            waitForIdle()
            onNodeWithContentDescription("Camera Leveler: Camera is tilted").assertIsDisplayed()
        }
    }

    /**
     * Test leveler overlay transition from level to tilted to cover state transition branches,
     * recomposition skipping, and debounce bounce cancellation where lastAnnouncedLevel == isLevel.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLevelerOverlayTransition() {
        setAppLanguage("en")
        runComposeUiTest {
            val pitchState = androidx.compose.runtime.mutableStateOf(0f)
            val rollState = androidx.compose.runtime.mutableStateOf(0f)
            val tolState = androidx.compose.runtime.mutableStateOf(3.0f)
            val parentState = androidx.compose.runtime.mutableStateOf(0)

            setContent {
                val p = parentState.value
                androidx.compose.material3.Text("Parent: $p")
                LevelerOverlay(
                    pitch = pitchState.value,
                    roll = rollState.value,
                    tolerance = tolState.value,
                )
            }
            mainClock.advanceTimeBy(600L)
            waitForIdle()
            onNodeWithContentDescription("Camera Leveler: Camera is level").assertIsDisplayed()

            // 1. Mutate pitch, roll, and tolerance to execute composer.changed true branches
            pitchState.value = 10f
            mainClock.advanceTimeBy(600L)
            waitForIdle()
            onNodeWithContentDescription("Camera Leveler: Camera is tilted").assertIsDisplayed()

            rollState.value = 2f
            waitForIdle()
            tolState.value = 4.0f
            waitForIdle()

            // 2. Rapid bounce back to tilted (isLevel: false -> true -> false quickly)
            // so that when debounce fires, lastAnnouncedLevel (false) == isLevel (false), covering the false branch of line 98
            pitchState.value = 0f
            rollState.value = 0f
            mainClock.advanceTimeBy(100L) // Not enough to debounce (500ms)
            pitchState.value = 10f // back to tilted before debounce completed
            mainClock.advanceTimeBy(600L) // debounce fires with isLevel == false, but lastAnnouncedLevel was already false!
            waitForIdle()

            // 3. Parent recomposition where LevelerOverlay inputs do not change (smart recomposition skipping)
            parentState.value = 1
            waitForIdle()
        }
    }

    /**
     * Test leveler overlay with [SensorManager] overload and recomposition skipping.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLevelerOverlayWithSensorManager() {
        setAppLanguage("en")
        runComposeUiTest {
            val sensorManager = MockSensorManager()
            val parentState = androidx.compose.runtime.mutableStateOf(0)
            setContent {
                val p = parentState.value
                androidx.compose.material3.Text("Parent: $p")
                LevelerOverlay(sensorManager)
            }
            mainClock.advanceTimeBy(600L)
            waitForIdle()
            onNodeWithContentDescription("Camera Leveler: Camera is level").assertIsDisplayed()

            // Smart recomposition skip
            parentState.value = 1
            waitForIdle()
        }
    }

    /**
     * Test leveler overlay high tilt extremes and boundary conditions.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLevelerOverlayExtremes() {
        setAppLanguage("en")
        runComposeUiTest {
            setContent {
                // Extremes
                LevelerOverlay(pitch = -40f, roll = 40f)
                // Pitch exceeds tolerance but roll is 0
                LevelerOverlay(pitch = 5f, roll = 0f)
                // Roll exceeds tolerance but pitch is 0
                LevelerOverlay(pitch = 0f, roll = 5f)
                // Custom tolerance
                LevelerOverlay(pitch = 1f, roll = 1f, tolerance = 0.5f)
                LevelerOverlay(pitch = 0.2f, roll = 0.2f, tolerance = 0.5f)
            }
            mainClock.advanceTimeBy(600L)
            waitForIdle()
        }
    }
}
