/**
 * @file TabFocusNext.kt
 * Contains declarations for TabFocusNext.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * A modifier that intercepts Tab key events and moves focus to the next or previous item.
 * It always consumes the Tab event to prevent text fields from inserting tab characters.
 *
 * @param focusManager The focus manager used to move focus.
 * @return The modified modifier.
 */
fun Modifier.tabFocusNext(focusManager: FocusManager): Modifier =
    this.onPreviewKeyEvent {
        if (it.key == Key.Tab) {
            if (it.type == KeyEventType.KeyDown) {
                focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
            }
            true // Always consume Tab key events (KeyDown, KeyUp, Unknown) to prevent inserting a tab character
        } else {
            false
        }
    }
