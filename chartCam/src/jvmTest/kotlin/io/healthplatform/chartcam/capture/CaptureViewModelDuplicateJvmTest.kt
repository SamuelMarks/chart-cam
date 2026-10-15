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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

            assertEquals(
                "Label A",
                viewModel.uiState.value.currentStep
                    ?.title,
            )
            assertEquals(0, viewModel.uiState.value.capturedCount)
            assertFalse(viewModel.uiState.value.isFinished)

            viewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.reviewImageBytes)
            viewModel.onConfirm()
            assertNull(viewModel.uiState.value.reviewImageBytes)

            assertEquals(
                "Label B",
                viewModel.uiState.value.currentStep
                    ?.title,
            )
            assertEquals(1, viewModel.uiState.value.capturedCount)
            assertFalse(viewModel.uiState.value.isFinished)

            viewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.reviewImageBytes)
            viewModel.onConfirm()

            assertEquals(2, viewModel.uiState.value.capturedCount)
            assertTrue(viewModel.uiState.value.isFinished)
        }
}
