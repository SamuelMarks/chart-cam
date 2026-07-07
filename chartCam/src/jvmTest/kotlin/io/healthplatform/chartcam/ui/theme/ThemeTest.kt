package io.healthplatform.chartcam.ui.theme

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ThemeTest {
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
}
