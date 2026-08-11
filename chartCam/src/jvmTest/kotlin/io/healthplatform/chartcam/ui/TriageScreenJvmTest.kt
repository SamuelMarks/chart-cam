/**
 * @file TriageScreenJvmTest.kt
 * Contains declarations for TriageScreenJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.repository.FhirRepository
import org.mockito.Mockito
import kotlin.test.Test

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
}
