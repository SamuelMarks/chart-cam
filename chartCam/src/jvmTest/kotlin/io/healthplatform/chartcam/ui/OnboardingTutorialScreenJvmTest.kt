/**
 * @file OnboardingTutorialScreenJvmTest.kt
 * Contains declarations for OnboardingTutorialScreenJvmTest.kt.
 *
 * JVM Compose UI tests for [OnboardingTutorialScreen].
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Verifies interactive carousel navigation, action callbacks, and accessibility elements
 * of [OnboardingTutorialScreen].
 */
class OnboardingTutorialScreenJvmTest {
    /**
     * Verifies that the initial tutorial slide displays expected privacy copy and skip action.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testTutorialInitialSlide() {
        runComposeUiTest {
            setContent {
                OnboardingTutorialScreen(
                    onDismiss = {},
                    onComplete = {},
                )
            }
            waitForIdle()

            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_SKIP).assertIsDisplayed().assertHasClickAction()
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_NEXT).assertIsDisplayed().assertHasClickAction()
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_PREV).assertDoesNotExist()

            onNodeWithText("Offline-First & Zero-Cloud Privacy").assertIsDisplayed()
        }
    }

    /**
     * Verifies that clicking "Next" advances the carousel and "Previous" navigates backward.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testTutorialNavigationNextAndPrev() {
        runComposeUiTest {
            setContent {
                OnboardingTutorialScreen(
                    onDismiss = {},
                    onComplete = {},
                )
            }
            waitForIdle()

            // Advance to Slide 2
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_NEXT).performClick()
            waitForIdle()

            onNodeWithText("Fast Patient & Encounter Intake").assertIsDisplayed()
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_PREV).assertIsDisplayed().assertHasClickAction()

            // Navigate back to Slide 1
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_PREV).performClick()
            waitForIdle()

            onNodeWithText("Offline-First & Zero-Cloud Privacy").assertIsDisplayed()
        }
    }

    /**
     * Verifies that clicking the "Skip" button invokes the onDismiss callback.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testTutorialSkipTriggersOnDismiss() {
        runComposeUiTest {
            var dismissed = false
            setContent {
                OnboardingTutorialScreen(
                    onDismiss = { dismissed = true },
                    onComplete = {},
                )
            }
            waitForIdle()

            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_SKIP).performClick()
            waitForIdle()

            assertTrue(dismissed)
        }
    }

    /**
     * Verifies that advancing to the last slide replaces "Next" with "Get Started" and invokes onComplete.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testTutorialFinalSlideAndGetStarted() {
        runComposeUiTest {
            var completed = false
            setContent {
                OnboardingTutorialScreen(
                    onDismiss = {},
                    onComplete = { completed = true },
                )
            }
            waitForIdle()

            // Advance through slides: 1 -> 2 -> 3 -> 4
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_NEXT).performClick()
            waitForIdle()
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_NEXT).performClick()
            waitForIdle()
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_NEXT).performClick()
            waitForIdle()

            onNodeWithText("Instant FHIR Export & Audit").assertIsDisplayed()
            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_NEXT).assertDoesNotExist()

            val getStartedNode = onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_GET_STARTED)
            getStartedNode.assertIsDisplayed().assertHasClickAction()
            getStartedNode.performClick()
            waitForIdle()

            assertTrue(completed)
        }
    }

    /**
     * Verifies that page indicators exist, are displayed, and can be clicked to navigate directly.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testTutorialIndicatorSemantics() {
        runComposeUiTest {
            setContent {
                OnboardingTutorialScreen(
                    onDismiss = {},
                    onComplete = {},
                    modifier = androidx.compose.ui.Modifier,
                    slides = OnboardingTutorialDefaults.DEFAULT_SLIDES,
                )
            }
            waitForIdle()

            onNodeWithTag(OnboardingTutorialDefaults.TAG_TUTORIAL_INDICATORS).assertIsDisplayed()

            // Click the indicator for slide 3 (page 3)
            onNodeWithContentDescription("Go to page 3", useUnmergedTree = true)
                .performClick()
            waitForIdle()

            onNodeWithText("Standardized Clinical Capture").assertIsDisplayed()
        }
    }

    /**
     * Verifies recomposition and skipping in [OnboardingTutorialScreen].
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testOnboardingTutorialRecompositionAndSkipping() {
        runComposeUiTest {
            val outerTrigger = androidx.compose.runtime.mutableStateOf(0)
            val onDismissState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val onCompleteState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val modifierState = androidx.compose.runtime.mutableStateOf<androidx.compose.ui.Modifier>(androidx.compose.ui.Modifier)
            val slidesState = androidx.compose.runtime.mutableStateOf(OnboardingTutorialDefaults.DEFAULT_SLIDES)

            setContent {
                val dummy = outerTrigger.value
                OnboardingTutorialScreen(
                    onDismiss = onDismissState.value,
                    onComplete = onCompleteState.value,
                    modifier = modifierState.value,
                    slides = slidesState.value,
                )
            }
            waitForIdle()

            outerTrigger.value++
            waitForIdle()

            onDismissState.value = { println("dismissed") }
            waitForIdle()

            onCompleteState.value = { println("completed") }
            waitForIdle()

            modifierState.value =
                androidx.compose.ui.Modifier
                    .semantics { }
            waitForIdle()

            slidesState.value = OnboardingTutorialDefaults.DEFAULT_SLIDES.take(2)
            waitForIdle()
        }
    }
}
