/**
 * @file PatientDetailScreenJvmTest.kt
 * Contains declarations for PatientDetailScreenJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.fhir.model.r4.Patient
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PatientDetailScreenJvmTest {
    @Test
    fun testPatientDetailScreenJvm() =
        runComposeUiTest {
            val mockRepo = Mockito.mock(FhirRepository::class.java)

            val patient = Patient.Builder().apply { id = "patient-123" }.build()
            runTest {
                Mockito.`when`(mockRepo.getPatient("patient-123")).thenReturn(patient)
                Mockito.`when`(mockRepo.getEncountersForPatient("patient-123")).thenReturn(emptyList())
            }

            setContent {
                PatientDetailScreen(
                    patientId = "patient-123",
                    fhirRepository = mockRepo,
                    onBack = {},
                    onNewVisit = {},
                    onVisitSelected = {},
                )
            }
            onRoot().assertExists()
        }
}
