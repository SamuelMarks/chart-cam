package io.healthplatform.chartcam

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.runDesktopComposeUiTest
import org.junit.Test

class QuickAppTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testApp() {
        runDesktopComposeUiTest(width = 1284, height = 2778) {
            setContent { App() }
            waitForIdle()
            try {
                onAllNodes(hasContentDescription("DoesNotExist"))[0]
            } catch (e: Throwable) {
                println("Exception type: ${e.javaClass.name}")
            }
        }
    }
}
