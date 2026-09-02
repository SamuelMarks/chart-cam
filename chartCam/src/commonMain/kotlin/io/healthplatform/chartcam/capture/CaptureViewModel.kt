/**
 * @file CaptureViewModel.kt
 * Contains declarations for CaptureViewModel.kt.
 *
 * Provides the ViewModel responsible for orchestrating the clinical photography workflow.
 */
package io.healthplatform.chartcam.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.files.FileStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the clinical photography workflow state machine.
 * Handles Camera interaction, step progression, and file persistence based on dynamically provided steps.
 *
 * @param cameraManager Wraps hardware camera calls to interface with device camera hardware.
 * @param fileStorage Handles filesystem IO operations to save captured photos.
 */
class CaptureViewModel(
    private val cameraManager: CameraManager,
    private val fileStorage: FileStorage,
) : ViewModel() {
    /** Internal mutable state flow for the Capture UI. */
    private val _uiState = MutableStateFlow(CaptureUiState())

    /** Publicly exposed immutable state flow for the Capture UI to observe. */
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    /** Stores the mapping of completed photo steps to their saved local file paths. */
    private val filePaths = mutableMapOf<PhotoStep, String>()

    /** The sequence of photo steps required for the current capture session. */
    private var stepsSequence = emptyList<PhotoStep>()

    /** The current index in the stepsSequence. */
    private var currentStepIndex = 0

    /**
     * Initializes the sequence of photos to be taken for this capture session.
     * Must be called before starting capture.
     *
     * @param steps The ordered list of photo steps required.
     */
    fun initSteps(steps: List<PhotoStep>) {
        if (steps.isNotEmpty() && stepsSequence.isEmpty()) {
            stepsSequence = steps
            currentStepIndex = 0
            _uiState.update {
                it.copy(
                    currentStep = steps.first(),
                    totalSteps = steps.size,
                )
            }
        } else if (steps.isEmpty()) {
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    /**
     * Triggered when the user taps the shutter button to capture an image.
     * Updates state to reflecting capture in progress and delegates to [cameraManager].
     */
    fun onCapture() {
        if (_uiState.value.isCapturing) return

        _uiState.update { it.copy(isCapturing = true) }

        viewModelScope.launch {
            try {
                val bytes = cameraManager.captureImage()
                if (bytes != null) {
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            reviewImageBytes = bytes,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isCapturing = false) }
                }
            } catch (e: IllegalStateException) {
                println("Capture error: ${e.message}")
                _uiState.update { it.copy(isCapturing = false) }
            } catch (e: IllegalArgumentException) {
                println("Capture error: ${e.message}")
                _uiState.update { it.copy(isCapturing = false) }
            }
        }
    }

    /**
     * Triggered when the user confirms the reviewed photo is satisfactory.
     * Saves the image file to storage, advances to the next step or finishes the sequence.
     */
    fun onConfirm() {
        val currentState = _uiState.value
        val bytes = currentState.reviewImageBytes ?: return
        val currentStep = currentState.currentStep ?: return

        try {
            // 1. Save File
            val fileName = "capture_${io.healthplatform.chartcam.utils.UUID.randomUUID()}_${currentStep.id}.jpg"
            val path = fileStorage.saveImage(fileName, bytes)
            filePaths[currentStep] = path

            // 2. Calculate Next Step
            currentStepIndex++
            val nextStep = if (currentStepIndex < stepsSequence.size) stepsSequence[currentStepIndex] else null

            if (nextStep != null) {
                _uiState.update {
                    it.copy(
                        currentStep = nextStep,
                        reviewImageBytes = null,
                        capturedCount = filePaths.size,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        reviewImageBytes = null,
                        isFinished = true,
                        capturedCount = filePaths.size,
                    )
                }
            }
        } catch (e: IllegalStateException) {
            println("Storage error: ${e.message}")
            _uiState.update { it.copy(reviewImageBytes = null) }
        } catch (e: IllegalArgumentException) {
            println("Argument error: ${e.message}")
            _uiState.update { it.copy(reviewImageBytes = null) }
        }
    }

    /**
     * Triggered when the user chooses to retake the photo from the Review screen.
     * Clears the current review image and resets to the capture state for the same step.
     */
    fun onRetake() {
        _uiState.update { it.copy(reviewImageBytes = null) }
    }

    /**
     * Retrieves the map of successfully captured and saved file paths.
     *
     * @return A map linking each completed [PhotoStep] to its local file path string.
     */
    fun getResultPaths(): Map<PhotoStep, String> = filePaths.toMap()
}
