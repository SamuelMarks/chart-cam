/**
 * @file EncounterDetailScreenJvmTest.kt
 * Contains declarations for EncounterDetailScreenJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.fhir.model.r4.Patient
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.navigation.PhotoSessionManager
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for EncounterDetailScreen on JVM.
 */
@OptIn(ExperimentalTestApi::class)
class EncounterDetailScreenJvmTest {
    /**
     * The compose rule.
     */
    @get:Rule
    val rule = createComposeRule()

    /**
     * Tests EncounterDetailScreen on JVM.
     */
    @Test
    fun testEncounterDetailScreenJvm() =
        runTest {
            val fhirRepository = mock(FhirRepository::class.java)
            val authRepository = mock(AuthRepository::class.java)
            val questionnaireRepository = mock(QuestionnaireRepository::class.java)
            val photoSessionManager = PhotoSessionManager()

            val practitioner =
                com.google.fhir.model.r4.Practitioner
                    .Builder()
                    .apply { id = "prac1" }
                    .build()
            `when`(authRepository.currentUser).thenReturn(MutableStateFlow(practitioner))
            `when`(authRepository.isDemoSession).thenReturn(MutableStateFlow(false))
            `when`(fhirRepository.getPatient("patient-1")).thenReturn(Patient.Builder().apply { id = "patient-1" }.build())

            val enc =
                io.healthplatform.chartcam.models.createFhirEncounter(
                    id = "enc-1",
                    patientId = "patient-1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )
            `when`(fhirRepository.getEncounter("enc-1")).thenReturn(enc)
            `when`(fhirRepository.getPhotosForEncounter("enc-1")).thenReturn(emptyList())
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter("enc-1")).thenReturn(emptyList())
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(emptyList())

            rule.setContent {
                EncounterDetailScreen(
                    patientId = "patient-1",
                    visitId = "enc-1",
                    dependencies =
                        EncounterDetailDependencies(
                            photoSessionManager = photoSessionManager,
                            fhirRepository = fhirRepository,
                            authRepository = authRepository,
                            questionnaireRepository = questionnaireRepository,
                        ),
                    actions =
                        EncounterDetailActions(
                            onBack = {},
                            onTakePhotos = { _, _ -> },
                            onFinalized = {},
                        ),
                )
            }

            // Let it load
            rule.waitForIdle()
        }

    /**
     * Tests PhotoGridItem rendering with load error state.
     */
    @Test
    fun testPhotoGridItem() =
        runTest {
            val doc =
                io.healthplatform.chartcam.models.createFhirDocumentReference(
                    io.healthplatform.chartcam.models.DocumentReferenceCreationParams(
                        id = "doc-test",
                        patientId = "pat-1",
                        encounterId = "enc-1",
                        dateStr = "2026-07-09T10:00:00Z",
                        desc = "Test Photo Description",
                        mime = "image/jpeg",
                        urlPath = "non_existent_file.jpg",
                    ),
                )

            rule.setContent {
                PhotoGridItem(doc)
            }
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Test Photo Description").assertExists()
        }

    /**
     * Tests PhotoGridItem clicking to open full photo review dialog and dismiss.
     */
    @Test
    fun testPhotoGridItemFullReviewDialog() =
        runTest {
            val storage = createFileStorage()
            val sampleBmp =
                byteArrayOf(
                    0x42,
                    0x4D,
                    0x1E,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x1A,
                    0x00,
                    0x00,
                    0x00,
                    0x0C,
                    0x00,
                    0x00,
                    0x00,
                    0x01,
                    0x00,
                    0x01,
                    0x00,
                    0x01,
                    0x00,
                    0x18,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                )
            val savedPath = storage.saveImage("test_encounter_photo.bmp", sampleBmp)

            val doc =
                io.healthplatform.chartcam.models.createFhirDocumentReference(
                    io.healthplatform.chartcam.models.DocumentReferenceCreationParams(
                        id = "doc-test-valid",
                        patientId = "pat-1",
                        encounterId = "enc-1",
                        dateStr = "2026-07-09T10:00:00Z",
                        desc = "Front View Photo",
                        mime = "image/bmp",
                        urlPath = savedPath,
                    ),
                )

            rule.setContent {
                PhotoGridItem(doc)
            }
            rule.waitForIdle()

            // Click the card
            rule.onNodeWithContentDescription("Front View Photo").performClick()
            rule.waitForIdle()

            // Verify dialog heading and dismiss button
            rule
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading).and(hasText("Front View Photo")))
                .assertExists()
            rule.onNodeWithText("Close").performClick()
            rule.waitForIdle()
        }

    /**
     * Tests PhotoGridItem opening DICOM Viewer.
     */
    @Test
    fun testPhotoGridItemOpenDicomViewer() =
        runTest {
            val storage = createFileStorage()
            val sampleBmp =
                byteArrayOf(
                    0x42,
                    0x4D,
                    0x1E,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x1A,
                    0x00,
                    0x00,
                    0x00,
                    0x0C,
                    0x00,
                    0x00,
                    0x00,
                    0x01,
                    0x00,
                    0x01,
                    0x00,
                    0x01,
                    0x00,
                    0x18,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                )
            val savedPath = storage.saveImage("test_dicom_trigger.bmp", sampleBmp)

            val doc =
                io.healthplatform.chartcam.models.createFhirDocumentReference(
                    io.healthplatform.chartcam.models.DocumentReferenceCreationParams(
                        id = "doc-dicom-valid",
                        patientId = "pat-1",
                        encounterId = "enc-1",
                        dateStr = "2026-07-09T10:00:00Z",
                        desc = "DICOM Target Photo",
                        mime = "image/bmp",
                        urlPath = savedPath,
                    ),
                )

            var openedPath: String? = null
            rule.setContent {
                PhotoGridItem(
                    doc = doc,
                    onOpenDicomViewer = { path -> openedPath = path },
                )
            }
            rule.waitForIdle()

            // Click the card
            rule.onNodeWithContentDescription("DICOM Target Photo").performClick()
            rule.waitForIdle()

            // Click DICOM Viewer button
            rule.onNodeWithText("DICOM Viewer").performClick()
            rule.waitForIdle()

            assertEquals(savedPath, openedPath)
        }
}
