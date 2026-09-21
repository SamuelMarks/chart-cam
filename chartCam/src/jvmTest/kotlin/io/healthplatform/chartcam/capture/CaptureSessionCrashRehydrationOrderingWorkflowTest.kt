/**
 * @file CaptureSessionCrashRehydrationOrderingWorkflowTest.kt
 * Contains declarations for CaptureSessionCrashRehydrationOrderingWorkflowTest.kt.
 *
 * Validates session rehydration order independence between restoreSession() and initSteps(),
 * ensuring captured progress is preserved across activity recreation and lifecycle restarts.
 */
package io.healthplatform.chartcam.capture

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Workflow tests ensuring that camera capture sessions properly rehydrate when restoreSession
 * and initSteps are invoked in arbitrary order during process re-creation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureSessionCrashRehydrationOrderingWorkflowTest {
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Prepares test dispatchers.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Cleans up test dispatchers.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Verifies that restoring session before calling initSteps preserves prior photos and advances step index.
     */
    @Test
    fun testRestoringSessionBeforeInitStepsPreservesProgress() =
        runTest(testDispatcher) {
            val cameraManager = MockCameraManager()
            val fileStorage = MockFileStorage()

            val step1 = PhotoStep("left_ear", "Left Ear")
            val step2 = PhotoStep("right_ear", "Right Ear")
            val step3 = PhotoStep("throat", "Throat")
            val steps = listOf(step1, step2, step3)

            val savedPaths =
                mapOf(
                    step1 to "/path/ear_l.jpg",
                    step2 to "/path/ear_r.jpg",
                )

            val vm = CaptureViewModel(cameraManager, fileStorage)

            // Rehydration order: restoreSession invoked first, then initSteps
            vm.restoreSession(savedPaths)
            vm.initSteps(steps)

            val state = vm.uiState.value
            assertEquals(2, state.capturedCount, "Should preserve 2 previously captured photos")
            assertEquals(3, state.totalSteps, "Total steps should reflect the sequence size")
            assertEquals(step3, state.currentStep, "Current step must advance to throat")
            assertFalse(state.isFinished, "Session is not finished yet as 1 step remains")

            // Complete Step 3
            vm.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.onConfirm()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(vm.uiState.value.isFinished, "Session must finish after 3rd photo confirmed")
            val results = vm.getResultPaths()
            assertEquals(3, results.size)
            assertEquals("/path/ear_l.jpg", results[step1])
            assertEquals("/path/ear_r.jpg", results[step2])
            assertNotNull(results[step3])
        }

    /**
     * Verifies that restoring an already completed session directly transitions to finished.
     */
    @Test
    fun testFullyCompletedSessionRehydrationTransitionsToFinished() =
        runTest(testDispatcher) {
            val cameraManager = MockCameraManager()
            val fileStorage = MockFileStorage()

            val step1 = PhotoStep("s1", "Step 1")
            val step2 = PhotoStep("s2", "Step 2")
            val steps = listOf(step1, step2)

            val completedPaths =
                mapOf(
                    step1 to "/path/s1.jpg",
                    step2 to "/path/s2.jpg",
                )

            val vm = CaptureViewModel(cameraManager, fileStorage)
            vm.restoreSession(completedPaths)
            vm.initSteps(steps)

            assertTrue(vm.uiState.value.isFinished, "Fully captured session should immediately be finished")
            assertNull(vm.uiState.value.currentStep)
            assertEquals(2, vm.uiState.value.capturedCount)
            assertEquals(io.healthplatform.chartcam.camera.SilhouetteType.NONE, vm.uiState.value.silhouetteType)
        }
}
