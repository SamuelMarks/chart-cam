/**
 * @file QuestionnaireBuilderViewModelJvmTest.kt
 * Contains declarations for QuestionnaireBuilderViewModelJvmTest.kt.
 */
package io.healthplatform.chartcam.viewmodel

import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class QuestionnaireBuilderViewModelJvmTest.
 */
class QuestionnaireBuilderViewModelJvmTest {
    /**
     * Test testInitialState.
     */
    @Test
    fun testInitialState() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        val state = viewModel.state.value
        assertEquals("", state.title)
        assertTrue(state.items.isEmpty())
        assertFalse(state.isPreviewMode)
    }

    /**
     * Test testUpdateTitle.
     */
    @Test
    fun testUpdateTitle() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("New Form")
        assertEquals("New Form", viewModel.state.value.title)
    }

    /**
     * Test testAddAndRemoveItem.
     */
    @Test
    fun testAddAndRemoveItem() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        assertEquals(1, viewModel.state.value.items.size)

        val item =
            viewModel.state.value.items
                .first()
        assertEquals("New SINGLE_LINE_TEXT Item", item.label)
        assertEquals(WidgetType.SINGLE_LINE_TEXT, item.widgetType)

        viewModel.removeItem(item.linkId)
        assertTrue(
            viewModel.state.value.items
                .isEmpty(),
        )
    }

    /**
     * Test testUpdateItem.
     */
    @Test
    fun testUpdateItem() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.addItem(WidgetType.MULTI_SELECT)
        val linkId =
            viewModel.state.value.items
                .first()
                .linkId

        viewModel.updateItem(linkId, "Updated Label", listOf("OptA", "OptB"))

        val updatedItem =
            viewModel.state.value.items
                .first()
        assertEquals("Updated Label", updatedItem.label)
        assertEquals(listOf("OptA", "OptB"), updatedItem.options)
    }

    /**
     * Test testMoveItemUpAndDown.
     */
    @Test
    fun testMoveItemUpAndDown() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        viewModel.addItem(WidgetType.MULTI_LINE_TEXT)
        viewModel.addItem(WidgetType.DATE)

        val itemsBefore = viewModel.state.value.items
        assertEquals(3, itemsBefore.size)
        val id1 = itemsBefore[0].linkId
        val id2 = itemsBefore[1].linkId
        val id3 = itemsBefore[2].linkId

        // Move id2 up
        viewModel.moveItemUp(id2)
        var itemsAfter = viewModel.state.value.items
        assertEquals(id2, itemsAfter[0].linkId)
        assertEquals(id1, itemsAfter[1].linkId)
        assertEquals(id3, itemsAfter[2].linkId)

        // Move id2 up again (should be no-op as it's already at top)
        viewModel.moveItemUp(id2)
        itemsAfter = viewModel.state.value.items
        assertEquals(id2, itemsAfter[0].linkId)
        assertEquals(id1, itemsAfter[1].linkId)
        assertEquals(id3, itemsAfter[2].linkId)

        // Move id1 down
        viewModel.moveItemDown(id1)
        itemsAfter = viewModel.state.value.items
        assertEquals(id2, itemsAfter[0].linkId)
        assertEquals(id3, itemsAfter[1].linkId)
        assertEquals(id1, itemsAfter[2].linkId)

        // Move id1 down again (should be no-op as it's already at bottom)
        viewModel.moveItemDown(id1)
        itemsAfter = viewModel.state.value.items
        assertEquals(id2, itemsAfter[0].linkId)
        assertEquals(id3, itemsAfter[1].linkId)
        assertEquals(id1, itemsAfter[2].linkId)
    }

    /**
     * Test testTogglePreviewMode.
     */
    @Test
    fun testTogglePreviewMode() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        assertFalse(viewModel.state.value.isPreviewMode)
        viewModel.togglePreviewMode()
        assertTrue(viewModel.state.value.isPreviewMode)
    }

    /**
     * Test testSaveQuestionnaire.
     */
    @Test
    fun testSaveQuestionnaire() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("Test Builder Form")
        viewModel.addItem(WidgetType.PHOTO_CAMERA)
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)

        viewModel.saveQuestionnaire()

        val savedForms = repo.getAvailableQuestionnaires()
        // ID should be "custom-test-builder-form"
        val form = savedForms.find { it.id == "custom-test-builder-form" }
        assertTrue(form != null)
        assertEquals("Test Builder Form", form?.title?.value)
        assertEquals(2, form?.item?.size)
    }

    /**
     * Test testSingleSelectExtensionMapping.
     */
    @Test
    fun testSingleSelectExtensionMapping() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("Single Select Test")
        viewModel.addItem(WidgetType.SINGLE_SELECT)

        val linkId =
            viewModel.state.value.items
                .first()
                .linkId
        viewModel.updateItem(linkId, "Select One", listOf("Option 1", "Option 2"))

        viewModel.saveQuestionnaire()

        val form = repo.getAvailableQuestionnaires().find { it.id == "custom-single-select-test" }
        assertTrue(form != null)

        val item = form?.item?.firstOrNull()
        assertTrue(item != null)

        val ext = item?.extension?.find { it.url == "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl" }
        assertTrue(ext != null)

        val code =
            ext
                ?.value
                ?.asCodeableConcept()
                ?.value
                ?.coding
                ?.firstOrNull()
                ?.code
                ?.value
        assertEquals("check-box", code)
    }

    /**
     * Test testDuplicateLinkIdValidation.
     */
    @Test
    fun testDuplicateLinkIdValidation() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("Duplicate LinkId Test")

        // Add two items
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)

        // At first they are different, should be valid
        assertTrue(viewModel.validate())

        val questionnaire = viewModel.buildQuestionnaire()
        // Duplicate the linkId in the FHIR object manually for validation check
        val duplicateQuestionnaire =
            Questionnaire
                .Builder(status = questionnaire.status)
                .apply {
                    title = questionnaire.title?.toBuilder()
                    val dupItemBuilder =
                        Questionnaire.Item.Builder(
                            linkId =
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "dup_id" },
                            type = questionnaire.item[0].type,
                        )
                    item.add(dupItemBuilder)
                    item.add(dupItemBuilder)
                }.build()
        assertFalse(
            io.healthplatform.chartcam.validation.FhirValidator
                .validate(duplicateQuestionnaire),
        )
    }

    /**
     * Test testChoiceOptionsValidation.
     */
    @Test
    fun testChoiceOptionsValidation() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("Validation Test")
        viewModel.addItem(WidgetType.SINGLE_SELECT)

        val linkId =
            viewModel.state.value.items
                .first()
                .linkId

        // Initial state, no options, should be error
        viewModel.updateItem(linkId, "Select One", emptyList())
        assertTrue(
            viewModel.state.value.items
                .first()
                .isError,
        )
        assertFalse(viewModel.validate())

        // Add options, error should clear
        viewModel.updateItem(linkId, "Select One", listOf("Opt1"))
        assertFalse(
            viewModel.state.value.items
                .first()
                .isError,
        )
        assertTrue(viewModel.validate())

        // Remove options again
        viewModel.updateItem(linkId, "Select One", emptyList())
        assertTrue(
            viewModel.state.value.items
                .first()
                .isError,
        )
        assertFalse(viewModel.validate())
    }

    /**
     * Test testDuplicateFromId.
     */
    @Test
    fun testDuplicateFromId() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }

        // 1. Create a source questionnaire
        val sourceViewModel = QuestionnaireBuilderViewModel(repo)
        sourceViewModel.updateTitle("Source Form")
        sourceViewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        sourceViewModel.addItem(WidgetType.MULTI_SELECT)

        // Fix the multi select item so it's valid
        val items = sourceViewModel.state.value.items
        val multiSelectId = items.first { it.widgetType == WidgetType.MULTI_SELECT }.linkId
        sourceViewModel.updateItem(multiSelectId, "Label", listOf("Opt1", "Opt2"))

        val sourceId = sourceViewModel.saveQuestionnaire()
        assertTrue(sourceId != null)

        // 2. Duplicate it
        val dupViewModel = QuestionnaireBuilderViewModel(repo, duplicateFromId = sourceId)
        val state = dupViewModel.state.value

        assertEquals("Source Form (Copy)", state.title)
        assertEquals(2, state.items.size)

        val item1 = state.items[0]
        assertEquals(WidgetType.SINGLE_LINE_TEXT, item1.widgetType)

        val item2 = state.items[1]
        assertEquals(WidgetType.MULTI_SELECT, item2.widgetType)
        assertEquals(listOf("Opt1", "Opt2"), item2.options)

        // 3. Ensure nextItemId is correctly set
        dupViewModel.addItem(WidgetType.PHOTO_CAMERA)
        val newItem =
            dupViewModel.state.value.items
                .last()
        assertTrue(newItem.linkId.startsWith("item_"))
        val idNum = newItem.linkId.removePrefix("item_").toInt()
        assertTrue(idNum > 2)
    }

    /**
     * Test testAllWidgetTypesMapping.
     */
    @Test
    fun testAllWidgetTypesMapping() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("All Widgets Test")
        WidgetType.values().forEach { widgetType ->
            viewModel.addItem(widgetType)
            val addedItem =
                viewModel.state.value.items
                    .last()

            // Fix validation for choice items
            if (widgetType == WidgetType.SINGLE_SELECT || widgetType == WidgetType.MULTI_SELECT) {
                viewModel.updateItem(addedItem.linkId, addedItem.label, listOf("OptA", "OptB"))
            }
        }

        val savedId = viewModel.saveQuestionnaire()
        assertTrue(savedId != null, "Should be valid and saved")

        // Duplicate to test the reverse mapping
        val dupViewModel = QuestionnaireBuilderViewModel(repo, duplicateFromId = savedId)
        val state = dupViewModel.state.value

        assertEquals(WidgetType.values().size, state.items.size)
        WidgetType.values().forEachIndexed { index, expectedType ->
            val actualType = state.items[index].widgetType
            // Note: SWITCH and CHECKBOX both map to QuestionnaireItemType.Boolean.
            // On duplicate, they map back to WidgetType.SWITCH since there's no itemControl code saved for CHECKBOX.
            if (expectedType == WidgetType.CHECKBOX) {
                assertEquals(WidgetType.SWITCH, actualType)
            } else {
                assertEquals(expectedType, actualType)
            }
        }
    }

    /**
     * Test testSaveQuestionnaireDuplicateNameError.
     */
    @Test
    fun testSaveQuestionnaireDuplicateNameError() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }

        val viewModel1 = QuestionnaireBuilderViewModel(repo)
        viewModel1.updateTitle("Unique Name")
        viewModel1.addItem(WidgetType.SINGLE_LINE_TEXT)
        val savedId = viewModel1.saveQuestionnaire()
        assertTrue(savedId != null)

        val viewModel2 = QuestionnaireBuilderViewModel(repo)
        viewModel2.updateTitle("Unique Name")
        viewModel2.addItem(WidgetType.SINGLE_LINE_TEXT)

        val failedId = viewModel2.saveQuestionnaire()
        assertEquals(null, failedId)
        assertTrue(viewModel2.state.value.isDuplicateNameError)

        // Update title should clear error
        viewModel2.updateTitle("Another Name")
        assertFalse(viewModel2.state.value.isDuplicateNameError)

        val successfulId = viewModel2.saveQuestionnaire()
        assertTrue(successfulId != null)
    }

    /**
     * Test testValidateCatchesItemError.
     */
    @Test
    fun testValidateCatchesItemError() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("Valid Title")

        // SINGLE_SELECT without options is an error state
        viewModel.addItem(WidgetType.SINGLE_SELECT)
        assertTrue(
            viewModel.state.value.items
                .last()
                .isError,
        )

        assertFalse(viewModel.validate())
    }
}
