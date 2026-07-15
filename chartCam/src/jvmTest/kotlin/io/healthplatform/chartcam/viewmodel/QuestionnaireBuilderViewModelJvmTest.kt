package io.healthplatform.chartcam.viewmodel

import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestionnaireBuilderViewModelJvmTest {
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

    @Test
    fun testUpdateTitle() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("New Form")
        assertEquals("New Form", viewModel.state.value.title)
    }

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

    @Test
    fun testTogglePreviewMode() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        assertFalse(viewModel.state.value.isPreviewMode)
        viewModel.togglePreviewMode()
        assertTrue(viewModel.state.value.isPreviewMode)
    }

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
}
