/**
 * @file LoginScreenDemoJvmTest.kt
 * Contains declarations for LoginScreenDemoJvmTest.kt.
 *
 * JVM Compose UI tests validating the Demo mode and Workflow Tour actions on [LoginScreen].
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.storage.SecureStorage
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Validates the demo authentication trigger, tour launcher, and language reactivity on [LoginScreen].
 */
class LoginScreenDemoJvmTest {
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
     * Verifies that the "Explore Demo" button is displayed and clicking it initiates demo login.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDemoButtonTriggersLogin() {
        setAppLanguage("en")
        runComposeUiTest {
            val storage = MockStorage()
            val authRepository = AuthRepository(storage)
            val viewModel = LoginViewModel(authRepository)
            var loginSuccessInvoked = false

            setContent {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { loginSuccessInvoked = true },
                )
            }
            waitForIdle()

            val demoButton = onNodeWithTag(TAG_DEMO_BUTTON)
            demoButton.assertIsDisplayed().assertHasClickAction()
            onNodeWithText("Explore Demo").assertIsDisplayed()

            demoButton.performClick()
            waitForIdle()

            assertTrue(authRepository.isDemoSession.value)
            assertTrue(viewModel.uiState.value.isLoggedIn)
            assertTrue(loginSuccessInvoked)
        }
    }

    /**
     * Verifies that clicking "Take App Tour" opens the tutorial and "Skip" returns to login.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testAppTourNavigationAndDismiss() {
        setAppLanguage("en")
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

            val tourButton = onNodeWithTag(TAG_TOUR_BUTTON)
            tourButton.assertIsDisplayed().assertHasClickAction()
            onNodeWithText("Take App Tour").assertIsDisplayed()

            // Open Tour
            tourButton.performClick()
            waitForIdle()

            // Verify tutorial is now shown
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_PAGER).assertIsDisplayed()
            onNodeWithText("Offline-First & Zero-Cloud Privacy").assertIsDisplayed()

            // Click Skip to return to login
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_SKIP).performClick()
            waitForIdle()

            // Verify back on login
            onNodeWithTag(TAG_DEMO_BUTTON).assertIsDisplayed()
        }
    }

    /**
     * Verifies that switching the app language immediately updates demo button copy.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLanguageSwitchingUpdatesDemoText() {
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

            setAppLanguage("es")
            waitForIdle()

            onNodeWithText("Explorar Demo").assertIsDisplayed()

            // Reset back to English
            setAppLanguage("en")
            waitForIdle()

            onNodeWithText("Explore Demo").assertIsDisplayed()
        }
    }
}
