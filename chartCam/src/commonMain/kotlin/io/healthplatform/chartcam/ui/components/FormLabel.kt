/**
 * @file FormLabel.kt
 * Contains declarations for FormLabel.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A consistent label component for form fields, automatically appending an asterisk if required.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param text The base text to display for the label.
 * @param isRequired Indicates if the field is mandatory; if true, appends " *" to the text.
 * @param modifier The modifier to be applied to the text layout.
 */
@Composable
fun FormLabel(
    text: String,
    isRequired: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val display = if (isRequired) "$text *" else text
    Text(text = display, modifier = modifier)
}
