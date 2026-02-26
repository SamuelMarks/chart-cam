package io.healthplatform.chartcam.capture

/**
 * The specific angle/type of photo required in the clinical sequence.
 * Order matters for the state machine progression.
 *
 * @property title Human readable name.
 */
data class PhotoStep(
    val id: String,
    val title: String
) {
    companion object {
        val STANDARD_STEPS = listOf(
            PhotoStep("front", "Front"),
            PhotoStep("front_ruler", "Front + Ruler"),
            PhotoStep("right", "Right Side"),
            PhotoStep("right_ruler", "Right Side + Ruler"),
            PhotoStep("back", "Back"),
            PhotoStep("back_ruler", "Back + Ruler"),
            PhotoStep("left", "Left Side"),
            PhotoStep("left_ruler", "Left Side + Ruler")
        )
    }
}

/**
 * UI State for the Capture Screen.
 *
 * @property currentStep The current photo being requested.
 * @property totalSteps Total number of steps in the sequence.
 * @property isCapturing Loading state during IO.
 * @property reviewImageBytes Image data present during the Review phase (after snap, before confirm).
 * @property capturedCount Number of photos successfully saved.
 * @property isFinished Whether the sequence is complete.
 */
data class CaptureUiState(
    val currentStep: PhotoStep? = null,
    val totalSteps: Int = 0,
    val isCapturing: Boolean = false,
    val reviewImageBytes: ByteArray? = null,
    val capturedCount: Int = 0,
    val isFinished: Boolean = false
) {
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
        } else if (other.reviewImageBytes != null) return false
        if (capturedCount != other.capturedCount) return false
        if (isFinished != other.isFinished) return false

        return true
    }

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