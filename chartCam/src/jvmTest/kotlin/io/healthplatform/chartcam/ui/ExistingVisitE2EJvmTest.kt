/**
 * @file ExistingVisitE2EJvmTest.kt
 * Contains declarations for ExistingVisitE2EJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.navigation.PhotoSessionManager
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ExistingVisitE2EJvmTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testExistingVisitLockedQuestionnaire() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val fhirRepository = FhirRepository(ChartCamDatabase(driver))
            val questionnaireRepository = QuestionnaireRepository()
            questionnaireRepository.loadDefaultForms()

            val authRepository = mock(AuthRepository::class.java)
            val practitioner =
                com.google.fhir.model.r4.Practitioner
                    .Builder()
                    .apply { id = "prac1" }
                    .build()
            `when`(authRepository.currentUser).thenReturn(MutableStateFlow(practitioner))

            val syncWorker = mock(SyncWorker::class.java)
            val photoSessionManager = PhotoSessionManager()

            // Create Patient
            val patient =
                io.healthplatform.chartcam.models.createFhirPatient(
                    id = "patient-1",
                    firstName = "John",
                    lastName = "Doe",
                    dob = kotlinx.datetime.LocalDate(1980, 1, 1),
                    mrnValue = "MRN123",
                )
            fhirRepository.savePatient(patient)

            // Create Encounter
            val enc =
                io.healthplatform.chartcam.models
                    .createFhirEncounter(
                        id = "enc-1",
                        patientId = "patient-1",
                        practitionerId = "prac1",
                        dateStr = "2026-07-09",
                    ).toBuilder()
                    .apply {
                        status =
                            com.google.fhir.model.r4
                                .Enumeration(value = com.google.fhir.model.r4.Encounter.EncounterStatus.Finished)
                    }.build()
            fhirRepository.saveEncounter(enc)

            // Add a QuestionnaireResponse to simulate a completed form
            val qr =
                com.google.fhir.model.r4.QuestionnaireResponse
                    .Builder(
                        com.google.fhir.model.r4.Enumeration(
                            value = com.google.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus.Completed,
                        ),
                    ).apply {
                        id = "qr-1"
                        encounter =
                            com.google.fhir.model.r4.Reference.Builder().apply {
                                reference =
                                    com.google.fhir.model.r4.String
                                        .Builder()
                                        .apply { value = "Encounter/enc-1" }
                            }
                        questionnaire =
                            com.google.fhir.model.r4.Canonical
                                .Builder()
                                .apply { value = "Questionnaire/std-form" }
                    }.build()
            fhirRepository.saveQuestionnaireResponse(qr)

            rule.setContent {
                EncounterDetailScreen(
                    patientId = "patient-1",
                    visitId = "enc-1",
                    dependencies =
                        EncounterDetailDependencies(
                            photoSessionManager = photoSessionManager,
                            fhirRepository = fhirRepository,
                            authRepository = authRepository,
                            syncWorker = syncWorker,
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

            rule.waitForIdle()

            // The Dropdown element should NOT exist
            rule.onNodeWithContentDescription("Questionnaire Selector").assertDoesNotExist()

            // The Text displaying the locked questionnaire title SHOULD exist
            rule.onNodeWithText("Questionnaire: Standard Clinical Photo", substring = true).assertIsDisplayed()
        }
}
