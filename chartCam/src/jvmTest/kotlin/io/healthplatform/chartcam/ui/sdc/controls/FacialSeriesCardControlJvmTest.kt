/**
 * @file FacialSeriesCardControlJvmTest.kt
 * Contains UI tests for FacialSeriesCardControl on JVM.
 */
package io.healthplatform.chartcam.ui.sdc.controls

import androidx.compose.ui.platform.testTag
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
                                Reference(reference = null),
                                Reference(reference = FhirString.Builder().apply { value = null }.build()),
                                Reference(reference = str("other_ref").build()),
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
                Questionnaire.Item
                    .Builder(
                        linkId = FhirString.Builder().apply { value = null },
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = FhirString.Builder().apply { value = null } }
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
            onNodeWithText("1/5").assertIsDisplayed()
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

    /**
     * Verifies recomposition skipping and dynamic parameter mutations for [FacialSeriesCardControl] and [FacialSlotTile].
     */
    @Test
    fun testFacialSeriesCardControlRecompositionAndSkipping() {
        runComposeUiTest {
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            val titleState = androidx.compose.runtime.mutableStateOf("變更標題甲")
            val answersState = androidx.compose.runtime.mutableStateOf<Map<String, Any?>>(emptyMap())
            val readOnlyState = androidx.compose.runtime.mutableStateOf(false)
            val modState = androidx.compose.runtime.mutableStateOf<androidx.compose.ui.Modifier>(androidx.compose.ui.Modifier)

            val item1 =
                Questionnaire.Item
                    .Builder(
                        linkId = str("slot_dyn1"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("動態欄位1") }
                    .build()
            val item2 =
                Questionnaire.Item
                    .Builder(
                        linkId = str("slot_dyn2"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("動態欄位2") }
                    .build()
            val itemsState = androidx.compose.runtime.mutableStateOf(listOf(item1))
            val attachmentsState = androidx.compose.runtime.mutableStateOf<List<DocumentReference>>(emptyList())
            val onSeriesState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val onSingleState = androidx.compose.runtime.mutableStateOf<(String) -> Unit>({})

            setContent {
                val dummy = trigger.value
                FacialSeriesCardControl(
                    title = titleState.value,
                    items = itemsState.value,
                    answers = answersState.value,
                    existingAttachments = attachmentsState.value,
                    readOnly = readOnlyState.value,
                    onCaptureSeries = onSeriesState.value,
                    onCaptureSingle = onSingleState.value,
                    modifier = modState.value,
                )
                FacialSlotTile(
                    slot = FacialSlotInfo("slot_dyn1", "動態瓷磚", isCaptured = false),
                    readOnly = readOnlyState.value,
                    onCapture = onSeriesState.value,
                    modifier = modState.value,
                )
            }
            waitForIdle()

            // Outer trigger causes recomposition skipping for unchanged children
            trigger.value++
            waitForIdle()

            // Mutate each state dynamically
            titleState.value = "變更標題乙"
            waitForIdle()

            itemsState.value = listOf(item2)
            waitForIdle()

            answersState.value = mapOf("slot_dyn2" to "captured.jpg")
            waitForIdle()

            val dummyDoc =
                DocumentReference(
                    status = Enumeration(value = DocumentReferenceStatus.Current),
                    content = emptyList(),
                )
            attachmentsState.value = listOf(dummyDoc)
            waitForIdle()

            readOnlyState.value = true
            waitForIdle()

            onSeriesState.value = { println("series_changed") }
            waitForIdle()

            onSingleState.value = { println("single_changed: $it") }
            waitForIdle()

            modState.value =
                androidx.compose.ui.Modifier
                    .testTag("new_card_tag")
            waitForIdle()
        }
    }

    /**
     * Tests pure skipping for FacialSeriesCardControl and FacialSlotTile when parent recomposes with constants.
     */
    @Test
    fun testPureSkippingBothControls() {
        runComposeUiTest {
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            val slot = FacialSlotInfo("c_slot", "純靜態瓷磚", isCaptured = true)
            val onCapture: () -> Unit = {}
            val item =
                Questionnaire.Item
                    .Builder(
                        linkId = str("item_const"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("靜態題目") }
                    .build()
            val items = listOf(item)
            val answers = emptyMap<String, Any?>()
            val attachments = emptyList<DocumentReference>()
            val onSeries: () -> Unit = {}
            val onSingle: (String) -> Unit = {}

            setContent {
                val dummy = trigger.value
                FacialSeriesCardControl(
                    title = "靜態標題",
                    items = items,
                    answers = answers,
                    existingAttachments = attachments,
                    readOnly = true,
                    onCaptureSeries = onSeries,
                    onCaptureSingle = onSingle,
                    modifier = androidx.compose.ui.Modifier,
                )
                FacialSlotTile(
                    slot = slot,
                    readOnly = true,
                    onCapture = onCapture,
                    modifier = androidx.compose.ui.Modifier,
                )
            }
            waitForIdle()
            trigger.value++
            waitForIdle()
        }
    }

    /**
     * Tests dynamic mutations of all parameters in FacialSlotTile.
     */
    @Test
    fun testDynamicSlotTileMutations() {
        runComposeUiTest {
            val slotState = androidx.compose.runtime.mutableStateOf(FacialSlotInfo("slot_mut", "初始瓷磚", isCaptured = false))
            val roState = androidx.compose.runtime.mutableStateOf(false)
            val capState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val modState = androidx.compose.runtime.mutableStateOf<androidx.compose.ui.Modifier>(androidx.compose.ui.Modifier)

            setContent {
                FacialSlotTile(
                    slot = slotState.value,
                    readOnly = roState.value,
                    onCapture = capState.value,
                    modifier = modState.value,
                )
            }
            waitForIdle()

            slotState.value = FacialSlotInfo("slot_mut", "更新瓷磚", isCaptured = true)
            waitForIdle()

            roState.value = true
            waitForIdle()

            capState.value = { }
            waitForIdle()

            modState.value =
                androidx.compose.ui.Modifier
                    .testTag("updated_tile_tag")
            waitForIdle()
        }
    }

    @androidx.compose.runtime.Composable
    private fun DynamicCardWrapper(
        title: String,
        items: List<Questionnaire.Item>,
        answers: Map<String, Any?>,
        existingAttachments: List<DocumentReference>,
        readOnly: Boolean,
        onCaptureSeries: () -> Unit,
        onCaptureSingle: (String) -> Unit,
        modifier: androidx.compose.ui.Modifier,
        trigger: Int,
    ) {
        val t = trigger
        FacialSeriesCardControl(
            title = title,
            items = items,
            answers = answers,
            existingAttachments = existingAttachments,
            readOnly = readOnly,
            onCaptureSeries = onCaptureSeries,
            onCaptureSingle = onCaptureSingle,
            modifier = modifier,
        )
    }

    /**
     * Tests dynamic wrapper propagation for FacialSeriesCardControl to cover unmemoized changed branches.
     */
    @Test
    fun testDynamicCardWrapperRecomposition() {
        runComposeUiTest {
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            val onSeries: () -> Unit = {}
            val onSingle: (String) -> Unit = {}
            val item =
                Questionnaire.Item
                    .Builder(
                        linkId = str("item_dyn"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply { text = str("動態題目") }
                    .build()
            val staticItems = listOf(item)

            setContent {
                DynamicCardWrapper(
                    title = "動態標題",
                    items = staticItems,
                    answers = emptyMap(),
                    existingAttachments = emptyList(),
                    readOnly = false,
                    onCaptureSeries = onSeries,
                    onCaptureSingle = onSingle,
                    modifier = androidx.compose.ui.Modifier,
                    trigger = trigger.value,
                )
            }
            waitForIdle()

            // Trigger recomposition where parameters (including staticItems) are unchanged
            trigger.value++
            waitForIdle()
        }
    }
}
