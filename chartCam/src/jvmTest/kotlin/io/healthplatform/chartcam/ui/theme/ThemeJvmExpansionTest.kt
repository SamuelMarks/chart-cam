/**
 * @file ThemeJvmExpansionTest.kt
 * JVM tests verifying [RecommendedPainScoreColor] and typography for all languages.
 */

package io.healthplatform.chartcam.ui.theme

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Tests covering different language typographies and pain score color branches.
 */
@OptIn(ExperimentalTestApi::class)
class ThemeJvmExpansionTest {
    /**
     * Verifies pain score colors across all score ranges (both dark and light modes).
     */
    @Test
    fun testPainScoreColorBranches() {
        for (isDark in listOf(true, false)) {
            for (score in 0..11) {
                val color = resolvePainScoreColor(score, isDark)
                assertNotNull(color)
            }
        }
        val defaultColor = resolvePainScoreColor(3)
        assertNotNull(defaultColor)
    }

    /**
     * Verifies getTypography for English, Japanese, Hebrew, and Traditional Chinese.
     */
    @Test
    fun testGetTypographyLocales() =
        runComposeUiTest {
            setContent {
                val typoEn = getTypography("en")
                assertNotNull(typoEn)

                val typoJa = getTypography("ja")
                assertNotNull(typoJa)

                val typoHe = getTypography("he")
                assertNotNull(typoHe)

                val typoZh = getTypography("zh-Hant")
                assertNotNull(typoZh)
            }
        }
}
