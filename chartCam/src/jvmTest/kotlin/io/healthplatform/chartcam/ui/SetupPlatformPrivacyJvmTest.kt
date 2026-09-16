/**
 * @file SetupPlatformPrivacyJvmTest.kt
 * Contains declarations for SetupPlatformPrivacyJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertNotNull

/**
 * JVM unit tests for SetupPlatformPrivacy.
 */
class SetupPlatformPrivacyJvmTest {
    /**
     * Compose test rule.
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Tests that SetupPlatformPrivacy executes without throwing.
     */
    @Test
    fun testSetupPlatformPrivacyRuns() {
        composeTestRule.setContent {
            SetupPlatformPrivacy()
        }
        assertNotNull(currentAppPrivacyManager)
    }
}
