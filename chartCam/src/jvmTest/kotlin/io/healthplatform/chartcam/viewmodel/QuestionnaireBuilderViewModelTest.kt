package io.healthplatform.chartcam.viewmodel

import io.healthplatform.chartcam.repository.QuestionnaireRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestionnaireBuilderViewModelTest {
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
}
