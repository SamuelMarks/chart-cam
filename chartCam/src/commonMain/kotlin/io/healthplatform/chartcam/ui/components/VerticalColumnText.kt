/**
 * @file VerticalColumnText.kt
 * Contains declarations for VerticalColumnText.kt.
 *
 * Provides specialized composables and utilities for rendering text in traditional
 * East Asian vertical column-only layout (直書 / 豎排), where characters are stacked
 * vertically within columns and columns traditionally progress from right to left.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_toggle_vertical_text
import org.jetbrains.compose.resources.stringResource

/**
 * Splits input text into vertical column segments respecting maximum characters per column
 * and explicit line break characters.
 *
 * @param text The source text string to segment into vertical columns.
 * @param maxCharsPerColumn The maximum number of characters permitted in a single vertical column.
 * @return A list of columns, where each column is represented as a list of single-character strings.
 */
fun splitTextIntoVerticalColumns(
    text: String,
    maxCharsPerColumn: Int,
): List<List<String>> {
    if (text.isEmpty() || maxCharsPerColumn <= 0) return emptyList()

    val columns = mutableListOf<List<String>>()
    val rawLines = text.lines()

    for (rawLine in rawLines) {
        if (rawLine.isEmpty()) {
            columns.add(listOf(" "))
            continue
        }
        var startIndex = 0
        while (startIndex < rawLine.length) {
            val endIndex = (startIndex + maxCharsPerColumn).coerceAtMost(rawLine.length)
            val chunk = rawLine.substring(startIndex, endIndex)
            columns.add(chunk.map { it.toString() })
            startIndex = endIndex
        }
    }

    return columns
}

/**
 * Renders text in traditional East Asian vertical column format (豎排 / 直書).
 *
 * Characters are arranged top-to-bottom within each column. Columns progress
 * horizontally (by default from right-to-left in accordance with traditional Chinese
 * typography conventions).
 *
 * @param text The text to render in vertical columns.
 * @param modifier The modifier to apply to the layout.
 * @param maxCharsPerColumn Maximum number of glyphs stacked in each vertical column before wrapping.
 * @param textStyle The typography style applied to each character glyph.
 * @param columnsRightToLeft Whether columns progress from right to left (traditional `vertical-rl`).
 * @param spacingBetweenColumns Horizontal space between adjacent vertical columns.
 * @param spacingBetweenChars Vertical space between stacked characters within a column.
 */
@Composable
fun VerticalColumnText(
    text: String,
    modifier: Modifier = Modifier,
    maxCharsPerColumn: Int = 10,
    textStyle: TextStyle = LocalTextStyle.current,
    columnsRightToLeft: Boolean = true,
    spacingBetweenColumns: Dp = 12.dp,
    spacingBetweenChars: Dp = 4.dp,
) {
    val columns = splitTextIntoVerticalColumns(text, maxCharsPerColumn)
    val displayColumns = if (columnsRightToLeft) columns else columns.reversed()
    val scrollState = rememberScrollState()

    CompositionLocalProvider(
        LocalLayoutDirection provides if (columnsRightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        Row(
            modifier =
                modifier
                    .horizontalScroll(scrollState)
                    .semantics(mergeDescendants = true) { contentDescription = text },
            horizontalArrangement = Arrangement.spacedBy(spacingBetweenColumns),
            verticalAlignment = Alignment.Top,
        ) {
            for (col in displayColumns) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacingBetweenChars),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for (char in col) {
                        Text(
                            text = char,
                            style = textStyle,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A decorative clinical banner card displaying text in Traditional Chinese vertical column layout.
 *
 * Features a parchment-styled background, classical border accents, and vertical column typography.
 *
 * @param title The primary title text to display in vertical columns.
 * @param modifier The modifier to apply to the card.
 * @param subtitle Optional subtitle or slogan text to display in an adjacent vertical column.
 * @param onToggleMode Optional callback invoked when toggling vertical/horizontal layout modes.
 * @param isVerticalMode Whether the card is currently rendering in vertical column format.
 */
@Composable
fun TraditionalChineseVerticalBanner(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onToggleMode: (() -> Unit)? = null,
    isVerticalMode: Boolean = true,
) {
    Card(
        modifier = modifier.padding(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
        ) {
            if (isVerticalMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        VerticalColumnText(
                            text = title,
                            maxCharsPerColumn = 8,
                            textStyle =
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 2.sp,
                                ),
                            columnsRightToLeft = true,
                        )

                        if (!subtitle.isNullOrBlank()) {
                            VerticalColumnText(
                                text = subtitle,
                                maxCharsPerColumn = 10,
                                textStyle =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                columnsRightToLeft = true,
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            if (onToggleMode != null) {
                IconButton(
                    onClick = onToggleMode,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = if (isVerticalMode) Icons.Default.ViewStream else Icons.Default.ViewColumn,
                        contentDescription = stringResource(Res.string.cd_toggle_vertical_text),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
