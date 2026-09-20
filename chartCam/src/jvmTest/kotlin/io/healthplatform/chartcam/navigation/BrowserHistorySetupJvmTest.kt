/**
 * @file BrowserHistorySetupJvmTest.kt
 * Contains declarations for BrowserHistorySetupJvmTest.kt.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation.compose.rememberNavController
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for BrowserHistorySetup on JVM.
 */
class BrowserHistorySetupJvmTest {
    /**
     * Test browser history setup component execution on JVM.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBrowserHistorySetupJvm() =
        runComposeUiTest {
            setContent {
                val navController = rememberNavController()
                SetupBrowserHistory(navController)
            }
            assertEquals("/auth/login", Routes.LOGIN)
        }
}
