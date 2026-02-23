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
 * ViewModel managing the clinical photography workflow.
 * Handles Camera interaction, ghosting logic, and file persistence based on dynamically provided steps.
 *
 * @property cameraManager Wraps hardware camera calls.
 * @property fileStorage Handles IO.
 */
class CaptureViewModel(
    private val cameraManager: CameraManager,
    private val fileStorage: FileStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val filePaths = mutableMapOf<PhotoStep, String>()
    private var stepsSequence = emptyList<PhotoStep>()

    /**
     * Initializes the sequence of photos to be taken.
     * @param steps The list of photo steps required.
     */
    fun initSteps(steps: List<PhotoStep>) {
        if (steps.isNotEmpty() && stepsSequence.isEmpty()) {
            stepsSequence = steps
            _uiState.update { 
                it.copy(
                    currentStep = steps.first(),
                    totalSteps = steps.size
                )
            }
        } else if (steps.isEmpty()) {
             _uiState.update { it.copy(isFinished = true) }
        }
    }

    /**
     * Triggered when user taps the shutter button.
     */
    fun onCapture() {
        if (_uiState.value.isCapturing) return
        
        _uiState.update { it.copy(isCapturing = true) }
        
        viewModelScope.launch {
            val bytes = cameraManager.captureImage()
            if (bytes != null) {
                _uiState.update { 
                    it.copy(
                        isCapturing = false,
                        reviewImageBytes = bytes
                    ) 
                }
            } else {
                _uiState.update { it.copy(isCapturing = false) }
            }
        }
    }

    /**
     * Triggered when user confirms the reviewed photo.
     * Saves file, updates steps, manages ghost reference.
     */
    fun onConfirm() {
        val currentState = _uiState.value
        val bytes = currentState.reviewImageBytes ?: return
        val currentStep = currentState.currentStep ?: return

        // 1. Save File
        val fileName = "capture_${currentStep.id}.jpg"
        val path = fileStorage.saveImage(fileName, bytes)
        filePaths[currentStep] = path

        // 2. Calculate Next Step
        val currentIndex = stepsSequence.indexOf(currentStep)
        val nextStep = if (currentIndex + 1 < stepsSequence.size) stepsSequence[currentIndex + 1] else null
        
        val nextGhostBytes = if (nextStep != null && nextStep.isRuler) {
            bytes
        } else {
            null
        }

        if (nextStep != null) {
            _uiState.update {
                it.copy(
                    currentStep = nextStep,
                    reviewImageBytes = null,
                    ghostImageBytes = nextGhostBytes,
                    capturedCount = filePaths.size
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    reviewImageBytes = null,
                    isFinished = true,
                    capturedCount = filePaths.size
                )
            }
        }
    }

    /**
     * Triggered when user Retakes from the Review screen.
     */
    fun onRetake() {
        _uiState.update { it.copy(reviewImageBytes = null) }
    }

    /**
     * Returns the map of captured file paths.
     */
    fun getResultPaths(): Map<PhotoStep, String> = filePaths.toMap()
}
