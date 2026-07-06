/**
 * Test definitions for the capture state data structures.
 */
package io.healthplatform.chartcam.capture

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Validates the custom equality and hashcode implementations of [CaptureUiState].
 */
class CaptureStateTest {
    /**
     * Verifies that instances are equal to themselves.
     */
    @Test
    fun `equals returns true for same instance`() {
        val state = CaptureUiState()
        assertTrue(state.equals(state))
    }

    /**
     * Verifies that equals handles null gracefully.
     */
    @Test
    fun `equals returns false for null`() {
        val state = CaptureUiState()
        assertFalse(state.equals(null))
    }

    /**
     * Verifies that equals handles different classes correctly.
     */
    @Test
    fun `equals returns false for different class`() {
        val state = CaptureUiState()
        assertFalse(state.equals("Some String"))
    }

    /**
     * Verifies equality based on standard property differences.
     */
    @Test
    fun `equals returns false for different standard properties`() {
        val state1 = CaptureUiState(currentStep = PhotoStep("1", "One"))
        val state2 = CaptureUiState(currentStep = PhotoStep("2", "Two"))

        assertFalse(state1 == state2)

        val state3 = CaptureUiState(totalSteps = 1)
        val state4 = CaptureUiState(totalSteps = 2)
        assertFalse(state3 == state4)

        val state5 = CaptureUiState(isCapturing = true)
        val state6 = CaptureUiState(isCapturing = false)
        assertFalse(state5 == state6)

        val state7 = CaptureUiState(capturedCount = 1)
        val state8 = CaptureUiState(capturedCount = 2)
        assertFalse(state7 == state8)

        val state9 = CaptureUiState(isFinished = true)
        val state10 = CaptureUiState(isFinished = false)
        assertFalse(state9 == state10)
    }

    /**
     * Verifies that arrays are compared by content, not identity.
     */
    @Test
    fun `equals returns true for same array content`() {
        val array1 = byteArrayOf(1, 2, 3)
        val array2 = byteArrayOf(1, 2, 3)
        val state1 = CaptureUiState(reviewImageBytes = array1)
        val state2 = CaptureUiState(reviewImageBytes = array2)

        assertTrue(state1 == state2)
    }

    /**
     * Verifies that different array contents result in inequality.
     */
    @Test
    fun `equals returns false for different array content`() {
        val state1 = CaptureUiState(reviewImageBytes = byteArrayOf(1, 2, 3))
        val state2 = CaptureUiState(reviewImageBytes = byteArrayOf(1, 2, 4))

        assertFalse(state1 == state2)
    }

    /**
     * Verifies inequality when one byte array is null and the other is not.
     */
    @Test
    fun `equals returns false when one byte array is null`() {
        val state1 = CaptureUiState(reviewImageBytes = byteArrayOf(1, 2, 3))
        val state2 = CaptureUiState(reviewImageBytes = null)

        assertFalse(state1 == state2)
        assertFalse(state2 == state1)
    }

    /**
     * Validates that hashCode incorporates all fields including array contents.
     */
    @Test
    fun `hashCode behaves consistently with equals`() {
        val state1 =
            CaptureUiState(
                currentStep = PhotoStep("1", "One"),
                totalSteps = 5,
                isCapturing = true,
                reviewImageBytes = byteArrayOf(1, 2, 3),
                capturedCount = 2,
                isFinished = false,
            )
        val state2 =
            CaptureUiState(
                currentStep = PhotoStep("1", "One"),
                totalSteps = 5,
                isCapturing = true,
                reviewImageBytes = byteArrayOf(1, 2, 3),
                capturedCount = 2,
                isFinished = false,
            )

        assertEquals(state1.hashCode(), state2.hashCode())

        val state3 = state1.copy(isFinished = true)
        assertNotEquals(state1.hashCode(), state3.hashCode())
    }
}
