package io.healthplatform.chartcam

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

class TestTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testCompose() =
        runComposeUiTest {
            setContent {
                Text("Hello")
            }
            onNodeWithText("Hello").assertExists()
        }
}
