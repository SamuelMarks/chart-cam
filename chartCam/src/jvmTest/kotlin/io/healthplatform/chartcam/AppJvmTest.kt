/**
 * @file AppJvmTest.kt
 * Contains declarations for AppJvmTest.kt.
 */
package io.healthplatform.chartcam

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Test class for App on JVM.
 */
@OptIn(ExperimentalTestApi::class)
class AppJvmTest {
    /**
     * Test app renders on JVM.
     */
    @Test
    fun testAppRenders() =
        runComposeUiTest {
            setContent {
                App()
            }

            onRoot().assertExists()
        }
}
