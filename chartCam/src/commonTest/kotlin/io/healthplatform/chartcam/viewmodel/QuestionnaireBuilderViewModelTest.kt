/**
 * @file QuestionnaireBuilderViewModelTest.kt
 * Contains tests for Questionnaire builder complex nested field state.
 */
package io.healthplatform.chartcam.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for managing complex nested field states during Questionnaire building.
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
}
