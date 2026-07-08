package io.healthplatform.chartcam

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppTest {
    @Test
    fun testAppRenders() =
        runComposeUiTest {
            setContent {
                App()
            }

            onRoot().assertExists()
        }
}
