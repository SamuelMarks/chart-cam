/**
 * @file PatientListScreenJvmTest.kt
 * Contains declarations for PatientListScreenJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test class for PatientListScreen on JVM.
 */
class PatientListScreenJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun patientListScreenDropdownMenuQuestionnairesOptionTriggersNavigation() =
        runComposeUiTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val fhirRepository = FhirRepository(ChartCamDatabase(driver))
            val authRepository = mock(AuthRepository::class.java)
            val exportImportService = mock(ExportImportService::class.java)

            val practitioner =
                Practitioner
                    .Builder()
                    .apply {
                        id = "test-id"
                    }.build()

            val flow = MutableStateFlow<Practitioner?>(practitioner)
            `when`(authRepository.currentUser).thenReturn(flow)

            var navigated = false
            val deps = PatientListDependencies(fhirRepository, exportImportService, authRepository)
            val acts =
                PatientListActions(
                    onPatientSelected = {},
                    onNavigateToQuestionnaires = { navigated = true },
                    onLogout = {},
                )

            setContent {
                PatientListScreen(dependencies = deps, actions = acts)
            }
            waitForIdle()

            onNodeWithContentDescription("More options").performClick()

            onAllNodesWithText("Questionnaires")[0].performClick()

            assertTrue(navigated)
            driver.close()
        }
}
