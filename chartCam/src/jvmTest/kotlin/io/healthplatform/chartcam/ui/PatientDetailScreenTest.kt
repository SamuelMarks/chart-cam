package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import io.healthplatform.chartcam.repository.FhirRepository
import org.mockito.Mockito
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PatientDetailScreenTest {
    @Test
    fun testPatientDetailScreen() =
        runComposeUiTest {
            val mockRepo = Mockito.mock(FhirRepository::class.java)

            setContent {
                PatientDetailScreen(
                    patientId = "test-123",
                    fhirRepository = mockRepo,
                    onBack = {},
                    onNewVisit = {},
                    onVisitSelected = {},
                )
            }

            onRoot().assertExists()
        }
}
