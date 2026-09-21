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
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus
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

            // Front slot is pending -> displays "Capture"
            onNodeWithTag("CaptureSlotButton_item_front").assertIsDisplayed().performClick()
            assertEquals("item_front", singleClickedId)

            // Click start series button
            onNodeWithTag("StartFacialSeriesButton").assertIsDisplayed().performClick()
            assertTrue(seriesClicked)
        }
    }

    @Test
    fun testFacialSeriesCardControlReadOnly() {
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
            )

        runComposeUiTest {
            setContent {
                FacialSeriesCardControl(
                    title = "Read Only Series",
                    items = items,
                    answers = emptyMap(),
                    existingAttachments = emptyList(),
                    readOnly = true,
                    onCaptureSeries = {},
                    onCaptureSingle = {},
                )
            }
            waitForIdle()

            onNodeWithText("Read Only Series").assertIsDisplayed()
            onNodeWithText("0/3").assertIsDisplayed()
            onNodeWithTag("StartFacialSeriesButton").assertDoesNotExist()
            onNodeWithTag("CaptureSlotButton_item_left").assertDoesNotExist()
        }
    }

    @Test
    fun testFacialSeriesCardControlAllCompleted() {
        setAppLanguage("en")
        val items =
            listOf(
                Questionnaire.Item
                    .Builder(
                        linkId = str("item_1"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("Step 1") }
                    .build(),
                Questionnaire.Item
                    .Builder(
                        linkId = str("item_2"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("Step 2") }
                    .build(),
                Questionnaire.Item
                    .Builder(
                        linkId = str("item_3"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("Step 3") }
                    .build(),
            )

        val answers =
            mapOf(
                "item_1" to "img1.jpg",
                "item_2" to "img2.jpg",
                "item_3" to "img3.jpg",
            )

        runComposeUiTest {
            setContent {
                FacialSeriesCardControl(
                    title = "Completed Series",
                    items = items,
                    answers = answers,
                    existingAttachments = emptyList(),
                    readOnly = false,
                    onCaptureSeries = {},
                    onCaptureSingle = {},
                )
            }
            waitForIdle()

            onNodeWithText("Completed Series").assertIsDisplayed()
            onNodeWithText("3/3").assertIsDisplayed()
            // When all slots are completed, start series button is hidden
            onNodeWithTag("StartFacialSeriesButton").assertDoesNotExist()
        }
    }

    @Test
    fun testFacialSeriesCardControlExistingAttachmentsAndFallbacks() {
        setAppLanguage("en")
        val docWithRelated =
            DocumentReference(
                status = Enumeration(value = DocumentReferenceStatus.Current),
                content = emptyList(),
                context =
                    DocumentReference.Context(
                        related =
                            listOf(
                                Reference(reference = str("slot_existing").build()),
                            ),
                    ),
            )
        val docNoContext =
            DocumentReference(
                status = Enumeration(value = DocumentReferenceStatus.Current),
                content = emptyList(),
                context = null,
            )
        val docNoRelated =
            DocumentReference(
                status = Enumeration(value = DocumentReferenceStatus.Current),
                content = emptyList(),
                context = DocumentReference.Context(related = emptyList()),
            )

        val items =
            listOf(
                Questionnaire.Item
                    .Builder(
                        linkId = str("slot_null_text"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = null }
                    .build(),
                Questionnaire.Item
                    .Builder(
                        linkId = str("slot_blank_text"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("   ") }
                    .build(),
                Questionnaire.Item
                    .Builder(
                        linkId = str("slot_existing"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("Existing Doc") }
                    .build(),
                Questionnaire.Item
                    .Builder(
                        linkId = str("slot_extra_fallback"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("") }
                    .build(),
            )

        runComposeUiTest {
            setContent {
                FacialSeriesCardControl(
                    title = "Fallback & Attachments Series",
                    items = items,
                    answers = emptyMap(),
                    existingAttachments = listOf(docWithRelated, docNoContext, docNoRelated),
                    readOnly = false,
                    onCaptureSeries = {},
                    onCaptureSingle = {},
                )
            }
            waitForIdle()

            onNodeWithText("Fallback & Attachments Series").assertIsDisplayed()
            onNodeWithText("1/4").assertIsDisplayed()
            onNodeWithText("Existing Doc").assertIsDisplayed()
            onNodeWithText("View 3").assertIsDisplayed()
        }
    }

    @Test
    fun testFacialSlotTileDirect() {
        var clickedCaptured = false
        var clickedPending = false

        runComposeUiTest {
            setContent {
                androidx.compose.foundation.layout.Column {
                    FacialSlotTile(
                        slot = FacialSlotInfo("slot_c", "Captured Slot", isCaptured = true),
                        readOnly = false,
                        onCapture = { clickedCaptured = true },
                    )
                    FacialSlotTile(
                        slot = FacialSlotInfo("slot_p", "Pending Slot", isCaptured = false),
                        readOnly = false,
                        onCapture = { clickedPending = true },
                    )
                    FacialSlotTile(
                        slot = FacialSlotInfo("slot_ro", "Read-Only Slot", isCaptured = true),
                        readOnly = true,
                        onCapture = {},
                    )
                }
            }
            waitForIdle()

            onNodeWithText("Captured Slot").assertIsDisplayed()
            onNodeWithText("Pending Slot").assertIsDisplayed()
            onNodeWithText("Read-Only Slot").assertIsDisplayed()

            onNodeWithTag("CaptureSlotButton_slot_c").assertIsDisplayed().performClick()
            assertTrue(clickedCaptured)

            onNodeWithTag("CaptureSlotButton_slot_p").assertIsDisplayed().performClick()
            assertTrue(clickedPending)

            onNodeWithTag("CaptureSlotButton_slot_ro").assertDoesNotExist()
        }
    }
}
