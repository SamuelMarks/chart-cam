/**
 * @file CaptureViewModelDuplicateJvmTest.kt
 * Contains declarations for CaptureViewModelDuplicateJvmTest.kt.
 *
 * Validates capture view model duplicate handling.
 */
package io.healthplatform.chartcam.capture

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests to ensure duplicate IDs in steps are handled correctly without infinite loops.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelDuplicateJvmTest {
    /** Standard test dispatcher. */
    private val testDispatcher = StandardTestDispatcher()

    /** Setup the test dispatcher. */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    /** Tear down the test dispatcher. */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Ensures that steps with the same ID but different labels can be progressed through without looping.
     */
    @Test
    fun testDuplicateStepsDifferentLabel() =
        runTest {
            val viewModel = CaptureViewModel(MockCameraManager(), MockFileStorage())
            val steps =
                listOf(
                    PhotoStep("item_1", "Label A"),
                    PhotoStep("item_1", "Label B"),
                )
            viewModel.initSteps(steps)

            println("Initial step: ${viewModel.uiState.value.currentStep}")
            viewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.onConfirm()

            println("Next step: ${viewModel.uiState.value.currentStep}")
            println("isFinished: ${viewModel.uiState.value.isFinished}")

            viewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.onConfirm()

            println("Final isFinished: ${viewModel.uiState.value.isFinished}")
        }
}
