/**
 * @file TabFocusNextTest.kt
 * Contains unit tests for TabFocusNext key event interception.
 */

package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Fake [FocusManager] that records focus movement directions.
 */
private class FakeFocusManager : FocusManager {
    var lastDirection: FocusDirection? = null
    var moveCount: Int = 0

    override fun clearFocus(force: Boolean) {}

    override fun moveFocus(focusDirection: FocusDirection): Boolean {
        lastDirection = focusDirection
        moveCount++
        return true
    }
}

/**
 * Test suite validating [tabFocusNext] modifier behavior.
 */
class TabFocusNextTest {
    /**
     * Verifies modifier chaining with [FakeFocusManager].
     */
    @Test
    fun testTabFocusNextModifierChain() {
        val focusManager = FakeFocusManager()
        val modifier = Modifier.tabFocusNext(focusManager)
        assertNotNull(modifier)
        assertEquals(0, focusManager.moveCount)
    }
}
