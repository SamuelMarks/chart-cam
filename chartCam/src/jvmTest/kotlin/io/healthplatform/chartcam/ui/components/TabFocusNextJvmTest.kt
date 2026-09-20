/**
 * @file TabFocusNextJvmTest.kt
 * Contains JVM unit tests for TabFocusNext key event interception.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Fake [FocusManager] for testing focus moves.
 */
private class TestFocusManager : FocusManager {
    /** The last requested focus direction. */
    var lastDirection: FocusDirection? = null

    /** Total count of moveFocus invocations. */
    var moveCount: Int = 0

    /**
     * Clears focus.
     *
     * @param force Whether to force clear.
     */
    override fun clearFocus(force: Boolean) {}

    /**
     * Moves focus in the given direction.
     *
     * @param focusDirection The direction to move.
     * @return Always true for recording.
     */
    override fun moveFocus(focusDirection: FocusDirection): Boolean {
        lastDirection = focusDirection
        moveCount++
        return true
    }
}

/**
 * JVM unit tests verifying all branches and key events in [tabFocusNext].
 */
@OptIn(ExperimentalTestApi::class)
class TabFocusNextJvmTest {
    /**
     * Verifies that Tab key events move focus forward, shift-Tab moves focus backward, and Enter is ignored.
     */
    @Test
    fun testTabFocusNextComposeKeyInput() {
        val focusManager = TestFocusManager()
        runComposeUiTest {
            setContent {
                Box(
                    modifier =
                        Modifier
                            .testTag("target")
                            .tabFocusNext(focusManager)
                            .focusable(),
                )
            }

            onNodeWithTag("target").requestFocus()

            // 1. Tab KeyDown and KeyUp without Shift -> move next
            onNodeWithTag("target").performKeyInput {
                keyDown(Key.Tab)
                keyUp(Key.Tab)
            }
            assertEquals(FocusDirection.Next, focusManager.lastDirection)
            assertEquals(1, focusManager.moveCount)

            // 2. Tab KeyDown with Shift -> move previous
            onNodeWithTag("target").performKeyInput {
                withKeyDown(Key.ShiftLeft) {
                    keyDown(Key.Tab)
                    keyUp(Key.Tab)
                }
            }
            assertEquals(FocusDirection.Previous, focusManager.lastDirection)
            assertEquals(2, focusManager.moveCount)

            // 3. Non-Tab key (Enter) -> ignored by tabFocusNext
            onNodeWithTag("target").performKeyInput {
                pressKey(Key.Enter)
            }
            assertEquals(2, focusManager.moveCount)
        }
    }
}
