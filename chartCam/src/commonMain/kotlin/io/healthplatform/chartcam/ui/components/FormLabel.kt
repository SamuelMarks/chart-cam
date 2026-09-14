/**
 * @file FormLabel.kt
 * Contains declarations for FormLabel.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_required_field
import chartcam.chartcam.generated.resources.required_field_cd_format
import chartcam.chartcam.generated.resources.required_field_format
import org.jetbrains.compose.resources.stringResource

/**
 * A consistent label component for form fields, automatically appending an asterisk if required.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param text The base text to display for the label.
 * @param isRequired Indicates if the field is mandatory; if true, appends " *" to text and adds required semantics.
 * @param modifier The modifier to be applied to the text layout.
 */
@Composable
fun FormLabel(
    text: String,
    isRequired: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (isRequired) {
        val requiredWord = stringResource(Res.string.cd_required_field)
        val fullCd = stringResource(Res.string.required_field_cd_format, text, requiredWord)
        val formattedText = stringResource(Res.string.required_field_format, text)
        val starIndex = formattedText.lastIndexOf('*')
        val errorColor = MaterialTheme.colorScheme.error
        val annotatedDisplay =
            if (starIndex >= 0) {
                buildAnnotatedString {
                    append(formattedText.substring(0, starIndex))
                    withStyle(SpanStyle(color = errorColor)) {
                        append('*')
                    }
                    if (starIndex + 1 < formattedText.length) {
                        append(formattedText.substring(starIndex + 1))
                    }
                }
            } else {
                buildAnnotatedString { append(formattedText) }
            }

        Text(
            text = annotatedDisplay,
            modifier =
                modifier.semantics {
                    contentDescription = fullCd
                },
        )
    } else {
        Text(
            text = text,
            modifier =
                modifier.semantics {
                    contentDescription = text
                },
        )
    }
}
