/**
 * Tests multi-photo workflows.
 */
package io.healthplatform.chartcam

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.capture.CaptureViewModel
import io.healthplatform.chartcam.capture.MockCameraManager
import io.healthplatform.chartcam.capture.MockFileStorage
import io.healthplatform.chartcam.capture.PhotoStep
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.storage.createSecureStorage
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import io.healthplatform.chartcam.viewmodel.WidgetType
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

/**
 * E2E tests for verifying multi-photo workflows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MultiplePhotosE2ETest {
    /** Standard test dispatcher. */
    private val testDispatcher = StandardTestDispatcher()

    /** Setup for tests. */
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        (createSecureStorage() as JvmSecureStorage).clearAll()
    }

    /** Teardown for tests. */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Validates that creating a multi-photo questionnaire and capturing photos advances correctly.
     */
    @Test
    fun testCreateAndCaptureMultiplePhotos() =
        runTest(testDispatcher) {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val fhirRepository = FhirRepository(ChartCamDatabase(driver))
            val questionnaireRepository = QuestionnaireRepository()

            // 1. Create a questionnaire using the Form Builder
            val builderViewModel = QuestionnaireBuilderViewModel(questionnaireRepository)
            builderViewModel.updateTitle("Multi Photo Form")

            // Add photo 1
            builderViewModel.addItem(WidgetType.PHOTO_CAMERA)
            builderViewModel.updateItem("item_1", "Photo 1 Label", emptyList())

            // Add photo 2
            builderViewModel.addItem(WidgetType.PHOTO_CAMERA)
            builderViewModel.updateItem("item_2", "Photo 2 Label", emptyList())

            val customFormId = builderViewModel.saveQuestionnaire()
            val q = questionnaireRepository.getQuestionnaire(customFormId)
            assertEquals(2, q?.item?.size)

            // 2. Simulate capturing photos via CaptureViewModel
            val captureViewModel = CaptureViewModel(MockCameraManager(), MockFileStorage())
            val steps =
                q?.item?.filter { it.type.value == com.google.fhir.model.r4.Questionnaire.QuestionnaireItemType.Attachment }?.map {
                    PhotoStep(it.linkId.value ?: "", it.text?.value ?: "")
                } ?: emptyList()

            captureViewModel.initSteps(steps)
            assertEquals(
                "Photo 1 Label",
                captureViewModel.uiState.value.currentStep
                    ?.title,
            )

            // Take photo 1
            captureViewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            captureViewModel.onConfirm()

            // Should advance to photo 2
            assertEquals(
                "Photo 2 Label",
                captureViewModel.uiState.value.currentStep
                    ?.title,
            )

            // Take photo 2
            captureViewModel.onCapture()
            testDispatcher.scheduler.advanceUntilIdle()
            captureViewModel.onConfirm()

            assertEquals(true, captureViewModel.uiState.value.isFinished)
        }
}
