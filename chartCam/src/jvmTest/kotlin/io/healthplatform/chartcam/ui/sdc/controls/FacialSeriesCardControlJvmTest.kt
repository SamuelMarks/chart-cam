/**
 * @file FacialSeriesCardControlJvmTest.kt
 * Contains UI tests for FacialSeriesCardControl on JVM.
 */
package io.healthplatform.chartcam.ui.sdc.controls

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * UI tests for FacialSeriesCardControl.
 */
@OptIn(ExperimentalTestApi::class)
class FacialSeriesCardControlJvmTest {
    private fun str(s: String) = FhirString.Builder().apply { value = s }

    @Test
    fun testFacialSeriesCardControlInteraction() {
        setAppLanguage("en")
        val items =
            listOf(
                Questionnaire.Item
                    .Builder(
                        linkId = str("item_left"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("Left Profile") }
                    .build(),
                Questionnaire.Item
                    .Builder(
                        linkId = str("item_front"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("Front View") }
                    .build(),
                Questionnaire.Item
                    .Builder(
                        linkId = str("item_right"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("Right Profile") }
                    .build(),
            )

        var seriesClicked = false
        var singleClickedId: String? = null

        runComposeUiTest {
            setContent {
                FacialSeriesCardControl(
                    title = "Facial Series Test",
                    items = items,
                    answers = mapOf("item_left" to "path/to/left.jpg"),
                    existingAttachments = emptyList(),
                    readOnly = false,
                    onCaptureSeries = { seriesClicked = true },
                    onCaptureSingle = { singleClickedId = it },
                )
            }
            waitForIdle()

            onNodeWithText("Facial Series Test").assertIsDisplayed()
            onNodeWithText("1/3").assertIsDisplayed()

            // Left slot is captured -> displays "Retake"
            onNodeWithTag("CaptureSlotButton_item_left").assertIsDisplayed().performClick()
            assertEquals("item_left", singleClickedId)

            // Click start series button
            onNodeWithTag("StartFacialSeriesButton").assertIsDisplayed().performClick()
            assertTrue(seriesClicked)
        }
    }
}
