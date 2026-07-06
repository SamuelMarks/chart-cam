package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import org.mockito.Mockito
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PatientListScreenTest {
    @Test
    fun testPatientListScreen() =
        runComposeUiTest {
            val mockFhirRepo = Mockito.mock(FhirRepository::class.java)
            val mockExportRepo = Mockito.mock(ExportImportService::class.java)
            val mockAuthRepo = Mockito.mock(AuthRepository::class.java)

            setContent {
                PatientListScreen(
                    fhirRepository = mockFhirRepo,
                    exportImportService = mockExportRepo,
                    authRepository = mockAuthRepo,
                    onPatientSelected = {},
                    onLogout = {},
                )
            }

            onRoot().assertExists()
        }
}
