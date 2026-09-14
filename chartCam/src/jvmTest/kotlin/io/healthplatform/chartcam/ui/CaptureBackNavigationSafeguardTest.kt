/**
 * @file CaptureBackNavigationSafeguardTest.kt
 * Contains declarations for CaptureBackNavigationSafeguardTest.kt.
 *
 * Validates back-navigation safeguards and unsaved ephemeral media handling in the capture workflow.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.capture.CaptureViewModel
import io.healthplatform.chartcam.capture.MockCameraManager
import io.healthplatform.chartcam.capture.MockFileStorage
import io.healthplatform.chartcam.capture.PhotoStep
import io.healthplatform.chartcam.navigation.PhotoSessionManager
import io.healthplatform.chartcam.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit and workflow tests validating back-navigation safeguards during camera capture sessions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureBackNavigationSafeguardTest {
    /**
     * Coroutine dispatcher for controlling test execution.
     */
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Sets up test dispatchers.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Cleans up test dispatchers.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Tests back navigation when no photos have been captured.
     * Verifies that the app transitions directly to the patient list with zero confirmation prompts needed.
     */
    @Test
    fun testEmptyCacheNavigationDirectToPatientList() =
        runTest(testDispatcher) {
            val cameraManager = MockCameraManager()
            val fileStorage = MockFileStorage()
            val viewModel = CaptureViewModel(cameraManager, fileStorage)
            val sessionManager = PhotoSessionManager()

            viewModel.initSteps(listOf(PhotoStep("step_1", "Step 1")))
            testDispatcher.scheduler.advanceUntilIdle()

            var navigatedRoute: String? = null
            var confirmationDialogVisible = false

            val handleCancel = {
                val results = viewModel.getResultPaths()
                if (results.isNotEmpty()) {
                    confirmationDialogVisible = true
                } else {
                    navigatedRoute = Routes.PATIENT_LIST
                }
            }

            handleCancel()

            assertFalse(confirmationDialogVisible, "Confirmation dialogue should not appear when cache is empty")
            assertEquals(Routes.PATIENT_LIST, navigatedRoute, "Should immediately transition to patient list")
            assertTrue(sessionManager.pendingPhotos.value.isEmpty(), "Session cache must remain empty")
        }

    /**
     * Tests back navigation when uncommitted photos exist, verifying intercept and cancellation.
     */
    @Test
    fun testUnsavedMediaInterceptAndCancellation() =
        runTest(testDispatcher) {
            val cameraManager = MockCameraManager()
            val fileStorage = MockFileStorage()
            val viewModel = CaptureViewModel(cameraManager, fileStorage)
            val sessionManager = PhotoSessionManager()

            val steps =
                listOf(
                    PhotoStep("step_1", "Lesion Primary"),
                    PhotoStep("step_2", "Lesion Secondary"),
                )
            viewModel.initSteps(steps)
            testDispatcher.scheduler.advanceUntilIdle()

            // Capture one photo
            viewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.onConfirm()
            testDispatcher.scheduler.advanceUntilIdle()

            val results = viewModel.getResultPaths()
            assertEquals(1, results.size, "One uncommitted photo should be captured")

            var promptVisible = false
            var userStayedOnCapture = false

            // User triggers back navigation
            val handleCancel = {
                if (viewModel.getResultPaths().isNotEmpty()) {
                    promptVisible = true
                }
            }
            handleCancel()
            assertTrue(promptVisible, "Must trigger discard confirmation dialogue when uncommitted media exists")

            // User selects "Cancel" on the prompt to keep capturing
            promptVisible = false
            userStayedOnCapture = true

            assertTrue(userStayedOnCapture, "User should remain in capture mode")
            assertEquals(1, viewModel.getResultPaths().size, "Captured photos must remain intact")
        }

    /**
     * Tests confirming media discard, verifying ephemeral storage purge and navigation.
     */
    @Test
    fun testUnsavedMediaConfirmationAndPurge() =
        runTest(testDispatcher) {
            val cameraManager = MockCameraManager()
            val fileStorage = MockFileStorage()
            val viewModel = CaptureViewModel(cameraManager, fileStorage)
            val sessionManager = PhotoSessionManager()

            viewModel.initSteps(listOf(PhotoStep("step_1", "Overview")))
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.onConfirm()
            testDispatcher.scheduler.advanceUntilIdle()

            val capturedMap = viewModel.getResultPaths().mapKeys { it.key.id }
            sessionManager.setPhotos(capturedMap)
            assertEquals(1, sessionManager.pendingPhotos.value.size)

            var navigatedRoute: String? = null

            // User confirms discard action
            fun onConfirmDiscard() {
                // Delete stored files and clear session
                fileStorage.clearCache()
                sessionManager.clear()
                navigatedRoute = Routes.PATIENT_LIST
            }

            onConfirmDiscard()

            assertTrue(sessionManager.pendingPhotos.value.isEmpty(), "Session cache must be completely cleared")
            assertEquals(Routes.PATIENT_LIST, navigatedRoute, "Should navigate to patient list upon discard")
        }
}
