/**
 * @file CaptureStateTest.kt
 * Contains declarations for CaptureStateTest.kt.
 */
package io.healthplatform.chartcam.capture

import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.photo_step_back
import chartcam.chartcam.generated.resources.photo_step_front
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for UI states, capture steps, and structured capture error models.
 */
class CaptureStateTest {
    /**
     * Validates [PhotoStep] equivalence, predefined sizes, and data class methods.
     */
    @Test
    fun testPhotoStep() {
        val step1 = PhotoStep("1", "Title 1")
        val step2 = PhotoStep("1", "Title 1")
        val stepDiffId = PhotoStep("2", "Title 1")
        val stepDiffTitle = PhotoStep("1", "Title 2")
        val stepWithRes = PhotoStep("1", "Title 1", Res.string.photo_step_front)

        assertEquals(step1, step2)
        assertNotEquals(step1, stepDiffId)
        assertNotEquals(step1, stepDiffTitle)
        assertNotEquals(step1, stepWithRes)
        assertFalse(step1.equals(null))
        assertFalse(step1.equals("different type"))
        assertEquals(step1.hashCode(), step2.hashCode())
        assertTrue(step1.toString().contains("Title 1"))

        val copied = step1.copy(id = "3", title = "Title 3")
        assertEquals("3", copied.id)
        assertEquals("Title 3", copied.title)
        assertNull(copied.titleRes)

        assertEquals("1", step1.component1())
        assertEquals("Title 1", step1.component2())
        assertNull(step1.component3())
        assertEquals(Res.string.photo_step_front, stepWithRes.component3())

        assertEquals(8, PhotoStep.STANDARD_STEPS.size)
        PhotoStep.STANDARD_STEPS.forEach { step ->
            assertNotNull(step.titleRes)
        }
    }

    /**
     * Validates [CaptureError] sealed hierarchy models.
     */
    @Test
    fun testCaptureErrors() {
        val emptyImg = CaptureError.EmptyImage
        val saveFailed = CaptureError.SaveFailed
        val camFailed1 = CaptureError.CameraFailed("Lens error")
        val camFailed2 = CaptureError.CameraFailed("Lens error")
        val camFailed3 = CaptureError.CameraFailed("Sensor error")

        assertEquals(camFailed1, camFailed2)
        assertNotEquals(camFailed1, camFailed3)
        assertEquals("Lens error", camFailed1.detail)
        assertEquals("Lens error", camFailed1.component1())
        assertEquals("CameraFailed(detail=Lens error)", camFailed1.toString())
        assertEquals(camFailed1.hashCode(), camFailed2.hashCode())

        assertNotEquals<CaptureError>(emptyImg, saveFailed)
        assertNotEquals<CaptureError>(emptyImg, camFailed1)
        assertEquals(emptyImg, CaptureError.EmptyImage)
        assertEquals(saveFailed, CaptureError.SaveFailed)
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

        // Identity check
        assertEquals(state1, state1)
        assertFalse(state1.equals(null))
        assertFalse(state1.equals("different type"))

        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())

        assertNotEquals(state1, state3)
        assertNotEquals(state1, state4)
        assertNotEquals(state1, state5)
        assertNotEquals(state5, state1)

        // Coverage for scalar fields
        assertNotEquals(CaptureUiState(totalSteps = 1), CaptureUiState(totalSteps = 2))
        assertNotEquals(CaptureUiState(isCapturing = true), CaptureUiState(isCapturing = false))
        assertNotEquals(CaptureUiState(capturedCount = 1), CaptureUiState(capturedCount = 2))
        assertNotEquals(CaptureUiState(isFinished = true), CaptureUiState(isFinished = false))
        assertNotEquals(
            CaptureUiState(silhouetteType = io.healthplatform.chartcam.camera.SilhouetteType.FRONTAL_FACE),
            CaptureUiState(silhouetteType = io.healthplatform.chartcam.camera.SilhouetteType.NONE),
        )
        assertNotEquals(
            CaptureUiState(ghostImageBytes = byteArrayOf(1, 2)),
            CaptureUiState(ghostImageBytes = byteArrayOf(3, 4)),
        )
        assertNotEquals(
            CaptureUiState(ghostImageBytes = byteArrayOf(1, 2)),
            CaptureUiState(ghostImageBytes = null),
        )

        // Coverage for error and payload fields
        val stateErr1 = CaptureUiState(error = CaptureError.EmptyImage)
        val stateErr2 = CaptureUiState(error = CaptureError.SaveFailed)
        val stateErrNone = CaptureUiState(error = null)
        assertNotEquals(stateErr1, stateErr2)
        assertNotEquals(stateErr1, stateErrNone)

        val stateMsg1 = CaptureUiState(errorMessage = "Err 1")
        val stateMsg2 = CaptureUiState(errorMessage = "Err 2")
        val stateMsgNone = CaptureUiState(errorMessage = null)
        assertNotEquals(stateMsg1, stateMsg2)
        assertNotEquals(stateMsg1, stateMsgNone)

        val stateRes1 = CaptureUiState(errorMessageResource = Res.string.photo_step_front)
        val stateRes2 = CaptureUiState(errorMessageResource = Res.string.photo_step_back)
        val stateResNone = CaptureUiState(errorMessageResource = null)
        assertNotEquals(stateRes1, stateRes2)
        assertNotEquals(stateRes1, stateResNone)

        // HashCode calculation with all non-null fields
        val fullState =
            CaptureUiState(
                currentStep = PhotoStep("1", "A"),
                totalSteps = 5,
                isCapturing = true,
                reviewImageBytes = byteArrayOf(1, 2, 3),
                capturedCount = 2,
                isFinished = false,
                error = CaptureError.SaveFailed,
                errorMessage = "Failed",
                errorMessageResource = Res.string.photo_step_front,
                silhouetteType = io.healthplatform.chartcam.camera.SilhouetteType.FRONTAL_FACE,
                ghostImageBytes = byteArrayOf(9, 8, 7),
            )
        assertNotNull(fullState.hashCode())

        // HashCode calculation with default/null fields
        val defaultState = CaptureUiState()
        assertNotNull(defaultState.hashCode())

        // Destructuring components
        assertEquals(fullState.currentStep, fullState.component1())
        assertEquals(fullState.totalSteps, fullState.component2())
        assertEquals(fullState.isCapturing, fullState.component3())
        assertEquals(fullState.reviewImageBytes, fullState.component4())
        assertEquals(fullState.capturedCount, fullState.component5())
        assertEquals(fullState.isFinished, fullState.component6())
        assertEquals(fullState.error, fullState.component7())
        assertEquals(fullState.errorMessage, fullState.component8())
        assertEquals(fullState.errorMessageResource, fullState.component9())
        assertEquals(fullState.silhouetteType, fullState.component10())
        assertEquals(fullState.ghostImageBytes, fullState.component11())

        // Copy method
        val copied = fullState.copy(isFinished = true)
        assertTrue(copied.isFinished)
    }
}
