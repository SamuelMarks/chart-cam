/**
 * @file CaptureState.kt
 * Contains declarations for CaptureState.kt.
 *
 * Contains data structures defining the state and steps for the clinical photo capture workflow.
 */
package io.healthplatform.chartcam.capture

import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.photo_step_back
import chartcam.chartcam.generated.resources.photo_step_back_ruler
import chartcam.chartcam.generated.resources.photo_step_front
import chartcam.chartcam.generated.resources.photo_step_front_ruler
import chartcam.chartcam.generated.resources.photo_step_left
import chartcam.chartcam.generated.resources.photo_step_left_ruler
import chartcam.chartcam.generated.resources.photo_step_right
import chartcam.chartcam.generated.resources.photo_step_right_ruler
import org.jetbrains.compose.resources.StringResource

/**
 * Represents structured errors that can occur during the camera capture workflow.
 */
sealed interface CaptureError {
    /** Captured image is empty or was interrupted. */
    data object EmptyImage : CaptureError

    /** Failed to save photo: storage is full or disk error occurred. */
    data object SaveFailed : CaptureError

    /**
     * Camera capture failed with an underlying message.
     *
     * @property detail The error detail message provided by the camera manager.
     */
    data class CameraFailed(
        val detail: String,
    ) : CaptureError
}

/**
 * Represents a specific angle or type of photo required in the clinical sequence.
 * The order of these steps dictates the state machine progression during capture.
 *
 * @property id Unique identifier for this step.
 * @property title Human readable name displayed in the UI.
 * @property titleRes Optional localized string resource for standard steps.
 */
data class PhotoStep(
    val id: String,
    val title: String,
    val titleRes: StringResource? = null,
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
                PhotoStep("front", "Front", Res.string.photo_step_front),
                PhotoStep("front_ruler", "Front + Ruler", Res.string.photo_step_front_ruler),
                PhotoStep("right", "Right Side", Res.string.photo_step_right),
                PhotoStep("right_ruler", "Right Side + Ruler", Res.string.photo_step_right_ruler),
                PhotoStep("back", "Back", Res.string.photo_step_back),
                PhotoStep("back_ruler", "Back + Ruler", Res.string.photo_step_back_ruler),
                PhotoStep("left", "Left Side", Res.string.photo_step_left),
                PhotoStep("left_ruler", "Left Side + Ruler", Res.string.photo_step_left_ruler),
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
    /** Structured error model representing failure states during capture or disk operations. */
    val error: CaptureError? = null,
    /** Error message to display to the user if capture or disk operations fail. */
    val errorMessage: String? = null,
    /** Localized error resource to display to the user if capture or disk operations fail. */
    val errorMessageResource: StringResource? = null,
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
        if (other !is CaptureUiState) return false
        return hasSameScalarFields(other) && hasSamePayloadFields(other)
    }

    /**
     * Checks if scalar fields of two states match.
     *
     * @param other The state to compare with.
     * @return True if scalar fields match.
     */
    private fun hasSameScalarFields(other: CaptureUiState): Boolean =
        currentStep == other.currentStep &&
            totalSteps == other.totalSteps &&
            isCapturing == other.isCapturing &&
            capturedCount == other.capturedCount &&
            isFinished == other.isFinished

    /**
     * Checks if payload and error fields of two states match.
     *
     * @param other The state to compare with.
     * @return True if payload fields match.
     */
    private fun hasSamePayloadFields(other: CaptureUiState): Boolean =
        error == other.error &&
            errorMessage == other.errorMessage &&
            errorMessageResource == other.errorMessageResource &&
            reviewImageBytes.contentEquals(other.reviewImageBytes)

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
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (errorMessageResource?.hashCode() ?: 0)
        return result
    }
}
