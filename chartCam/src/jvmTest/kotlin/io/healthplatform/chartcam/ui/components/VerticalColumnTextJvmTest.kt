/**
 * @file VerticalColumnTextJvmTest.kt
 * Contains JVM Compose UI and logic tests for [VerticalColumnText] and [TraditionalChineseVerticalBanner].
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JVM unit test suite verifying vertical column text splitting and composable rendering.
 */
@OptIn(ExperimentalTestApi::class)
class VerticalColumnTextJvmTest {
    /**
     * Verifies text splitting logic covering empty text, negative chunk size, surrogate pairs, and CRLF line breaks.
     */
    @Test
    fun testSplitTextIntoVerticalColumnsLogic() {
        // 1. Empty text or non-positive maxCharsPerColumn
        assertEquals(emptyList(), splitTextIntoVerticalColumns("", maxCharsPerColumn = 10))
        assertEquals(emptyList(), splitTextIntoVerticalColumns("Sample", maxCharsPerColumn = 0))
        assertEquals(emptyList(), splitTextIntoVerticalColumns("Sample", maxCharsPerColumn = -3))

        // 2. Empty lines generate single-space columns
        val nl = 10.toChar().toString()
        val cr = 13.toChar().toString()
        val emptyLineCols = splitTextIntoVerticalColumns("Line1" + nl + nl + "Line2", maxCharsPerColumn = 10)
        assertEquals(3, emptyLineCols.size)
        assertEquals(listOf(" "), emptyLineCols[1])

        // 3. CRLF line break handling
        val crlfCols = splitTextIntoVerticalColumns("First" + cr + nl + "Second", maxCharsPerColumn = 10)
        assertEquals(2, crlfCols.size)

        // 4. Surrogate pair handling (emoji preserves 2-char code point)
        val emojiText = "A\uD83D\uDE00B"
        val emojiCols = splitTextIntoVerticalColumns(emojiText, maxCharsPerColumn = 10)
        assertEquals(1, emojiCols.size)
        assertEquals(listOf("A", "\uD83D\uDE00", "B"), emojiCols[0])

        // 5. Trailing high surrogate without low surrogate
        val orphanSurrogateText = "Test\uD83D"
        val orphanCols = splitTextIntoVerticalColumns(orphanSurrogateText, maxCharsPerColumn = 10)
        assertEquals(1, orphanCols.size)
        assertEquals(listOf("T", "e", "s", "t", "\uD83D"), orphanCols[0])

        // 6. High surrogate followed by a regular (non-low-surrogate) character
        val highFollowedByRegular = "A\uD83DBC"
        val highRegularCols = splitTextIntoVerticalColumns(highFollowedByRegular, maxCharsPerColumn = 10)
        assertEquals(1, highRegularCols.size)
        assertEquals(listOf("A", "\uD83D", "B", "C"), highRegularCols[0])

        // 7. Column chunking when glyphs exceed maxCharsPerColumn
        val chunkedCols = splitTextIntoVerticalColumns("1234567890", maxCharsPerColumn = 4)
        assertEquals(3, chunkedCols.size)
        assertEquals(listOf("1", "2", "3", "4"), chunkedCols[0])
        assertEquals(listOf("5", "6", "7", "8"), chunkedCols[1])
        assertEquals(listOf("9", "0"), chunkedCols[2])
    }

