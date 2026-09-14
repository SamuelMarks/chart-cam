/**
 * @file TriageScreenJvmTest.kt
 * Contains declarations for TriageScreenJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.datetime.LocalDate
import org.mockito.Mockito
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test class for TriageScreen on JVM.
 */
@OptIn(ExperimentalTestApi::class)
class TriageScreenJvmTest {
    /**
     * Test triage screen on JVM.
     */
    @Test
    fun testTriageScreenJvm() =
        runComposeUiTest {
            val mockRepo = Mockito.mock(FhirRepository::class.java)

            setContent {
                TriageScreen(
                    capturedPhotoPaths = emptyMap(),
                    fhirRepository = mockRepo,
                    onProceedToEncounter = { _, _ -> },
                )
            }

            onRoot().assertExists()
        }

    /**
     * Verifies that TriagePatientSelectionHeader merges descendants into a single accessible clickable node.
     */
    @Test
    fun testTriagePatientSelectionHeaderMergedSemantics() =
        runComposeUiTest {
            val patient = createFhirPatient("p1", "John", "Doe", LocalDate(1990, 1, 1), "MRN-1")
            var proceedClicked = false

            setContent {
                TriagePatientSelectionHeader(
                    patient = patient,
                    photoCount = 2,
                    onProceed = { proceedClicked = true },
                )
            }

            waitForIdle()
            onNodeWithText("Doe, John").assertExists().performClick()
            assertTrue(proceedClicked)
        }
}
