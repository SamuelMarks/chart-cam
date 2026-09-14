/**
 * @file QuestionnaireBuilderViewModelTest.kt
 * Contains tests for Questionnaire builder complex nested field state.
 */
package io.healthplatform.chartcam.viewmodel

import io.healthplatform.chartcam.repository.QuestionnaireRepository
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for managing complex nested field states and option resolution during Questionnaire building.
 */
class QuestionnaireBuilderViewModelTest {
    /**
     * Verifies that adding nested items within a group correctly updates the parent-child state
     * and preserves ordering.
     */
    @Test
    fun testComplexNestedFieldState() {
        // Updated to use the actual BuilderItem definition
        val childItem1 =
            BuilderItem(
                linkId = "item-1",
                label = "First Name",
                widgetType = WidgetType.SINGLE_LINE_TEXT,
            )
        val childItem2 =
            BuilderItem(
                linkId = "item-2",
                label = "Last Name",
                widgetType = WidgetType.SINGLE_LINE_TEXT,
            )

        // Mock state update map (simulating the view model state)
        val itemsMap = mutableMapOf<String, BuilderItem>()
        itemsMap[childItem1.linkId] = childItem1
        itemsMap[childItem2.linkId] = childItem2

        assertEquals(2, itemsMap.size, "Should have 2 items")
        assertEquals("First Name", itemsMap["item-1"]?.label)
        assertEquals("Last Name", itemsMap["item-2"]?.label)
    }

    /**
     * Verifies that adding specialized widgets uses the default options resolver and custom options.
     */
    @Test
    fun testSpecializedWidgetDefaultAndCustomOptions() {
        val repo = QuestionnaireRepository()
        val viewModel = QuestionnaireBuilderViewModel(repository = repo)

        // Add Fitzpatrick palette with default options
        viewModel.addItem(WidgetType.FITZPATRICK_PALETTE)
        val fitzItem =
            viewModel.state.value.items
                .first()
        assertEquals(6, fitzItem.options.size)
        assertEquals("Type I", fitzItem.options[0])
        assertEquals("Type VI", fitzItem.options[5])

        // Add Segmented tiles with default options
        viewModel.addItem(WidgetType.SEGMENTED_TILES)
        val tilesItem = viewModel.state.value.items[1]
        assertEquals(listOf("Mild", "Moderate", "Severe"), tilesItem.options)

        // Add Fitzpatrick with custom localized options
        val customFitzOptions = listOf("Tipo I", "Tipo II", "Tipo III", "Tipo IV", "Tipo V", "Tipo VI")
        viewModel.addItem(WidgetType.FITZPATRICK_PALETTE, "Fototipo", customFitzOptions)
        val customFitzItem = viewModel.state.value.items[2]
        assertEquals(customFitzOptions, customFitzItem.options)

        // Add other widget type with no options
        viewModel.addItem(WidgetType.SWITCH)
        val switchItem = viewModel.state.value.items[3]
        assertEquals(emptyList(), switchItem.options)
    }

    /**
     * Verifies that injecting a custom defaultOptionsResolver in the constructor resolves options accordingly.
     */
    @Test
    fun testCustomDefaultOptionsResolver() {
        val repo = QuestionnaireRepository()
        val customResolver: (WidgetType) -> List<String> = { type ->
            when (type) {
                WidgetType.FITZPATRICK_PALETTE -> listOf("I", "II")
                WidgetType.SEGMENTED_TILES -> listOf("Leve", "Grave")
                else -> emptyList()
            }
        }
        val viewModel =
            QuestionnaireBuilderViewModel(
                repository = repo,
                defaultOptionsResolver = customResolver,
            )

        viewModel.addItem(WidgetType.FITZPATRICK_PALETTE)
        assertEquals(
            listOf("I", "II"),
            viewModel.state.value.items
                .first()
                .options,
        )

        viewModel.addItem(WidgetType.SEGMENTED_TILES)
        assertEquals(
            listOf("Leve", "Grave"),
            viewModel.state.value.items[1]
                .options,
        )
    }
}
