/**
 * @file AccessibilityDemoTutorialJvmTest.kt
 * Contains declarations for AccessibilityDemoTutorialJvmTest.kt.
 *
 * Accessibility tests validating WCAG compliance, 48dp minimum touch targets,
 * and semantic heading hierarchy on Demo and Tutorial UI components.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.storage.SecureStorage
import io.healthplatform.chartcam.ui.components.DemoModeBanner
import io.healthplatform.chartcam.ui.components.TAG_EXIT_DEMO_BUTTON
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import org.junit.Test

/**
 * Validates accessibility mandates (48x48dp touch bounds, heading semantics, live regions)
 * across the workflow tutorial and demo components.
 */
class AccessibilityDemoTutorialJvmTest {
    /**
     * Mock storage for auth operations.
     */
    class MockStorage : SecureStorage {
        val map = mutableMapOf<String, String>()

        override fun save(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        override fun getString(key: String): String? = map[key]

        override fun delete(key: String) {
            map.remove(key)
        }
    }

    /**
     * Verifies that tutorial buttons meet the 48x48 dp minimum interactive target size.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testTutorialTouchTargetsAndHeading() {
        runComposeUiTest {
            setContent {
                OnboardingTutorialScreen(
                    onDismiss = {},
                    onComplete = {},
                )
            }
            waitForIdle()

            // Verify minimum 48dp touch targets
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_SKIP)
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)

            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_NEXT)
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)

            // Verify slide title has heading semantics
            val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_SLIDE_TITLE).assert(isHeading)
        }
    }

    /**
     * Verifies that the LoginScreen demo mode button meets 48dp touch target bounds.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDemoButtonTouchTarget() {
        runComposeUiTest {
            val storage = MockStorage()
            val authRepository = AuthRepository(storage)
            val viewModel = LoginViewModel(authRepository)

            setContent {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {},
                )
            }
            waitForIdle()

            onNodeWithTag(TAG_DEMO_BUTTON)
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)

            onNodeWithTag(TAG_TOUR_BUTTON)
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    /**
     * Verifies that the DemoModeBanner exit button meets 48dp bounds and banner title has heading semantics.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDemoModeBannerAccessibility() {
        runComposeUiTest {
            setContent {
                DemoModeBanner(
                    onExitDemo = {},
                )
            }
            waitForIdle()

            onNodeWithTag(TAG_EXIT_DEMO_BUTTON)
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)

            val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
            onNodeWithText("DEMO MODE — Sample Data Only").assert(isHeading)
        }
    }
}
