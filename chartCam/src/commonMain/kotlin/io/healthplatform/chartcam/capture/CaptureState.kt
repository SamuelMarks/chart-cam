/**
 * Contains data structures defining the state and steps for the clinical photo capture workflow.
 */
package io.healthplatform.chartcam.capture

/**
 * Represents a specific angle or type of photo required in the clinical sequence.
 * The order of these steps dictates the state machine progression during capture.
 */
data class PhotoStep(
    /** Unique identifier for this step. */
    val id: String,
    /** Human readable name displayed in the UI. */
    val title: String,
) {
    /**
     * Companion object holding predefined constant sequences.
     */
    companion object {
        /**
         * The standard sequence of steps required for a complete clinical photo series.
         */
        val STANDARD_STEPS =
            listOf(
                PhotoStep("front", "Front"),
                PhotoStep("front_ruler", "Front + Ruler"),
                PhotoStep("right", "Right Side"),
                PhotoStep("right_ruler", "Right Side + Ruler"),
                PhotoStep("back", "Back"),
                PhotoStep("back_ruler", "Back + Ruler"),
                PhotoStep("left", "Left Side"),
                PhotoStep("left_ruler", "Left Side + Ruler"),
            )
    }
}

/**
 * Represents the UI state for the Capture Screen workflow.
 */
data class CaptureUiState(
    /** The current photo step being requested. Null if uninitialized. */
    val currentStep: PhotoStep? = null,
    /** Total number of steps in the active sequence. */
    val totalSteps: Int = 0,
    /** True if the camera is currently capturing and saving an image. */
    val isCapturing: Boolean = false,
    /** Image data present during the Review phase (after snap, before confirm). */
    val reviewImageBytes: ByteArray? = null,
    /** Number of photos successfully captured and saved. */
    val capturedCount: Int = 0,
    /** True if the entire capture sequence has been completed. */
    val isFinished: Boolean = false,
) {
    /**
     * Compares this CaptureUiState instance to another object for equality.
     * Array content equality is properly handled for [reviewImageBytes].
     *
     * @param other The object to compare with.
     * @return True if both objects represent identical states.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CaptureUiState

        if (currentStep != other.currentStep) return false
        if (totalSteps != other.totalSteps) return false
        if (isCapturing != other.isCapturing) return false
        if (reviewImageBytes != null) {
            if (other.reviewImageBytes == null) return false
            if (!reviewImageBytes.contentEquals(other.reviewImageBytes)) return false
        } else if (other.reviewImageBytes != null) {
            return false
        }
        if (capturedCount != other.capturedCount) return false
        if (isFinished != other.isFinished) return false

        return true
    }

    /**
     * Generates a hash code for this CaptureUiState instance.
     * Properly includes the hash of the byte array if present.
     *
     * @return The hash code value.
     */
    override fun hashCode(): Int {
        var result = currentStep?.hashCode() ?: 0
        result = 31 * result + totalSteps
        result = 31 * result + isCapturing.hashCode()
        result = 31 * result + (reviewImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + capturedCount
        result = 31 * result + isFinished.hashCode()
        return result
    }
}