    /**
     * Verifies rendering of [VerticalColumnText] in both RTL and LTR orientations,
     * exercising all default parameter combinations, long scrollable texts, and recomposition skipping.
     */
    @Test
    fun testVerticalColumnTextRendering() {
        setAppLanguage("en")
        runComposeUiTest {
            val parentState = mutableStateOf(0)
            val textState = mutableStateOf("臨床記錄")
            val colsRtlState = mutableStateOf(true)

            setContent {
                val p = parentState.value
                Text("Parent: $p")

                // 1. All default parameters
                VerticalColumnText(text = "預設排版")

                // 2. Explicit parameters and state binding
                VerticalColumnText(
                    text = textState.value,
                    modifier = Modifier.testTag("vert_text_rtl"),
                    maxCharsPerColumn = 5,
                    textStyle = MaterialTheme.typography.titleMedium,
                    columnsRightToLeft = colsRtlState.value,
                    spacingBetweenColumns = 16.dp,
                    spacingBetweenChars = 6.dp,
                )

                // 3. LTR mode in LTR layout direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    VerticalColumnText(
                        text = "病歷摘要",
                        modifier = Modifier.testTag("vert_text_ltr"),
                        columnsRightToLeft = false,
                    )
                }

                // 4. Very long text triggering scrollState.maxValue > 0 in RTL mode
                VerticalColumnText(
                    text = "一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十",
                    maxCharsPerColumn = 3,
                    columnsRightToLeft = true,
                )
            }
            waitForIdle()

            onNodeWithTag("vert_text_rtl").assertIsDisplayed()
            onNodeWithContentDescription("臨床記錄").assertIsDisplayed()

            onNodeWithTag("vert_text_ltr").assertIsDisplayed()
            onNodeWithContentDescription("病歷摘要").assertIsDisplayed()

            // Mutate states to trigger composer.changed branches
            textState.value = "新臨床記錄"
            colsRtlState.value = false
            waitForIdle()

            // Trigger parent recomposition without modifying VerticalColumnText inputs (smart skip)
            parentState.value = 1
            waitForIdle()
        }
    }

    /**
     * Verifies rendering and toggle interaction in [TraditionalChineseVerticalBanner] across
     * all subtitle variations (null, blank, present), modes (vertical, horizontal), and default parameters.
     */
    @Test
    fun testTraditionalChineseVerticalBanner() {
        setAppLanguage("en")
        runComposeUiTest {
            var toggleClicked = false
            val parentState = mutableStateOf(0)
            val isVertState = mutableStateOf(true)

            // 1. Vertical mode with subtitle and toggle callback
            setContent {
                val p = parentState.value
                Text("Parent: $p")

                TraditionalChineseVerticalBanner(
                    title = "健康監控系統",
                    subtitle = "智慧診斷模組",
                    isVerticalMode = isVertState.value,
                    onToggleMode = { toggleClicked = true },
                    modifier = Modifier.testTag("banner_vert"),
                )
            }
            waitForIdle()

            onNodeWithTag("banner_vert").assertIsDisplayed()
            onNodeWithContentDescription("Toggle vertical column text layout").assertIsDisplayed()
            onNodeWithContentDescription("Toggle vertical column text layout").performClick()
            waitForIdle()
            assertTrue(toggleClicked)

            // Mutate isVerticalMode to trigger composer.changed
            isVertState.value = false
            waitForIdle()

            // Parent recomposition skip
            parentState.value = 1
            waitForIdle()

            // 2. All default arguments (title only)
            setContent {
                TraditionalChineseVerticalBanner(title = "純標題橫幅")
            }
            waitForIdle()

            // 3. Vertical mode with blank subtitle
            setContent {
                TraditionalChineseVerticalBanner(
                    title = "直排空白副標題",
                    subtitle = "   ",
                    isVerticalMode = true,
                )
            }
            waitForIdle()

            // 4. Horizontal mode with null subtitle
            setContent {
                TraditionalChineseVerticalBanner(
                    title = "橫排無副標題",
                    subtitle = null,
                    isVerticalMode = false,
                )
            }
            waitForIdle()

            // 5. Horizontal mode with blank subtitle
            setContent {
                TraditionalChineseVerticalBanner(
                    title = "橫排空白副標題",
                    subtitle = "   ",
                    isVerticalMode = false,
                )
            }
            waitForIdle()

            // 6. Horizontal mode with non-blank subtitle and toggle callback
            setContent {
                TraditionalChineseVerticalBanner(
                    title = "橫排有效副標題",
                    subtitle = "有效備註",
                    isVerticalMode = false,
                    onToggleMode = {},
                )
            }
            waitForIdle()
        }
    }
}
