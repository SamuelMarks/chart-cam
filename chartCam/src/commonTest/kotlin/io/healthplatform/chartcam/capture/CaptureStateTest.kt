/**
 * @file CaptureStateTest.kt
 * Contains declarations for CaptureStateTest.kt.
 */
package io.healthplatform.chartcam.capture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests for UI states and capture steps.
 */
class CaptureStateTest {
    /**
     * Validates [PhotoStep] equivalence and predefined sizes.
     */
    @Test
    fun testPhotoStep() {
        val step1 = PhotoStep("1", "Title 1")
        val step2 = PhotoStep("1", "Title 1")
        assertEquals(step1, step2)
        assertEquals(8, PhotoStep.STANDARD_STEPS.size)
    }

    /**
     * Validates [CaptureUiState] equivalence and hashcode behavior across its varying states.
     */
    @Test
    fun testCaptureUiStateEqualsAndHashCode() {
        val state1 = CaptureUiState(currentStep = PhotoStep("1", "A"), reviewImageBytes = byteArrayOf(1, 2, 3))
        val state2 = CaptureUiState(currentStep = PhotoStep("1", "A"), reviewImageBytes = byteArrayOf(1, 2, 3))
        val state3 = CaptureUiState(currentStep = PhotoStep("2", "B"), reviewImageBytes = byteArrayOf(1, 2, 3))
        val state4 = CaptureUiState(currentStep = PhotoStep("1", "A"), reviewImageBytes = byteArrayOf(1, 2))
        val state5 = CaptureUiState(currentStep = PhotoStep("1", "A"), reviewImageBytes = null)

        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())

        assertNotEquals(state1, state3)
        assertNotEquals(state1, state4)
        assertNotEquals(state1, state5)
        assertNotEquals(state5, state1)

        // Coverage for other fields
        assertNotEquals(CaptureUiState(totalSteps = 1), CaptureUiState(totalSteps = 2))
        assertNotEquals(CaptureUiState(isCapturing = true), CaptureUiState(isCapturing = false))
        assertNotEquals(CaptureUiState(capturedCount = 1), CaptureUiState(capturedCount = 2))
        assertNotEquals(CaptureUiState(isFinished = true), CaptureUiState(isFinished = false))
    }
}
