package io.healthplatform.chartcam.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [SetupBrowserHistory] on Android.
 */
@RunWith(AndroidJUnit4::class)
class BrowserHistorySetupAndroidTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test SetupBrowserHistory no-op execution.
     */
    @Test
    fun testSetupBrowserHistory() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            SetupBrowserHistory(navController)
        }
        // Just verify it doesn't crash as it is a no-op
    }
}
