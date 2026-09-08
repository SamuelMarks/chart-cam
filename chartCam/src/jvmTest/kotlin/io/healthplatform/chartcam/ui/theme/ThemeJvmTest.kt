/**
 * @file ThemeJvmTest.kt
 * Contains declarations for ThemeJvmTest.kt.
 */
package io.healthplatform.chartcam.ui.theme

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for Theme on JVM.
 */
@OptIn(ExperimentalTestApi::class)
class ThemeJvmTest {
    /**
     * Tests AppTheme on JVM.
     */
    @Test
    fun testAppTheme() =
        runComposeUiTest {
            setContent {
                AppTheme(darkTheme = false) {
                    Text("Test")
                }
            }

            onRoot().assertExists()
        }

    /**
     * Tests typography font family resolution across locales.
     */
    @Test
    fun testTypographyResolutionAcrossLocales() =
        runComposeUiTest {
            setContent {
                val enTypo = getTypography("en")
                val jaTypo = getTypography("ja")
                val zhTypo = getTypography("zh")
                val heTypo = getTypography("he")
                val esTypo = getTypography("es")

                assertNotNull(enTypo.bodyMedium)
                assertNotNull(jaTypo.bodyMedium)
                assertNotNull(zhTypo.bodyMedium)
                assertNotNull(heTypo.bodyMedium)
                assertNotNull(esTypo.bodyMedium)
            }
        }
}
